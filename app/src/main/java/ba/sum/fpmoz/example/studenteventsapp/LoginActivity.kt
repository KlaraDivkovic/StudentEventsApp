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
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private val RC_SIGN_IN = 9001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        // Google konfiguracija
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // UI elementi
        val emailEditText: EditText = findViewById(R.id.emailEditText)
        val passwordEditText: EditText = findViewById(R.id.passwordEditText)
        val loginButton: Button = findViewById(R.id.loginButton)
        val googleLoginButton: Button = findViewById(R.id.googleLoginButton)
        val registerRedirect: TextView = findViewById(R.id.registerRedirect)
        val forgotPasswordText: TextView = findViewById(R.id.forgotPasswordText)

        loginButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "Uspješna prijava!", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
                        } else {
                            Toast.makeText(this, "Greška: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
            } else {
                Toast.makeText(this, "Popuni sva polja", Toast.LENGTH_SHORT).show()
            }
        }

        googleLoginButton.setOnClickListener {
            googleSignInClient.signOut().addOnCompleteListener {
                val signInIntent = googleSignInClient.signInIntent
                startActivityForResult(signInIntent, RC_SIGN_IN)
            }
        }

        forgotPasswordText.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            if (email.isNotEmpty()) {
                auth.sendPasswordResetEmail(email)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Email za reset lozinke poslan!", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Greška: ${it.message}", Toast.LENGTH_LONG).show()
                    }
            } else {
                Toast.makeText(this, "Unesi email za reset lozinke", Toast.LENGTH_SHORT).show()
            }
        }

        // Stiliziraj "Sign up" u tekstu
        val fullText = "Don't have an account? Sign up"
        val spannable = SpannableString(fullText)
        val greenColor = Color.parseColor("#2C5F2D")
        val start = fullText.indexOf("Sign up")
        val end = fullText.length

        spannable.setSpan(ForegroundColorSpan(greenColor), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannable.setSpan(StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannable.setSpan(object : ClickableSpan() {
            override fun onClick(widget: View) {
                startActivity(Intent(this@LoginActivity, RegisterActivity::class.java))
            }

            override fun updateDrawState(ds: android.text.TextPaint) {
                super.updateDrawState(ds)
                ds.color = greenColor
                ds.isUnderlineText = false
            }
        }, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        registerRedirect.text = spannable
        registerRedirect.movementMethod = LinkMovementMethod.getInstance()
        registerRedirect.highlightColor = Color.TRANSPARENT
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.result
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                auth.signInWithCredential(credential)
                    .addOnCompleteListener(this) { signInTask ->
                        if (signInTask.isSuccessful) {
                            Toast.makeText(this, "Google prijava uspješna!", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
                        } else {
                            Toast.makeText(this, "Google prijava neuspješna!", Toast.LENGTH_SHORT).show()
                        }
                    }
            } catch (e: Exception) {
                Toast.makeText(this, "Greška: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
