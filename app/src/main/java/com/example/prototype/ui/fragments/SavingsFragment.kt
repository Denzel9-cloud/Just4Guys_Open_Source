package com.example.prototype.ui.fragments

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.prototype.R
import com.example.prototype.data.PaymentPlannerRepository
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.*

class SavingsFragment : Fragment() {
    private lateinit var repository: PaymentPlannerRepository
    private lateinit var sharedPrefs: SharedPreferences

    private lateinit var tvGoalAmount: TextView
    private lateinit var tvProgress: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvSaved: TextView
    private lateinit var tvRemaining: TextView
    private lateinit var tvMonthsLeft: TextView
    private lateinit var btnUpdateSavings: MaterialButton

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_savings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        repository = PaymentPlannerRepository(requireContext())
        sharedPrefs = requireContext().getSharedPreferences("payment_planner_prefs", Context.MODE_PRIVATE)

        initializeViews(view)
        loadSavingsData()

        btnUpdateSavings.setOnClickListener {
            showUpdateSavingsDialog()
        }
    }

    private fun initializeViews(view: View) {
        tvGoalAmount = view.findViewById(R.id.tvGoalAmount)
        tvProgress = view.findViewById(R.id.tvProgress)
        progressBar = view.findViewById(R.id.progressBar)
        tvSaved = view.findViewById(R.id.tvSaved)
        tvRemaining = view.findViewById(R.id.tvRemaining)
        tvMonthsLeft = view.findViewById(R.id.tvMonthsLeft)
        btnUpdateSavings = view.findViewById(R.id.btnUpdateSavings)
    }

    private fun loadSavingsData() {
        lifecycleScope.launch {
            val userId = sharedPrefs.getLong("current_user_id", -1)
            if (userId == -1L) return@launch

            val budgetGoal = repository.getBudgetGoalForCurrentMonth(userId)
            val targetSavings = budgetGoal?.targetSavings ?: 10000.0

            // Get current savings (from expenses or user input)
            // For now, using a sample value
            val currentSavings = 2000.0

            val progressPercent = if (targetSavings > 0) {
                ((currentSavings / targetSavings) * 100).coerceIn(0.0, 100.0)
            } else 0.0

            val formatter = NumberFormat.getCurrencyInstance(Locale("en", "ZA"))
            formatter.currency = java.util.Currency.getInstance("ZAR")

            tvGoalAmount.text = formatter.format(targetSavings)
            tvProgress.text = "${String.format("%.0f", progressPercent)}%"
            progressBar.progress = progressPercent.toInt()
            tvSaved.text = formatter.format(currentSavings)
            tvRemaining.text = formatter.format(targetSavings - currentSavings)

            val monthsLeft = if (currentSavings > 0 && targetSavings > currentSavings) {
                val monthlySaving = currentSavings / 12
                ((targetSavings - currentSavings) / monthlySaving).toInt().coerceAtLeast(1)
            } else 0
            tvMonthsLeft.text = if (monthsLeft > 0) monthsLeft.toString() else "Goal reached!"
        }
    }

    private fun showUpdateSavingsDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_update_savings, null)

        val etCurrentSavings = dialogView.findViewById<TextInputEditText>(R.id.etCurrentSavings)
        val etGoalAmount = dialogView.findViewById<TextInputEditText>(R.id.etGoalAmount)

        lifecycleScope.launch {
            val userId = sharedPrefs.getLong("current_user_id", -1)
            if (userId != -1L) {
                val budgetGoal = repository.getBudgetGoalForCurrentMonth(userId)
                etCurrentSavings.setText("2000")
                etGoalAmount.setText(budgetGoal?.targetSavings?.toString() ?: "10000")
            }
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Update Savings")
            .setView(dialogView)
            .setPositiveButton("Update") { _, _ ->
                val currentSavings = etCurrentSavings.text.toString().toDoubleOrNull() ?: 0.0
                val goalAmount = etGoalAmount.text.toString().toDoubleOrNull() ?: 10000.0

                lifecycleScope.launch {
                    val userId = sharedPrefs.getLong("current_user_id", -1)
                    if (userId != -1L) {
                        val existingGoal = repository.getBudgetGoalForCurrentMonth(userId)
                        if (existingGoal != null) {
                            repository.saveBudgetGoal(
                                userId,
                                existingGoal.minGoal,
                                existingGoal.maxGoal,
                                goalAmount
                            )
                        } else {
                            repository.saveBudgetGoal(userId, 3000.0, 5000.0, goalAmount)
                        }
                        android.widget.Toast.makeText(requireContext(), "Savings goal updated!", android.widget.Toast.LENGTH_SHORT).show()
                        loadSavingsData()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}