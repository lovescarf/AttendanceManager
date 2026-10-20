package com.example.attendancemanager

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView

class RoleSelectionActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_role_selection)

        val prefs = getSharedPreferences("attendance_prefs", Context.MODE_PRIVATE)

        val cardTeacher = findViewById<MaterialCardView>(R.id.cardTeacher)
        val cardStudent = findViewById<MaterialCardView>(R.id.cardStudent)

        cardTeacher.setOnClickListener {
            val savedTeacherName = prefs.getString("teacher_name", "") ?: ""
            showUsernameDialog(
                title = "Enter Teacher Name",
                hint = "e.g. Prof. Alan Smith",
                defaultName = savedTeacherName
            ) { enteredName ->
                prefs.edit()
                    .putString("user_name", enteredName)
                    .putString("teacher_name", enteredName)
                    .putString("active_role", "teacher")
                    .apply()

                val intent = Intent(this, TeacherDashboardActivity::class.java)
                startActivity(intent)
            }
        }

        cardStudent.setOnClickListener {
            val savedStudentName = prefs.getString("student_name", "") ?: ""
            showUsernameDialog(
                title = "Enter Student Name",
                hint = "e.g. John Doe",
                defaultName = savedStudentName
            ) { enteredName ->
                prefs.edit()
                    .putString("user_name", enteredName)
                    .putString("student_name", enteredName)
                    .putString("active_role", "student")
                    .apply()

                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
            }
        }
    }

    private fun showUsernameDialog(
        title: String,
        hint: String,
        defaultName: String,
        onNameSubmitted: (String) -> Unit
    ) {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 24)
        }

        val input = EditText(this).apply {
            this.hint = hint
            if (defaultName.isNotEmpty()) {
                setText(defaultName)
            }
        }
        container.addView(input)

        AlertDialog.Builder(this)
            .setTitle(title)
            .setView(container)
            .setPositiveButton("Continue") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    onNameSubmitted(name)
                } else {
                    Toast.makeText(this, "Username cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
