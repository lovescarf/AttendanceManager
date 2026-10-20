package com.example.attendancemanager

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.Menu
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.LiveData
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var database: AppDatabase
    private lateinit var adapter: CourseAdapter

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var btnMenu: ImageButton
    private lateinit var btnDarkModeToggle: ImageButton
    private lateinit var tvSelectedSemester: TextView

    private lateinit var prefs: SharedPreferences
    private var currentSemester: String = "Semester 1"
    private var semesterList: MutableList<String> = mutableListOf(
        "Semester 1", "Semester 2", "Semester 3", "Semester 4"
    )

    private var currentCourseObserver: LiveData<List<Course>>? = null

    // Register ZXing QR Code Scanner Launcher
    private val qrScanLauncher = registerForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            handleScannedQrResult(result.contents)
        } else {
            Toast.makeText(this, "QR Scan Cancelled", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Load dark mode preference before view creation
        prefs = getSharedPreferences("attendance_prefs", Context.MODE_PRIVATE)
        val isDarkMode = prefs.getBoolean("is_dark_mode", false)
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        database = AppDatabase.getDatabase(this)

        // Initialize UI Views
        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.navigationView)
        btnMenu = findViewById(R.id.btnMenu)
        btnDarkModeToggle = findViewById(R.id.btnDarkModeToggle)
        tvSelectedSemester = findViewById(R.id.tvSelectedSemester)

        // Set student header welcome name
        val headerView = navigationView.getHeaderView(0)
        val tvStudentHeaderName = headerView?.findViewById<TextView>(R.id.tvStudentHeaderName)
        val studentName = prefs.getString("user_name", "Student") ?: "Student"
        tvStudentHeaderName?.text = "Welcome, $studentName"

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        val fabAdd = findViewById<FloatingActionButton>(R.id.fabAdd)

        // Saved active semester
        currentSemester = prefs.getString("selected_semester", "Semester 1") ?: "Semester 1"
        tvSelectedSemester.text = "📁 $currentSemester"

        // Update dark mode icon state
        updateDarkModeToggleIcon(isDarkMode)

        // Dark mode button click
        btnDarkModeToggle.setOnClickListener {
            val newDarkMode = !prefs.getBoolean("is_dark_mode", false)
            prefs.edit().putBoolean("is_dark_mode", newDarkMode).apply()
            if (newDarkMode) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        }

        // Open Sidebar Drawer
        btnMenu.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        // Course Adapter Setup
        adapter = CourseAdapter(
            onPresentClicked = { course ->
                course.classesAttended += 1
                course.classesTotal += 1
                lifecycleScope.launch {
                    database.courseDao().updateCourse(course)
                }
            },
            onAbsentClicked = { course ->
                course.classesTotal += 1
                lifecycleScope.launch {
                    database.courseDao().updateCourse(course)
                }
            },
            onCancelledClicked = { course ->
                course.classesCancelled += 1
                lifecycleScope.launch {
                    database.courseDao().updateCourse(course)
                }
            },
            onDeleteClicked = { course ->
                showDeleteCourseConfirmationDialog(course)
            }
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // Setup Sidebar Menu items
        setupSidebarMenu()

        // Observe courses for the currently selected semester
        observeSemesterCourses(currentSemester)

        // Observe custom semesters saved in Room DB to dynamically expand sidebar list
        database.courseDao().getAllSemesters().observe(this) { dbSemesters ->
            for (sem in dbSemesters) {
                if (sem.isNotBlank() && !semesterList.contains(sem)) {
                    semesterList.add(sem)
                }
            }
            setupSidebarMenu()
        }

        // Tap '+' FAB button to add a new course or scan QR
        fabAdd.setOnClickListener {
            showAddOrScanOptions()
        }
    }

    private fun updateDarkModeToggleIcon(isDarkMode: Boolean) {
        if (isDarkMode) {
            btnDarkModeToggle.setImageResource(R.drawable.ic_moon)
        } else {
            btnDarkModeToggle.setImageResource(R.drawable.ic_sun)
        }
    }

    private fun observeSemesterCourses(semesterName: String) {
        currentCourseObserver?.removeObservers(this)
        val newLiveData = database.courseDao().getCoursesBySemester(semesterName)
        currentCourseObserver = newLiveData
        newLiveData.observe(this) { courseList ->
            adapter.setCourses(courseList)
        }
    }

    private fun setupSidebarMenu() {
        val menu = navigationView.menu
        menu.clear()

        val semesterGroup = menu.addSubMenu("Semesters")
        for (i in semesterList.indices) {
            val semName = semesterList[i]
            val menuItem = semesterGroup.add(Menu.NONE, i + 100, Menu.NONE, semName)
            menuItem.setIcon(R.drawable.ic_folder)
        }

        val actionGroup = menu.addSubMenu("Actions")
        val scanQrItem = actionGroup.add(Menu.NONE, 888, Menu.NONE, "📱 Scan QR Attendance")
        scanQrItem.setIcon(R.drawable.ic_add)

        val addSemItem = actionGroup.add(Menu.NONE, 999, Menu.NONE, "+ Add New Semester")
        addSemItem.setIcon(R.drawable.ic_add)

        val switchRoleItem = actionGroup.add(Menu.NONE, 777, Menu.NONE, "Switch Role")
        switchRoleItem.setIcon(R.drawable.ic_delete)

        navigationView.setNavigationItemSelectedListener { menuItem ->
            drawerLayout.closeDrawer(GravityCompat.START)
            when (menuItem.itemId) {
                888 -> startQrScanner()
                999 -> showAddSemesterDialog()
                777 -> {
                    val intent = Intent(this, RoleSelectionActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                    finish()
                }
                else -> {
                    val selectedName = menuItem.title.toString()
                    currentSemester = selectedName
                    prefs.edit().putString("selected_semester", currentSemester).apply()
                    tvSelectedSemester.text = "📁 $currentSemester"
                    observeSemesterCourses(currentSemester)
                }
            }
            true
        }
    }

    private fun showAddOrScanOptions() {
        val options = arrayOf("Add Subject Manually", "Scan Teacher's QR Code")
        AlertDialog.Builder(this)
            .setTitle("Class Options")
            .setItems(options) { _, which ->
                if (which == 0) {
                    showAddCourseDialog()
                } else {
                    startQrScanner()
                }
            }
            .show()
    }

    private fun startQrScanner() {
        val options = ScanOptions().apply {
            setPrompt("Align QR Code within the frame to mark attendance")
            setCameraId(0)
            setBeepEnabled(true)
            setBarcodeImageEnabled(false)
            setOrientationLocked(false)
        }
        qrScanLauncher.launch(options)
    }

    private fun handleScannedQrResult(scannedData: String) {
        if (!scannedData.startsWith("ATTENDANCE_QR:")) {
            Toast.makeText(this, "Invalid Attendance QR Code", Toast.LENGTH_SHORT).show()
            return
        }

        // Format: ATTENDANCE_QR:className:classCode:date
        val payload = scannedData.substringAfter("ATTENDANCE_QR:")
        val parts = payload.split(":")

        val className = parts.getOrNull(0) ?: "Scanned Class"
        val classCode = parts.getOrNull(1) ?: ""
        val fullCourseTitle = if (classCode.isNotEmpty()) "$className ($classCode)" else className

        lifecycleScope.launch {
            val existingCourses = database.courseDao().getCoursesBySemester(currentSemester).value ?: emptyList()
            val match = existingCourses.find {
                it.courseName.equals(fullCourseTitle, ignoreCase = true) || it.courseName.equals(className, ignoreCase = true)
            }

            if (match != null) {
                match.classesAttended += 1
                match.classesTotal += 1
                database.courseDao().updateCourse(match)
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("Attendance Marked! ✅")
                    .setMessage("Marked PRESENT for '$className'.\nAttended: ${match.classesAttended}/${match.classesTotal}")
                    .setPositiveButton("OK", null)
                    .show()
            } else {
                val newCourse = Course(
                    courseName = fullCourseTitle,
                    classesAttended = 1,
                    classesTotal = 1,
                    semester = currentSemester
                )
                database.courseDao().insertCourse(newCourse)
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("Enrolled & Attendance Marked! ✅")
                    .setMessage("Added '$className' and marked PRESENT for today.")
                    .setPositiveButton("OK", null)
                    .show()
            }
        }
    }

    private fun showAddSemesterDialog() {
        val input = EditText(this)
        input.hint = "e.g., Semester 5"

        AlertDialog.Builder(this)
            .setTitle("Add New Semester")
            .setView(input)
            .setPositiveButton("Add") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    if (!semesterList.contains(name)) {
                        semesterList.add(name)
                    }
                    currentSemester = name
                    prefs.edit().putString("selected_semester", currentSemester).apply()
                    tvSelectedSemester.text = "📁 $currentSemester"
                    setupSidebarMenu()
                    observeSemesterCourses(currentSemester)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showAddCourseDialog() {
        val input = EditText(this)
        input.hint = "Enter Subject Name"

        AlertDialog.Builder(this)
            .setTitle("Add New Course ($currentSemester)")
            .setView(input)
            .setPositiveButton("Add") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    lifecycleScope.launch {
                        database.courseDao().insertCourse(
                            Course(
                                courseName = name,
                                semester = currentSemester
                            )
                        )
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteCourseConfirmationDialog(course: Course) {
        AlertDialog.Builder(this)
            .setTitle("Remove Class")
            .setMessage("Are you sure you want to remove '${course.courseName}'?")
            .setPositiveButton("Remove") { _, _ ->
                lifecycleScope.launch {
                    database.courseDao().deleteCourse(course)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
