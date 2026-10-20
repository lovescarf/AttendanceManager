package com.example.attendancemanager

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.FirebaseFirestore
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TeacherClassDetailsActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var adapter: DailyRegisterAdapter

    private lateinit var tvClassNameHeader: TextView
    private lateinit var tvClassCodeHeader: TextView
    private lateinit var tvDateDisplay: TextView
    private lateinit var tvEmptyStudentsState: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnSaveAttendance: MaterialButton
    private lateinit var btnExportCsv: MaterialButton
    private lateinit var btnGenerateQr: MaterialButton
    private lateinit var btnBack: ImageButton

    private var classId: String = ""
    private var className: String = ""
    private var classCode: String = ""
    private var todayDateString: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_teacher_class_details)

        db = FirebaseFirestore.getInstance()

        classId = intent.getStringExtra("CLASS_ID") ?: ""
        className = intent.getStringExtra("CLASS_NAME") ?: "Class Details"
        classCode = intent.getStringExtra("CLASS_CODE") ?: ""

        todayDateString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        tvClassNameHeader = findViewById(R.id.tvClassNameHeader)
        tvClassCodeHeader = findViewById(R.id.tvClassCodeHeader)
        tvDateDisplay = findViewById(R.id.tvDateDisplay)
        tvEmptyStudentsState = findViewById(R.id.tvEmptyStudentsState)
        progressBar = findViewById(R.id.progressBar)
        btnSaveAttendance = findViewById(R.id.btnSaveAttendance)
        btnExportCsv = findViewById(R.id.btnExportCsv)
        btnGenerateQr = findViewById(R.id.btnGenerateQr)
        btnBack = findViewById(R.id.btnBack)

        tvClassNameHeader.text = className
        tvClassCodeHeader.text = "Code: $classCode"
        tvDateDisplay.text = "📅 Date: $todayDateString"

        btnBack.setOnClickListener { finish() }

        val rvDailyRegister = findViewById<RecyclerView>(R.id.rvDailyRegister)
        adapter = DailyRegisterAdapter()
        rvDailyRegister.layoutManager = LinearLayoutManager(this)
        rvDailyRegister.adapter = adapter

        fetchEnrolledStudents()

        btnSaveAttendance.setOnClickListener {
            saveAttendanceRegister()
        }

        btnExportCsv.setOnClickListener {
            exportAttendanceToCsv()
        }

        btnGenerateQr.setOnClickListener {
            showQrCodeDialog()
        }
    }

    private fun fetchEnrolledStudents() {
        if (classId.isEmpty()) return

        progressBar.visibility = View.VISIBLE
        db.collection("classes").document(classId).collection("students")
            .get()
            .addOnSuccessListener { querySnapshot ->
                progressBar.visibility = View.GONE
                val studentList = mutableListOf<StudentModel>()
                for (doc in querySnapshot.documents) {
                    val uid = doc.id
                    val name = doc.getString("studentName") ?: "Student"
                    val email = doc.getString("studentEmail") ?: ""
                    val attended = doc.getLong("classesAttended")?.toInt() ?: 0
                    val total = doc.getLong("classesTotal")?.toInt() ?: 0

                    studentList.add(
                        StudentModel(
                            studentUid = uid,
                            studentName = name,
                            studentEmail = email,
                            classesAttended = attended,
                            classesTotal = total,
                            currentStatus = "PRESENT"
                        )
                    )
                }

                if (studentList.isEmpty()) {
                    tvEmptyStudentsState.visibility = View.VISIBLE
                } else {
                    tvEmptyStudentsState.visibility = View.GONE
                }

                adapter.setStudents(studentList)
            }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                Toast.makeText(this, "Failed to load students: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveAttendanceRegister() {
        val studentList = adapter.getStudents()
        if (studentList.isEmpty()) {
            Toast.makeText(this, "No students to save", Toast.LENGTH_SHORT).show()
            return
        }

        progressBar.visibility = View.VISIBLE
        btnSaveAttendance.isEnabled = false

        val batch = db.batch()
        val sessionRef = db.collection("classes").document(classId)
            .collection("attendance_sessions").document(todayDateString)

        val sessionData = hashMapOf<String, Any>(
            "date" to todayDateString,
            "savedAt" to com.google.firebase.Timestamp.now()
        )
        batch.set(sessionRef, sessionData)

        for (student in studentList) {
            val studentRef = db.collection("classes").document(classId)
                .collection("students").document(student.studentUid)

            var newAttended = student.classesAttended
            val newTotal = student.classesTotal + 1

            if (student.currentStatus == "PRESENT" || student.currentStatus == "LATE") {
                newAttended += 1
            }

            val updateData = hashMapOf<String, Any>(
                "classesAttended" to newAttended,
                "classesTotal" to newTotal,
                "lastStatus" to student.currentStatus
            )

            batch.update(studentRef, updateData)
        }

        batch.commit()
            .addOnSuccessListener {
                progressBar.visibility = View.GONE
                btnSaveAttendance.isEnabled = true
                Toast.makeText(this, "Attendance register saved successfully! 💾", Toast.LENGTH_SHORT).show()
                fetchEnrolledStudents() // Refresh list with updated stats
            }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                btnSaveAttendance.isEnabled = true
                Toast.makeText(this, "Failed to save register: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun exportAttendanceToCsv() {
        val studentList = adapter.getStudents()
        if (studentList.isEmpty()) {
            Toast.makeText(this, "No student data available to export", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val csvHeader = "Student Name,Email,Classes Attended,Total Classes,Attendance Percentage,Today Status\n"
            val csvData = StringBuilder(csvHeader)

            for (student in studentList) {
                csvData.append("\"${student.studentName}\",")
                csvData.append("\"${student.studentEmail}\",")
                csvData.append("${student.classesAttended},")
                csvData.append("${student.classesTotal},")
                csvData.append("${String.format("%.1f", student.attendancePercentage)}%,")
                csvData.append("${student.currentStatus}\n")
            }

            val fileName = "Attendance_${classCode}_$todayDateString.csv"
            val file = File(cacheDir, fileName)
            val out = FileOutputStream(file)
            out.write(csvData.toString().toByteArray())
            out.close()

            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_SUBJECT, "Attendance Sheet - $className ($classCode)")
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            startActivity(Intent.createChooser(shareIntent, "Share / Export Attendance Sheet"))

        } catch (e: Exception) {
            Toast.makeText(this, "Export Failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showQrCodeDialog() {
        val qrContent = "ATTENDANCE_QR:$className:$classCode:$todayDateString"
        val qrBitmap = generateQrCodeBitmap(qrContent, 600, 600)

        if (qrBitmap == null) {
            Toast.makeText(this, "Could not generate QR Code", Toast.LENGTH_SHORT).show()
            return
        }

        val imageView = ImageView(this).apply {
            setImageBitmap(qrBitmap)
            setPadding(32, 32, 32, 32)
        }

        AlertDialog.Builder(this)
            .setTitle("QR Attendance - $className")
            .setMessage("Students can scan this QR code or enter Class Code '$classCode' to mark attendance!")
            .setView(imageView)
            .setPositiveButton("Close", null)
            .show()
    }

    private fun generateQrCodeBitmap(text: String, width: Int, height: Int): Bitmap? {
        return try {
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, width, height)
            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bmp.setPixel(x, y, if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE)
                }
            }
            bmp
        } catch (e: Exception) {
            null
        }
    }
}
