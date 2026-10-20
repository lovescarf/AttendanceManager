package com.example.attendancemanager

data class ClassModel(
    val classId: String = "",
    val className: String = "",
    val subjectCode: String = "",
    val classCode: String = "",
    val teacherUid: String = "",
    val teacherName: String = "",
    val studentCount: Int = 0
)
