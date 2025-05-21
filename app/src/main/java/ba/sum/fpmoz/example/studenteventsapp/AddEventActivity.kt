package ba.sum.fpmoz.example.studenteventsapp

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.FirebaseDatabase

class AddEventActivity : AppCompatActivity() {

    private lateinit var titleEditText: EditText
    private lateinit var descriptionEditText: EditText
    private lateinit var yearEditText: EditText
    private lateinit var dateEditText: EditText
    private lateinit var saveButton: Button
    private lateinit var selectImageButton: Button

    private var selectedImageUri: Uri? = null
    private val PICK_IMAGE_REQUEST = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "BACK"
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_event)

        titleEditText = findViewById(R.id.editTextTitle)
        descriptionEditText = findViewById(R.id.editTextDescription)
        yearEditText = findViewById(R.id.editTextYear)
        dateEditText = findViewById(R.id.editTextDate)
        saveButton = findViewById(R.id.buttonSaveEvent)
        selectImageButton = findViewById(R.id.buttonSelectImage)

        selectImageButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"
            startActivityForResult(Intent.createChooser(intent, "Odaberi sliku"), PICK_IMAGE_REQUEST)
        }

        saveButton.setOnClickListener {
            val title = titleEditText.text.toString().trim()
            val description = descriptionEditText.text.toString().trim()
            val year = yearEditText.text.toString().trim()
            val date = dateEditText.text.toString().trim()

            if (title.isEmpty() || description.isEmpty() || year.isEmpty() || date.isEmpty()) {
                Toast.makeText(this, "Ispuni sva polja", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val yearInt = year.toIntOrNull()
            if (yearInt == null || yearInt !in 1..6) {
                Toast.makeText(this, "Godina mora biti od 1 do 6", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val ref = FirebaseDatabase.getInstance().getReference("Events")
            val eventId = ref.push().key ?: return@setOnClickListener

            val imageUrl = selectedImageUri?.toString()
                ?: "android.resource://${packageName}/drawable/download"

            val eventMap = mapOf(
                "title" to title,
                "description" to description,
                "year" to yearInt,
                "date" to date,
                "imageUrl" to imageUrl
            )

            ref.child(eventId).setValue(eventMap)
                .addOnSuccessListener {
                    Toast.makeText(this, "Događaj spremljen!", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Greška pri spremanju", Toast.LENGTH_SHORT).show()
                }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data?.data != null) {
            selectedImageUri = data.data
            Toast.makeText(this, "Slika odabrana!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
