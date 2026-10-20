package com.example.attendancemanager

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CourseAdapter(
    private val onPresentClicked: (Course) -> Unit,
    private val onAbsentClicked: (Course) -> Unit,
    private val onCancelledClicked: (Course) -> Unit,
    private val onDeleteClicked: (Course) -> Unit
) : RecyclerView.Adapter<CourseAdapter.CourseViewHolder>() {

    private var courses: List<Course> = emptyList()

    fun setCourses(newCourses: List<Course>) {
        this.courses = newCourses
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CourseViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_course, parent, false)
        return CourseViewHolder(view)
    }

    override fun onBindViewHolder(holder: CourseViewHolder, position: Int) {
        val course = courses[position]
        holder.tvCourseName.text = course.courseName

        // Show all stats including cancelled
        holder.tvAttendanceStats.text = "Attended: ${course.classesAttended} | Total: ${course.classesTotal} | Cancelled: ${course.classesCancelled}"

        val percentage = if (course.classesTotal > 0) {
            (course.classesAttended.toFloat() / course.classesTotal.toFloat()) * 100
        } else {
            0.0f
        }

        holder.tvPercentage.text = String.format("%.1f%%", percentage)

        if (percentage < 75.0f && course.classesTotal > 0) {
            holder.tvPercentage.setTextColor(Color.parseColor("#D32F2F")) // Material Red
        } else {
            holder.tvPercentage.setTextColor(Color.parseColor("#2E7D32")) // Material Green
        }

        holder.btnPresent.setOnClickListener { onPresentClicked(course) }
        holder.btnAbsent.setOnClickListener { onAbsentClicked(course) }
        holder.btnCancelled.setOnClickListener { onCancelledClicked(course) }
        holder.btnDeleteCourse.setOnClickListener { onDeleteClicked(course) }
    }

    override fun getItemCount(): Int = courses.size

    class CourseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvCourseName: TextView = itemView.findViewById(R.id.tvCourseName)
        val tvAttendanceStats: TextView = itemView.findViewById(R.id.tvAttendanceStats)
        val tvPercentage: TextView = itemView.findViewById(R.id.tvPercentage)
        val btnPresent: Button = itemView.findViewById(R.id.btnPresent)
        val btnAbsent: Button = itemView.findViewById(R.id.btnAbsent)
        val btnCancelled: Button = itemView.findViewById(R.id.btnCancelled)
        val btnDeleteCourse: Button = itemView.findViewById(R.id.btnDeleteCourse)
    }
}
