package ba.sum.fpmoz.example.studenteventsapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import ba.sum.fpmoz.example.studenteventsapp.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "⬅️"
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        val currentUser = auth.currentUser

        if (currentUser != null) {
            val uid = currentUser.uid
            database.child("Users").child(uid).child("role")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val role = snapshot.getValue(String::class.java)
                        if (role == "admin") {
                            binding.addEventButton.visibility = View.VISIBLE
                        } else {
                            binding.addEventButton.visibility = View.GONE
                        }
                        binding.logoutButton.visibility = View.VISIBLE
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(this@MainActivity, "Greška u dohvaćanju uloge.", Toast.LENGTH_SHORT).show()
                    }
                })
        } else {
            // GOST
            binding.addEventButton.visibility = View.GONE
            binding.logoutButton.visibility = View.GONE
        }

        // ✅ ODJAVA + Povratak na HomeActivity
        binding.logoutButton.setOnClickListener {
            auth.signOut()
            Toast.makeText(this, "Odjavljeni ste.", Toast.LENGTH_SHORT).show()

            // Vrati se na početni ekran (HomeActivity)
            val intent = Intent(this, HomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

        // Prikaz događaja
        binding.viewEventsButton.setOnClickListener {
            startActivity(Intent(this, EventListActivity::class.java))
        }

        // Dodaj događaj (admin)
        binding.addEventButton.setOnClickListener {
            startActivity(Intent(this, AddEventActivity::class.java))
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
