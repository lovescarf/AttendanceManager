package com.example.attendancemanager

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface CourseDao {
    @Insert
    suspend fun insertCourse(course: Course)

    @Update
    suspend fun updateCourse(course: Course)

    @Delete
    suspend fun deleteCourse(course: Course)

    @Query("SELECT * FROM course_table ORDER BY id ASC")
    fun getAllCourses(): LiveData<List<Course>>

    @Query("SELECT * FROM course_table WHERE semester = :semester ORDER BY id ASC")
    fun getCoursesBySemester(semester: String): LiveData<List<Course>>

    @Query("SELECT DISTINCT semester FROM course_table ORDER BY semester ASC")
    fun getAllSemesters(): LiveData<List<String>>
}