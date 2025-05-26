package ba.sum.fpmoz.example.studenteventsapp

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*

class InterestedUsersActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private lateinit var database: DatabaseReference
    private val interestedUserNames = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_interested_users)

        listView = findViewById(R.id.listViewInterestedUsers)
        database = FirebaseDatabase.getInstance().reference

        val eventId = intent.getStringExtra("eventId")
        if (eventId != null) {
            fetchInterestedUsers(eventId)
        } else {
            Toast.makeText(this, "Nema ID događaja", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchInterestedUsers(eventId: String) {
        val votesRef = database.child("Events").child(eventId).child("votes")

        votesRef.orderByValue().equalTo("interested")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(voteSnapshot: DataSnapshot) {
                    if (!voteSnapshot.exists()) {
                        Toast.makeText(this@InterestedUsersActivity, "Nema zainteresiranih korisnika.", Toast.LENGTH_SHORT).show()
                        return
                    }

                    interestedUserNames.clear()
                    val total = voteSnapshot.children.count()
                    var fetched = 0

                    for (vote in voteSnapshot.children) {
                        val userId = vote.key ?: continue

                        database.child("Users").child(userId)
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(userSnapshot: DataSnapshot) {
                                    val firstName = userSnapshot.child("firstName").getValue(String::class.java)
                                    val lastName = userSnapshot.child("lastName").getValue(String::class.java)
                                    if (!firstName.isNullOrEmpty() && !lastName.isNullOrEmpty()) {
                                        interestedUserNames.add("$firstName $lastName")
                                    }

                                    fetched++
                                    if (fetched == total) {
                                        val adapter = ArrayAdapter(
                                            this@InterestedUsersActivity,
                                            android.R.layout.simple_list_item_1,
                                            interestedUserNames
                                        )
                                        listView.adapter = adapter
                                    }
                                }

                                override fun onCancelled(error: DatabaseError) {}
                            })
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }
}
