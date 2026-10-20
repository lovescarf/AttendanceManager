package com.example.attendancemanager

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton

class TeacherClassAdapter(
    private val onClassClick: (ClassModel) -> Unit
) : RecyclerView.Adapter<TeacherClassAdapter.ClassViewHolder>() {

    private var classList: List<ClassModel> = emptyList()

    fun setClasses(newList: List<ClassModel>) {
        this.classList = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClassViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_teacher_class, parent, false)
        return ClassViewHolder(view)
    }

    override fun onBindViewHolder(holder: ClassViewHolder, position: Int) {
        val item = classList[position]
        holder.tvClassName.text = item.className
        holder.tvSubjectCode.text = item.subjectCode
        holder.tvClassCode.text = item.classCode
        holder.tvStudentCount.text = "👥 ${item.studentCount} Students Enrolled"

        holder.btnCopyCode.setOnClickListener {
            val clipboard = holder.itemView.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Class Code", item.classCode)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(holder.itemView.context, "Class Code '${item.classCode}' copied!", Toast.LENGTH_SHORT).show()
        }

        holder.itemView.setOnClickListener {
            onClassClick(item)
        }
    }

    override fun getItemCount(): Int = classList.size

    class ClassViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvClassName: TextView = itemView.findViewById(R.id.tvClassName)
        val tvSubjectCode: TextView = itemView.findViewById(R.id.tvSubjectCode)
        val tvClassCode: TextView = itemView.findViewById(R.id.tvClassCode)
        val tvStudentCount: TextView = itemView.findViewById(R.id.tvStudentCount)
        val btnCopyCode: MaterialButton = itemView.findViewById(R.id.btnCopyCode)
    }
}
