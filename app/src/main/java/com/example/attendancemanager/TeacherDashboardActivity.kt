package com.example.attendancemanager

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID

class TeacherDashboardActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var adapter: TeacherClassAdapter

    private lateinit var tvTeacherInfo: TextView
    private lateinit var tvEmptyState: TextView
    private lateinit var rvClasses: RecyclerView
    private lateinit var fabCreateClass: FloatingActionButton
    private lateinit var btnSignOut: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_teacher_dashboard)

        db = FirebaseFirestore.getInstance()

        tvTeacherInfo = findViewById(R.id.tvTeacherInfo)
        tvEmptyState = findViewById(R.id.tvEmptyState)
        rvClasses = findViewById(R.id.rvClasses)
        fabCreateClass = findViewById(R.id.fabCreateClass)
        btnSignOut = findViewById(R.id.btnSignOut)

        val prefs = getSharedPreferences("attendance_prefs", Context.MODE_PRIVATE)
        val teacherName = prefs.getString("user_name", "Teacher") ?: "Teacher"

        btnSignOut.text = "Switch Role"
        tvTeacherInfo.text = "Welcome, $teacherName"

        btnSignOut.setOnClickListener {
            val intent = Intent(this, RoleSelectionActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }

        adapter = TeacherClassAdapter { clickedClass ->
            val intent = Intent(this, TeacherClassDetailsActivity::class.java).apply {
                putExtra("CLASS_ID", clickedClass.classId)
                putExtra("CLASS_NAME", clickedClass.className)
                putExtra("CLASS_CODE", clickedClass.classCode)
            }
            startActivity(intent)
        }

        rvClasses.layoutManager = LinearLayoutManager(this)
        rvClasses.adapter = adapter

        fabCreateClass.setOnClickListener {
            showCreateClassDialog()
        }

        fetchTeacherClasses()
    }

    private fun fetchTeacherClasses() {
        db.collection("classes")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Toast.makeText(this, "Failed to load classes: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val classList = mutableListOf<ClassModel>()
                    for (doc in snapshot.documents) {
                        val classId = doc.id
                        val className = doc.getString("className") ?: ""
                        val subjectCode = doc.getString("subjectCode") ?: ""
                        val classCode = doc.getString("classCode") ?: ""
                        val teacherUid = doc.getString("teacherUid") ?: "teacher"
                        val teacherName = doc.getString("teacherName") ?: "Teacher"
                        val studentCount = doc.getLong("studentCount")?.toInt() ?: 0

                        classList.add(
                            ClassModel(
                                classId = classId,
                                className = className,
                                subjectCode = subjectCode,
                                classCode = classCode,
                                teacherUid = teacherUid,
                                teacherName = teacherName,
                                studentCount = studentCount
                            )
                        )
                    }

                    if (classList.isEmpty()) {
                        tvEmptyState.visibility = View.VISIBLE
                    } else {
                        tvEmptyState.visibility = View.GONE
                    }

                    adapter.setClasses(classList)
                }
            }
    }

    private fun showCreateClassDialog() {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 24)
        }

        val etClassName = EditText(this).apply {
            hint = "Class Name (e.g. Data Structures)"
        }
        val etSubjectCode = EditText(this).apply {
            hint = "Subject Code (e.g. CS201)"
        }

        container.addView(etClassName)
        container.addView(etSubjectCode)

        AlertDialog.Builder(this)
            .setTitle("Create New Class")
            .setView(container)
            .setPositiveButton("Create") { _, _ ->
                val className = etClassName.text.toString().trim()
                val subjectCode = etSubjectCode.text.toString().trim()

                if (className.isNotEmpty()) {
                    createClassInFirestore(className, subjectCode)
                } else {
                    Toast.makeText(this, "Class Name cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun createClassInFirestore(className: String, subjectCode: String) {
        val prefs = getSharedPreferences("attendance_prefs", Context.MODE_PRIVATE)
        val teacherName = prefs.getString("user_name", "Teacher") ?: "Teacher"
        val teacherUid = "teacher_${teacherName.lowercase().replace(" ", "_")}"

        // Generate unique 6-character uppercase class code
        val generatedClassCode = UUID.randomUUID().toString().substring(0, 6).uppercase()

        val classDocRef = db.collection("classes").document()
        val classData = hashMapOf<String, Any>(
            "classId" to classDocRef.id,
            "className" to className,
            "subjectCode" to if (subjectCode.isEmpty()) "COURSE" else subjectCode,
            "classCode" to generatedClassCode,
            "teacherUid" to teacherUid,
            "teacherName" to teacherName,
            "studentCount" to 0,
            "createdAt" to com.google.firebase.Timestamp.now()
        )

        classDocRef.set(classData)
            .addOnSuccessListener {
                Toast.makeText(this, "Class created! Code: $generatedClassCode", Toast.LENGTH_LONG).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error creating class: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
