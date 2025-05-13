package ba.sum.fpmoz.example.studenteventsapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import ba.sum.fpmoz.example.studenteventsapp.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        // Provjeri je li korisnik prijavljen
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // Prijavljen – prikaži "Odjava"
            binding.logoutButton.visibility = View.VISIBLE
        } else {
            // Nije prijavljen – sakrij "Odjava"
            binding.logoutButton.visibility = View.GONE
        }

        binding.loginButton.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        binding.registerButton.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        binding.logoutButton.setOnClickListener {
            auth.signOut()
            Toast.makeText(this, "Odjavljen", Toast.LENGTH_SHORT).show()
            // Sakrij dugme nakon odjave
            binding.logoutButton.visibility = View.GONE
        }
    }
}
