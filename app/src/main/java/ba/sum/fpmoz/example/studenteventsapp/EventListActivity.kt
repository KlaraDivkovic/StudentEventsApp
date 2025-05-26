package ba.sum.fpmoz.example.studenteventsapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class EventListActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private val events = mutableListOf<Event>()
    private var isGuest = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_StudentEventsApp)
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
                        Toast.makeText(
                            this@EventListActivity,
                            "Prijavljen kao: ${if (isAdmin) "Admin" else "Korisnik"}",
                            Toast.LENGTH_SHORT
                        ).show()
                        loadEvents(isAdmin)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        loadEvents(false)
                    }
                })
        } else {
            isGuest = true
            Toast.makeText(this, "Pregledavate događaje kao gost.", Toast.LENGTH_SHORT).show()
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
                recyclerView.adapter = EventAdapter(events, isAdmin, isGuest)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    this@EventListActivity,
                    "Greška u dohvaćanju podataka",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    class EventAdapter(
        private val items: List<Event>,
        private val isAdmin: Boolean,
        private val isGuest: Boolean
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
            val editTextComment: EditText = view.findViewById(R.id.editTextComment)
            val buttonSubmitComment: Button = view.findViewById(R.id.buttonSubmitComment)
            val commentsContainer: LinearLayout = view.findViewById(R.id.commentsLayout)

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

            holder.btnEdit.visibility = if (isAdmin) View.VISIBLE else View.GONE
            holder.btnDelete.visibility = if (isAdmin) View.VISIBLE else View.GONE
            holder.btnInterested.visibility = if (!isGuest) View.VISIBLE else View.GONE
            holder.btnNotInterested.visibility = if (!isGuest) View.VISIBLE else View.GONE
            holder.textInterested.visibility = if (!isGuest) View.VISIBLE else View.GONE
            holder.textNotInterested.visibility = if (!isGuest) View.VISIBLE else View.GONE

            // Komentari (prikaz u stvarnom vremenu)
            val commentsRef = FirebaseDatabase.getInstance().getReference("Events")
                .child(event.id).child("comments")
            holder.commentsContainer.removeAllViews()
            commentsRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    holder.commentsContainer.removeAllViews()
                    for (commentSnapshot in snapshot.children) {
                        val commentText = commentSnapshot.child("text").getValue(String::class.java)
                        val commentView = TextView(holder.itemView.context)
                        commentView.text = "\u2022 $commentText"
                        holder.commentsContainer.addView(commentView)
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })

            holder.itemView.setOnClickListener {
                val context = holder.itemView.context
                val intent = Intent(context, EventDetailActivity::class.java)
                intent.putExtra("eventId", event.id)
                intent.putExtra("title", event.title)
                intent.putExtra("year", event.year)
                intent.putExtra("date", event.date)
                intent.putExtra("imageUrl", event.imageUrl)
                intent.putExtra("interestedCount", event.interestedCount)
                intent.putExtra("notInterestedCount", event.notInterestedCount)
                context.startActivity(intent)
            }

            if (!isGuest) {
                holder.buttonSubmitComment.setOnClickListener {
                    val commentText = holder.editTextComment.text.toString().trim()
                    if (commentText.isNotEmpty()) {
                        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@setOnClickListener
                        val commentData = mapOf(
                            "text" to commentText,
                            "userId" to userId,
                            "timestamp" to ServerValue.TIMESTAMP
                        )
                        val commentRef = FirebaseDatabase.getInstance()
                            .getReference("Events")
                            .child(event.id)
                            .child("comments")
                            .push()
                        commentRef.setValue(commentData)
                        holder.editTextComment.text.clear()
                    } else {
                        Toast.makeText(holder.itemView.context, "Komentar ne može biti prazan", Toast.LENGTH_SHORT).show()
                    }
                }


            } else {
                holder.editTextComment.visibility = View.GONE
                holder.buttonSubmitComment.visibility = View.GONE
            }
        }

        override fun getItemCount(): Int = items.size
    }
}
