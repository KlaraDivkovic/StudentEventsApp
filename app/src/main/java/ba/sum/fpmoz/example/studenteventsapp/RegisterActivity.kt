package ba.sum.fpmoz.example.studenteventsapp

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "⬅\uFE0F" //
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()

        val firstName = findViewById<EditText>(R.id.firstNameEditText)
        val lastName = findViewById<EditText>(R.id.lastNameEditText)
        val faculty = findViewById<EditText>(R.id.facultyEditText)
        val year = findViewById<EditText>(R.id.yearEditText)
        val email = findViewById<EditText>(R.id.emailEditText)
        val password = findViewById<EditText>(R.id.passwordEditText)
        val registerButton = findViewById<Button>(R.id.registerButton)
        val loginRedirect = findViewById<TextView>(R.id.loginRedirect)

        registerButton.setOnClickListener {
            val firstNameText = firstName.text.toString().trim()
            val lastNameText = lastName.text.toString().trim()
            val facultyText = faculty.text.toString().trim()
            val yearText = year.text.toString().trim()
            val emailText = email.text.toString().trim()
            val passwordText = password.text.toString().trim()

            if (emailText.isEmpty() || passwordText.isEmpty() || yearText.isEmpty() || firstNameText.isEmpty() || lastNameText.isEmpty() || facultyText.isEmpty()) {
                Toast.makeText(this, "Ispunite sva polja", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val yearInt = yearText.toIntOrNull()
            if (yearInt == null || yearInt !in 1..6) {
                Toast.makeText(this, "Godina mora biti broj od 1 do 6", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(emailText, passwordText)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val uid = auth.currentUser?.uid
                        val userMap = mapOf(
                            "firstName" to firstNameText,
                            "lastName" to lastNameText,
                            "faculty" to facultyText,
                            "year" to yearInt,
                            "email" to emailText
                        )

                        FirebaseDatabase.getInstance().getReference("Users")
                            .child(uid ?: "")
                            .setValue(userMap)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Registracija uspješna!", Toast.LENGTH_SHORT).show()
                                startActivity(Intent(this, LoginActivity::class.java))
                                finish()
                            }
                            .addOnFailureListener {
                                Toast.makeText(this, "Greška pri spremanju podataka", Toast.LENGTH_LONG).show()
                            }
                    } else {
                        Toast.makeText(this, "Greška: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }

        // Tekst: Already have an account? Log in — Log in zelen i bold, klikabilan
        val fullText = "Already have an account? Log in"
        val spannable = SpannableString(fullText)
        val start = fullText.indexOf("Log in")
        val end = fullText.length
        val greenColor = Color.parseColor("#2C5F2D")

        // Podebljano i zeleno za "Log in"
        spannable.setSpan(StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannable.setSpan(ForegroundColorSpan(greenColor), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        // Klikabilno
        val clickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                startActivity(Intent(this@RegisterActivity, LoginActivity::class.java))
                finish()
            }

            override fun updateDrawState(ds: android.text.TextPaint) {
                super.updateDrawState(ds)
                ds.color = greenColor
                ds.isUnderlineText = false
            }
        }
        spannable.setSpan(clickableSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        loginRedirect.text = spannable
        loginRedirect.movementMethod = LinkMovementMethod.getInstance()
        loginRedirect.highlightColor = Color.TRANSPARENT
    }
    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

}
