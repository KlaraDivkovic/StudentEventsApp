package ba.sum.fpmoz.example.studenteventsapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import android.net.Uri
import android.widget.ImageView
import com.bumptech.glide.Glide
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

data class Event(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val year: Int = 0,
    val date: String = "",
    val imageUrl: String = "",
    val interestedCount: Int = 0,
    val notInterestedCount: Int = 0
)

class EditEventActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private val events = mutableListOf<Event>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_event_list)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "BACK"

        recyclerView = findViewById(R.id.recyclerViewEvents)
        recyclerView.layoutManager = LinearLayoutManager(this)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        checkIfAdminAndLoadEvents()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    private fun checkIfAdminAndLoadEvents() {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            database.child("Users").child(uid).child("role")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val role = snapshot.getValue(String::class.java)
                        val isAdmin = role == "admin"
                        Log.d("ROLE_CHECK", "Prijavljen kao: ${if (isAdmin) "ADMIN" else "KORISNIK"}")
                        Toast.makeText(this@EditEventActivity, "Prijavljen kao: ${if (isAdmin) "Admin" else "Korisnik"}", Toast.LENGTH_SHORT).show()
                        loadEvents(isAdmin)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        loadEvents(false)
                    }
                })
        } else {
            Log.d("ROLE_CHECK", "Nije prijavljen – gost")
            Toast.makeText(this, "Gost ste", Toast.LENGTH_SHORT).show()
            loadEvents(false)
        }
    }

    private fun loadEvents(isAdmin: Boolean) {
        database.child("Events").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                events.clear()
                for (child in snapshot.children) {
                    val event = child.getValue(Event::class.java)
                    if (event != null) {
                        events.add(event.copy(id = child.key ?: ""))
                    }
                }
                recyclerView.adapter = EventAdapter(events, isAdmin)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@EditEventActivity, "Greška u dohvaćanju podataka", Toast.LENGTH_SHORT).show()
            }
        })
    }

    class EventAdapter(
        private val items: List<Event>,
        private val isAdmin: Boolean
    ) : RecyclerView.Adapter<EventAdapter.EventViewHolder>() {

        inner class EventViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val image: ImageView = view.findViewById(R.id.imageEvent)
            val title: TextView = view.findViewById(R.id.textTitle)
            val description: TextView = view.findViewById(R.id.textDescription)
            val btnEdit: Button = view.findViewById(R.id.buttonEdit)
            val btnDelete: Button = view.findViewById(R.id.buttonDelete)
            val btnInterested: Button = view.findViewById(R.id.buttonInterested)
            val btnNotInterested: Button = view.findViewById(R.id.buttonNotInterested)
            val textInterested: TextView = view.findViewById(R.id.textInterestedCount)
            val textNotInterested: TextView = view.findViewById(R.id.textNotInterestedCount)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_event, parent, false)
            return EventViewHolder(view)
        }

        override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
            val event = items[position]
            holder.title.text = event.title
            holder.description.text = event.description

            // Glide za sliku
            Glide.with(holder.itemView.context)
                .load(Uri.parse(event.imageUrl))
                .placeholder(R.drawable.download)
                .error(R.drawable.download)
                .into(holder.image)

            // AUTOMATSKI UPDATE BROJA GLASOVA
            val eventRef = FirebaseDatabase.getInstance().getReference("Events").child(event.id)
            eventRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val interested = snapshot.child("interestedCount").getValue(Int::class.java) ?: 0
                    val notInterested = snapshot.child("notInterestedCount").getValue(Int::class.java) ?: 0
                    holder.textInterested.text = "Zainteresirani: $interested"
                    holder.textNotInterested.text = "Nisu zainteresirani: $notInterested"
                }

                override fun onCancelled(error: DatabaseError) {}
            })

            // ADMIN DUGMAD
            if (!isAdmin) {
                holder.btnEdit.visibility = View.GONE
                holder.btnDelete.visibility = View.GONE
            } else {
                holder.btnEdit.visibility = View.VISIBLE
                holder.btnDelete.visibility = View.VISIBLE

                holder.btnEdit.setOnClickListener {
                    Toast.makeText(
                        holder.itemView.context,
                        "Uredi kliknut (nije implementirano)",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                holder.btnDelete.setOnClickListener {
                    val context = holder.itemView.context
                    val database = FirebaseDatabase.getInstance().getReference("Events")
                    database.child(event.id).removeValue().addOnSuccessListener {
                        Toast.makeText(context, "Događaj obrisan", Toast.LENGTH_SHORT).show()
                    }.addOnFailureListener {
                        Toast.makeText(context, "Greška pri brisanju", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            // GLASANJE
            val auth = FirebaseAuth.getInstance()
            val userId = auth.currentUser?.uid

            if (userId == null) {
                holder.btnInterested.visibility = View.GONE
                holder.btnNotInterested.visibility = View.GONE
            } else {
                val votesRef = eventRef.child("votes")

                votesRef.child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val vote = snapshot.getValue(String::class.java)

                        if (vote == "interested" || vote == "not_interested") {
                            holder.btnInterested.isEnabled = false
                            holder.btnNotInterested.isEnabled = false
                        } else {
                            holder.btnInterested.setOnClickListener {
                                eventRef.child("interestedCount")
                                    .runTransaction(object : Transaction.Handler {
                                        override fun doTransaction(mutableData: MutableData): Transaction.Result {
                                            val current = mutableData.getValue(Int::class.java) ?: 0
                                            mutableData.value = current + 1
                                            return Transaction.success(mutableData)
                                        }

                                        override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {}
                                    })
                                votesRef.child(userId).setValue("interested")
                                holder.btnInterested.isEnabled = false
                                holder.btnNotInterested.isEnabled = false
                            }

                            holder.btnNotInterested.setOnClickListener {
                                eventRef.child("notInterestedCount")
                                    .runTransaction(object : Transaction.Handler {
                                        override fun doTransaction(mutableData: MutableData): Transaction.Result {
                                            val current = mutableData.getValue(Int::class.java) ?: 0
                                            mutableData.value = current + 1
                                            return Transaction.success(mutableData)
                                        }

                                        override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {}
                                    })
                                votesRef.child(userId).setValue("not_interested")
                                holder.btnInterested.isEnabled = false
                                holder.btnNotInterested.isEnabled = false
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })
            }
        }

        override fun getItemCount(): Int = items.size
    }

}