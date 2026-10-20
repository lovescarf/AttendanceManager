package com.example.attendancemanager

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var tilFullName: TextInputLayout
    private lateinit var etFullName: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var toggleRole: MaterialButtonToggleGroup
    private lateinit var btnSubmit: MaterialButton
    private lateinit var tvToggleMode: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvSubtitle: TextView

    private var isSignUpMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Check if user is already signed in
        val currentUser = auth.currentUser
        if (currentUser != null) {
            checkUserRoleAndRedirect(currentUser.uid)
            return
        }

        setContentView(R.layout.activity_login)

        tilFullName = findViewById(R.id.tilFullName)
        etFullName = findViewById(R.id.etFullName)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        toggleRole = findViewById(R.id.toggleRole)
        btnSubmit = findViewById(R.id.btnSubmit)
        tvToggleMode = findViewById(R.id.tvToggleMode)
        progressBar = findViewById(R.id.progressBar)
        tvSubtitle = findViewById(R.id.tvSubtitle)

        tvToggleMode.setOnClickListener {
            isSignUpMode = !isSignUpMode
            updateUiMode()
        }

        btnSubmit.setOnClickListener {
            handleAuthAction()
        }
    }

    private fun updateUiMode() {
        if (isSignUpMode) {
            tvSubtitle.text = "Create a new account"
            tilFullName.visibility = View.VISIBLE
            btnSubmit.text = "Sign Up"
            tvToggleMode.text = "Already have an account? Sign In"
        } else {
            tvSubtitle.text = "Sign in to your account"
            tilFullName.visibility = View.GONE
            btnSubmit.text = "Sign In"
            tvToggleMode.text = "Don't have an account? Sign Up"
        }
    }

    private fun handleAuthAction() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val fullName = etFullName.text.toString().trim()

        if (email.isEmpty()) {
            etEmail.error = "Email is required"
            return
        }
        if (password.length < 6) {
            etPassword.error = "Password must be at least 6 characters"
            return
        }

        val selectedRole = if (toggleRole.checkedButtonId == R.id.btnRoleTeacher) "teacher" else "student"

        progressBar.visibility = View.VISIBLE
        btnSubmit.isEnabled = false

        if (isSignUpMode) {
            if (fullName.isEmpty()) {
                etFullName.error = "Full Name is required"
                progressBar.visibility = View.GONE
                btnSubmit.isEnabled = true
                return
            }

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val uid = auth.currentUser?.uid ?: ""
                        val userMap = hashMapOf(
                            "uid" to uid,
                            "name" to fullName,
                            "email" to email,
                            "role" to selectedRole
                        )
                        db.collection("users").document(uid).set(userMap)
                            .addOnSuccessListener {
                                progressBar.visibility = View.GONE
                                redirectByRole(selectedRole)
                            }
                            .addOnFailureListener { e ->
                                progressBar.visibility = View.GONE
                                btnSubmit.isEnabled = true
                                Toast.makeText(this, "Failed to save user: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        progressBar.visibility = View.GONE
                        btnSubmit.isEnabled = true
                        Toast.makeText(this, "Sign Up Failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        } else {
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val uid = auth.currentUser?.uid ?: ""
                        checkUserRoleAndRedirect(uid)
                    } else {
                        progressBar.visibility = View.GONE
                        btnSubmit.isEnabled = true
                        Toast.makeText(this, "Login Failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }

    private fun checkUserRoleAndRedirect(uid: String) {
        progressBar.visibility = View.VISIBLE
        db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                progressBar.visibility = View.GONE
                if (document.exists()) {
                    val role = document.getString("role") ?: "student"
                    redirectByRole(role)
                } else {
                    // Default to student if no document exists
                    redirectByRole("student")
                }
            }
            .addOnFailureListener {
                progressBar.visibility = View.GONE
                // Fallback on error to Student dashboard
                redirectByRole("student")
            }
    }

    private fun redirectByRole(role: String) {
        val intent = if (role == "teacher") {
            Intent(this, TeacherDashboardActivity::class.java)
        } else {
            Intent(this, MainActivity::class.java)
        }
        startActivity(intent)
        finish()
    }
}
