package ba.sum.fpmoz.example.studenteventsapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class HomeActivity : AppCompatActivity() {

    private lateinit var btnViewEvents: Button
    private lateinit var btnLogin: Button
    private lateinit var btnRegister: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        btnViewEvents = findViewById(R.id.buttonViewEvents)
        btnLogin = findViewById(R.id.buttonLogin)
        btnRegister = findViewById(R.id.buttonRegister)

        val currentUser = FirebaseAuth.getInstance().currentUser

        if (currentUser != null) {
            // Ako je korisnik već prijavljen – idi odmah na MainActivity
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            return
        }

        btnViewEvents.setOnClickListener {
            startActivity(Intent(this, EventListActivity::class.java))
        }

        btnLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        btnRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
}
