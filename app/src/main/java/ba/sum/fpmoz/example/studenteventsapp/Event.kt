package ba.sum.fpmoz.example.studenteventsapp

data class Event(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val year: Int = 0,
    val date: String = "",
    val imageUrl: String = "",
    val interestedCount: Int = 0,
    val notInterestedCount: Int = 0
)
