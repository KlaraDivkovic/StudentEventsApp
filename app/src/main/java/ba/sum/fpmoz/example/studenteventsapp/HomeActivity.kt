package ba.sum.fpmoz.example.studenteventsapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class HomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        val btnViewEvents: Button = findViewById(R.id.buttonViewEvents)
        val btnLogin: Button = findViewById(R.id.buttonLogin)
        val btnRegister: Button = findViewById(R.id.buttonRegister)

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
