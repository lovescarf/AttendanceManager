package com.example.attendancemanager

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButtonToggleGroup

class DailyRegisterAdapter : RecyclerView.Adapter<DailyRegisterAdapter.RegisterViewHolder>() {

    private var students: List<StudentModel> = emptyList()

    fun setStudents(newList: List<StudentModel>) {
        this.students = newList
        notifyDataSetChanged()
    }

    fun getStudents(): List<StudentModel> = students

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RegisterViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_student_register, parent, false)
        return RegisterViewHolder(view)
    }

    override fun onBindViewHolder(holder: RegisterViewHolder, position: Int) {
        val student = students[position]
        holder.tvStudentName.text = student.studentName
        holder.tvStudentStats.text = "Attended: ${student.classesAttended} / ${student.classesTotal}"

        val pct = student.attendancePercentage
        holder.tvPercentage.text = String.format("%.1f%%", pct)

        // Low Attendance Alert: Flag < 75% in Red!
        if (pct < 75.0f && student.classesTotal > 0) {
            holder.tvPercentage.text = String.format("%.1f%% (⚠️ Low Attendance)", pct)
            holder.tvPercentage.setTextColor(Color.parseColor("#D32F2F")) // Material Red
        } else {
            holder.tvPercentage.setTextColor(Color.parseColor("#2E7D32")) // Material Green
        }

        // Set toggle group state
        when (student.currentStatus) {
            "LATE" -> holder.toggleStatusGroup.check(R.id.btnLate)
            "ABSENT" -> holder.toggleStatusGroup.check(R.id.btnAbsent)
            else -> holder.toggleStatusGroup.check(R.id.btnPresent)
        }

        holder.toggleStatusGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.btnPresent -> student.currentStatus = "PRESENT"
                    R.id.btnLate -> student.currentStatus = "LATE"
                    R.id.btnAbsent -> student.currentStatus = "ABSENT"
                }
            }
        }
    }

    override fun getItemCount(): Int = students.size

    class RegisterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvStudentName: TextView = itemView.findViewById(R.id.tvStudentName)
        val tvStudentStats: TextView = itemView.findViewById(R.id.tvStudentStats)
        val tvPercentage: TextView = itemView.findViewById(R.id.tvPercentage)
        val toggleStatusGroup: MaterialButtonToggleGroup = itemView.findViewById(R.id.toggleStatusGroup)
    }
}
