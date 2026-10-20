package com.example.attendancemanager

data class StudentModel(
    val studentUid: String = "",
    val studentName: String = "",
    val studentEmail: String = "",
    var classesAttended: Int = 0,
    var classesTotal: Int = 0,
    var currentStatus: String = "PRESENT" // "PRESENT", "ABSENT", "LATE"
) {
    val attendancePercentage: Float
        get() = if (classesTotal > 0) (classesAttended.toFloat() / classesTotal.toFloat()) * 100f else 0f
}
