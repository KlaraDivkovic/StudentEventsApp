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
    val imageUrl: String = ""
)

class EventEventActivity : AppCompatActivity() {

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
                        Toast.makeText(this@EventEventActivity, "Prijavljen kao: ${if (isAdmin) "Admin" else "Korisnik"}", Toast.LENGTH_SHORT).show()
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
                Toast.makeText(this@EventEventActivity, "Greška u dohvaćanju podataka", Toast.LENGTH_SHORT).show()
            }
        })
    }

    class EventAdapter(
        private val items: List<Event>,
        private val isAdmin: Boolean
    ) : RecyclerView.Adapter<EventAdapter.EventViewHolder>() {

        inner class EventViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val title: TextView = view.findViewById(R.id.textTitle)
            val description: TextView = view.findViewById(R.id.textDescription)
            val btnEdit: Button = view.findViewById(R.id.buttonEdit)
            val btnDelete: Button = view.findViewById(R.id.buttonDelete)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_event, parent, false)
            return EventViewHolder(view)
        }

        override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
            val event = items[position]
            holder.title.text = event.title
            holder.description.text = event.description

            if (!isAdmin) {
                holder.btnEdit.visibility = View.GONE
                holder.btnDelete.visibility = View.GONE
            } else {
                holder.btnEdit.visibility = View.VISIBLE
                holder.btnDelete.visibility = View.VISIBLE

                holder.btnEdit.setOnClickListener {
                    Toast.makeText(holder.itemView.context, "Uredi kliknut (nije implementirano)", Toast.LENGTH_SHORT).show()
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
