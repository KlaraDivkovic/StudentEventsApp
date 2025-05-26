package ba.sum.fpmoz.example.studenteventsapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

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
        database.child("Events").addValueEventListener(object : ValueEventListener {
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
            val commentInput: EditText = view.findViewById(R.id.editTextComment)
            val submitComment: Button = view.findViewById(R.id.buttonSubmitComment)
            val commentsLayout: LinearLayout = view.findViewById(R.id.commentsLayout)
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

            Glide.with(holder.itemView.context)
                .load(Uri.parse(event.imageUrl))
                .placeholder(R.drawable.download)
                .error(R.drawable.download)
                .into(holder.image)

            holder.textInterested.text = "Zainteresirani: ${event.interestedCount}"
            holder.textNotInterested.text = "Nisu zainteresirani: ${event.notInterestedCount}"

            val auth = FirebaseAuth.getInstance()
            val userId = auth.currentUser?.uid

            if (userId == null) {
                holder.btnEdit.visibility = View.GONE
                holder.btnDelete.visibility = View.GONE
                holder.btnInterested.visibility = View.GONE
                holder.btnNotInterested.visibility = View.GONE
                holder.commentInput.visibility = View.GONE
                holder.submitComment.visibility = View.GONE
            } else {
                holder.commentInput.visibility = View.VISIBLE
                holder.submitComment.visibility = View.VISIBLE

                holder.submitComment.setOnClickListener {
                    val commentText = holder.commentInput.text.toString().trim()
                    if (commentText.isNotEmpty()) {
                        val commentRef = FirebaseDatabase.getInstance().getReference("Events")
                            .child(event.id).child("comments").push()
                        val commentMap = mapOf("userId" to userId, "text" to commentText)
                        commentRef.setValue(commentMap)
                            .addOnSuccessListener {
                                Toast.makeText(holder.itemView.context, "Komentar poslan", Toast.LENGTH_SHORT).show()
                                holder.commentInput.text.clear()
                            }
                            .addOnFailureListener {
                                Toast.makeText(holder.itemView.context, "Greška pri slanju komentara", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        Toast.makeText(holder.itemView.context, "Komentar ne smije biti prazan", Toast.LENGTH_SHORT).show()
                    }
                }

                val votesRef = FirebaseDatabase.getInstance().getReference("Events").child(event.id).child("votes")

                votesRef.child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val vote = snapshot.getValue(String::class.java)

                        if (vote == "interested" || vote == "not_interested") {
                            holder.btnInterested.isEnabled = false
                            holder.btnNotInterested.isEnabled = false
                        } else {
                            holder.btnInterested.setOnClickListener {
                                val eventRef = FirebaseDatabase.getInstance().getReference("Events").child(event.id)
                                eventRef.child("interestedCount").setValue(event.interestedCount + 1)
                                votesRef.child(userId).setValue("interested")
                                Toast.makeText(holder.itemView.context, "Glasano: zainteresiran", Toast.LENGTH_SHORT).show()
                                holder.btnInterested.isEnabled = false
                                holder.btnNotInterested.isEnabled = false
                            }

                            holder.btnNotInterested.setOnClickListener {
                                val eventRef = FirebaseDatabase.getInstance().getReference("Events").child(event.id)
                                eventRef.child("notInterestedCount").setValue(event.notInterestedCount + 1)
                                votesRef.child(userId).setValue("not_interested")
                                Toast.makeText(holder.itemView.context, "Glasano: nije zainteresiran", Toast.LENGTH_SHORT).show()
                                holder.btnInterested.isEnabled = false
                                holder.btnNotInterested.isEnabled = false
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })

                // Prikaz komentara u realnom vremenu
                val commentsRef = FirebaseDatabase.getInstance().getReference("Events").child(event.id).child("comments")
                holder.commentsLayout.removeAllViews()
                commentsRef.addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        holder.commentsLayout.removeAllViews()
                        for (commentSnap in snapshot.children) {
                            val commentText = commentSnap.child("text").getValue(String::class.java) ?: continue
                            val commentView = TextView(holder.itemView.context).apply {
                                text = "- $commentText"
                                textSize = 14f
                                setPadding(8, 4, 8, 4)
                            }
                            holder.commentsLayout.addView(commentView)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })
            }

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
        }

        override fun getItemCount(): Int = items.size
    }
}
