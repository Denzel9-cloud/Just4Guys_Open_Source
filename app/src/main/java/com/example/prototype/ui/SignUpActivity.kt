package com.example.prototype.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.prototype.R
import com.example.prototype.data.PaymentPlannerRepository
import com.example.prototype.data.entity.Category
import kotlinx.coroutines.launch

class SignUpActivity : AppCompatActivity() {
    private lateinit var repository: PaymentPlannerRepository
    private lateinit var etUsername: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var btnRegister: Button
    private lateinit var tvLoginLink: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        repository = PaymentPlannerRepository(this)

        // Initialize views
        etUsername = findViewById(R.id.etUsername)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        btnRegister = findViewById(R.id.btnRegister)
        tvLoginLink = findViewById(R.id.tvLoginLink)

        setupClickListeners()
    }

    private fun setupClickListeners() {
        btnRegister.setOnClickListener {
            val username = etUsername.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString()
            val confirmPassword = etConfirmPassword.text.toString()

            // Validation
            if (username.isEmpty()) {
                etUsername.error = "Username required"
                return@setOnClickListener
            }

            if (email.isEmpty()) {
                etEmail.error = "Email required"
                return@setOnClickListener
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etEmail.error = "Valid email required"
                return@setOnClickListener
            }

            if (password.isEmpty()) {
                etPassword.error = "Password required"
                return@setOnClickListener
            }

            if (password.length < 6) {
                etPassword.error = "Password must be at least 6 characters"
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                etConfirmPassword.error = "Passwords do not match"
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val existingUser = repository.getUserByUsername(username)
                if (existingUser != null) {
                    Toast.makeText(this@SignUpActivity, "Username already exists", Toast.LENGTH_SHORT).show()
                } else {
                    val userId = repository.createUser(username, password)

                    // Create default categories
                    createDefaultCategories(userId)

                    Toast.makeText(this@SignUpActivity, "Account created successfully! Please login.", Toast.LENGTH_LONG).show()
                    startActivity(Intent(this@SignUpActivity, LoginActivity::class.java))
                    finish()
                }
            }
        }

        tvLoginLink.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private suspend fun createDefaultCategories(userId: Long) {
        val defaultCategories = listOf(
            Category(name = "Food", color = 0xFFFF5722.toInt()),
            Category(name = "Transport", color = 0xFF2196F3.toInt()),
            Category(name = "Entertainment", color = 0xFF9C27B0.toInt()),
            Category(name = "Utilities", color = 0xFF4CAF50.toInt()),
            Category(name = "Shopping", color = 0xFFFF9800.toInt()),
            Category(name = "Healthcare", color = 0xFFE91E63.toInt()),
            Category(name = "Education", color = 0xFF3F51B5.toInt()),
            Category(name = "Dining Out", color = 0xFF795548.toInt())
        )

        defaultCategories.forEach { category ->
            repository.addCategory(category)
        }
    }
}