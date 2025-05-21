package ba.sum.fpmoz.example.studenteventsapp

import android.app.DatePickerDialog
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
import java.text.SimpleDateFormat
import java.util.*

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "⬅️"
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()

        val firstName = findViewById<EditText>(R.id.firstNameEditText)
        val lastName = findViewById<EditText>(R.id.lastNameEditText)
        val dateOfBirth = findViewById<EditText>(R.id.dateOfBirthEditText)
        val faculty = findViewById<AutoCompleteTextView>(R.id.facultyEditText)
        val year = findViewById<AutoCompleteTextView>(R.id.yearEditText)
        val email = findViewById<EditText>(R.id.emailEditText)
        val password = findViewById<EditText>(R.id.passwordEditText)
        val confirmPassword = findViewById<EditText>(R.id.confirmPasswordEditText)
        val registerButton = findViewById<Button>(R.id.registerButton)
        val loginRedirect = findViewById<TextView>(R.id.loginRedirect)

        val faculties = listOf(
            "Agronomski i prehrambeno-tehnološki fakultet",
            "Akademija likovnih umjetnosti",
            "Ekonomski fakultet",
            "Fakultet prirodoslovno-matematičkih i odgojnih znanosti",
            "Fakultet strojarstva, računarstva i elektrotehnike",
            "Fakultet zdravstvenih studija",
            "Farmaceutski fakultet",
            "Filozofski fakultet",
            "Fakultet građevinarstva, arhitekture i geodezije",
            "Medicinski fakultet",
            "Pravni fakultet"
        )
        val facultyAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, faculties)
        faculty.setAdapter(facultyAdapter)
        faculty.setOnClickListener { faculty.showDropDown() }

        val yearOptions = listOf("1", "2", "3", "4", "5", "6")
        val yearAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, yearOptions)
        year.setAdapter(yearAdapter)
        year.setOnClickListener { year.showDropDown() }

        dateOfBirth.setOnClickListener {
            val calendar = Calendar.getInstance()
            val yearNow = calendar.get(Calendar.YEAR)
            val monthNow = calendar.get(Calendar.MONTH)
            val dayNow = calendar.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = DatePickerDialog(this, { _, year, month, day ->
                val selectedCalendar = Calendar.getInstance()
                selectedCalendar.set(year, month, day)

                val dateFormat = SimpleDateFormat("dd.MM.yyyy.", Locale.getDefault())
                val formattedDate = dateFormat.format(selectedCalendar.time)
                dateOfBirth.setText(formattedDate)
            }, yearNow - 18, monthNow, dayNow)

            calendar.add(Calendar.YEAR, -18)
            datePickerDialog.datePicker.maxDate = calendar.timeInMillis

            datePickerDialog.setOnShowListener {
                try {
                    val headerId = resources.getIdentifier("date_picker_header_year", "id", "android")
                    val headerView = datePickerDialog.findViewById<View>(headerId)
                    headerView?.performClick()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            datePickerDialog.show()
        }

        registerButton.setOnClickListener {
            val firstNameText = firstName.text.toString().trim()
            val lastNameText = lastName.text.toString().trim()
            val dobText = dateOfBirth.text.toString().trim()
            val facultyText = faculty.text.toString().trim()
            val yearText = year.text.toString().trim()
            val emailText = email.text.toString().trim()
            val passwordText = password.text.toString().trim()
            val confirmPasswordText = confirmPassword.text.toString().trim()

            if (firstNameText.isEmpty() || lastNameText.isEmpty() || dobText.isEmpty() ||
                facultyText.isEmpty() || yearText.isEmpty() ||
                emailText.isEmpty() || passwordText.isEmpty() || confirmPasswordText.isEmpty()) {
                Toast.makeText(this, "Ispunite sva polja", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!emailText.endsWith(".sum.ba")) {
                Toast.makeText(this, "Registracija je moguća samo s email adresama od fakulteta", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (passwordText != confirmPasswordText) {
                Toast.makeText(this, "Lozinke se ne podudaraju", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val yearInt = yearText.toIntOrNull()
            if (yearInt == null || yearInt !in 1..6) {
                Toast.makeText(this, "Godina mora biti broj od 1 do 6", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val dobFormat = SimpleDateFormat("dd.MM.yyyy.", Locale.getDefault())
            try {
                val birthDate = dobFormat.parse(dobText)
                val minAdultDate = Calendar.getInstance().apply { add(Calendar.YEAR, -18) }

                if (birthDate == null || birthDate.after(minAdultDate.time)) {
                    Toast.makeText(this, "Morate imati barem 18 godina da biste se registrirali.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Neispravan format datuma.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(emailText, passwordText)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val uid = auth.currentUser?.uid
                        val userMap = mapOf(
                            "firstName" to firstNameText,
                            "lastName" to lastNameText,
                            "dateOfBirth" to dobText,
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

        val fullText = "Already have an account? Log in"
        val spannable = SpannableString(fullText)
        val start = fullText.indexOf("Log in")
        val end = fullText.length
        val greenColor = Color.parseColor("#2C5F2D")

        spannable.setSpan(StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannable.setSpan(ForegroundColorSpan(greenColor), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

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
