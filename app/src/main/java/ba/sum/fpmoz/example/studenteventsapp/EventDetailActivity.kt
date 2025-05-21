package ba.sum.fpmoz.example.studenteventsapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import ba.sum.fpmoz.example.studenteventsapp.Event


class EventDetailActivity : AppCompatActivity() {

    private lateinit var imageView: ImageView
    private lateinit var titleView: TextView
    private lateinit var longDescriptionView: TextView
    private lateinit var yearView: TextView
    private lateinit var dateView: TextView
    private lateinit var placeView: TextView
    private lateinit var timeView: TextView
    private lateinit var interestedCountView: TextView
    private lateinit var notInterestedCountView: TextView
    private lateinit var editButton: Button
    private lateinit var deleteButton: Button
    private lateinit var facultyView: TextView

    private lateinit var auth: FirebaseAuth
    private var eventId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_event_detail)

        auth = FirebaseAuth.getInstance()

        imageView = findViewById(R.id.imageDetail)
        titleView = findViewById(R.id.textTitleDetail)
        longDescriptionView = findViewById(R.id.textLongDescription)
        yearView = findViewById(R.id.textYearDetail)
        dateView = findViewById(R.id.textDateDetail)
        placeView = findViewById(R.id.textPlaceDetail)
        timeView = findViewById(R.id.textTimeDetail)
        interestedCountView = findViewById(R.id.textInterestedDetail)
        notInterestedCountView = findViewById(R.id.textNotInterestedDetail)
        facultyView = findViewById(R.id.textFacultyDetail)
        editButton = findViewById(R.id.buttonEditEvent)
        deleteButton = findViewById(R.id.buttonDeleteEvent)

        eventId = intent.getStringExtra("eventId") ?: return
        val title = intent.getStringExtra("title") ?: ""
        val year = intent.getIntExtra("year", 0)
        val date = intent.getStringExtra("date") ?: ""
        val imageUrl = intent.getStringExtra("imageUrl")
        val interestedCount = intent.getIntExtra("interestedCount", 0)
        val notInterestedCount = intent.getIntExtra("notInterestedCount", 0)

        titleView.text = title
        interestedCountView.text = "✅ Zainteresirani: $interestedCount"
        notInterestedCountView.text = "❌ Nisu zainteresirani: $notInterestedCount"

        when (title) {
            "Brunch & Board Games" -> {
                yearView.text = "🎓 Godina studija: Za sve godine studija"
                dateView.text = "📅 Datum održavanja: 22.05.2025."
                placeView.text = "📍 Mjesto: Campus Caffe, Mostar"
                timeView.text = "🕔 Vrijeme: 17:00h"
                facultyView.text = "🏫 Fakultet: Za sve fakultete"
                longDescriptionView.text = """
                    📝 Opis:
                    Pridruži nam se na opuštenom druženju uz društvene igre i fini brunch!
                    Na meniju su Uno, Alijas, Monopol, Catan, Šah i još mnogo drugih igara koje spajaju ljude – savršeno za razbijanje stresa i upoznavanje kolega sa fakulteta.
                    Bit će hrane, smijeha i možda malo natjecateljskog duha.
                    Dođi sam/a ili povedi društvo – igre i dobro raspoloženje su obezbijeđeni!
                """.trimIndent()
            }

            "HackForChange Hackathon" -> {
                titleView.text = "HackForChange Hackathon"
                yearView.text = "🎓 Godina studija: 3"
                dateView.text = "📅 Datum održavanja: 26.05.2025."
                placeView.text = "📍 Mjesto: FPMOZ, učionica 108"
                timeView.text = "🕔 Vrijeme: 12:00h"
                facultyView.text = "🏫 Fakultet: Studiji Informatike i Računarstva"
                longDescriptionView.text = """
                    📝 Opis:
                    Vrijeme je da pokažeš da tehnologija može mijenjati svijet – na bolje! 🌍✨
                    HackForChange je hackathon za studente koji žele više od običnog kodiranja – ovdje razvijaš ideje koje pomažu u stvarnom svijetu.
                    Očekuju te inspirativni mentori, odlična ekipa, radna (ali zabavna) atmosfera i naravno – nagrade za najbolje timove! 🏆
                    Sva oprema i materijali su obezbijeđeni – ponesi samo kreativnost i dobru volju!
                """.trimIndent()
            }

            "Silent Disco" -> {
                yearView.text = "🎓 Godina studija: Za sve godine studija"
                dateView.text = "📅 Datum održavanja: 06.06.2025."
                placeView.text = "📍 Mjesto: Charlie bar & snack, Mostar"
                timeView.text = "🕔 Vrijeme: 23:00h"
                facultyView.text = "🏫 Fakultet: Za sve fakultete"
                longDescriptionView.text = """
                    📝 Opis:
                    Ovo nije obična žurka. Ovo je Silent Disco – ples pod slušalicama, bez buke, ali s najjačom energijom u gradu! 🔥
                    Tri različita DJ kanala, bežične slušalice, svjetleći podijum i ti biraš sam/a svoj ritam. Zvuči čudno? Jeste – ali u najboljem mogućem smislu. 🎶
                    Ulaz je besplatan uz studentsku iskaznicu, a za prve prijavljene – piće dobrodošlice. 🍹
                    Dođi i doživi noć koju nećeš zaboraviti!
                """.trimIndent()
            }
        }

        Glide.with(this).load(Uri.parse(imageUrl)).into(imageView)

        val uid = auth.currentUser?.uid
        if (uid != null) {
            FirebaseDatabase.getInstance().getReference("Users").child(uid).child("role")
                .get().addOnSuccessListener {
                    val role = it.getValue(String::class.java)
                    if (role == "admin") {
                        editButton.visibility = View.VISIBLE
                        deleteButton.visibility = View.VISIBLE
                    } else {
                        editButton.visibility = View.GONE
                        deleteButton.visibility = View.GONE
                    }
                }
        } else {
            editButton.visibility = View.GONE
            deleteButton.visibility = View.GONE
        }

        editButton.setOnClickListener {
            val intent = Intent(this, AddEventActivity::class.java)
            intent.putExtra("editMode", true)
            intent.putExtra("eventId", eventId)
            startActivity(intent)
        }

        deleteButton.setOnClickListener {
            FirebaseDatabase.getInstance().getReference("Events").child(eventId).removeValue()
                .addOnSuccessListener {
                    Toast.makeText(this, "Događaj obrisan", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Greška pri brisanju", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
