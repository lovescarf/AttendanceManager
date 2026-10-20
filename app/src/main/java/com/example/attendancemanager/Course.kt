package com.example.attendancemanager

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "course_table")
data class Course(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val courseName: String,
    var classesAttended: Int = 0,
    var classesTotal: Int = 0,
    var classesCancelled: Int = 0,
    var semester: String = "Semester 1"
)
