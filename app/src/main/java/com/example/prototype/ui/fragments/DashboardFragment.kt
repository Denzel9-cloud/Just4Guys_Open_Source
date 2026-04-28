package com.example.prototype.ui.fragments

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.prototype.R
import com.example.prototype.data.PaymentPlannerRepository
import com.example.prototype.data.entity.Category
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import java.util.*

class DashboardFragment : Fragment() {
    private lateinit var repository: PaymentPlannerRepository
    private lateinit var sharedPrefs: SharedPreferences

    // View references
    private lateinit var tvSalaryValue: TextView
    private lateinit var tvExpensesValue: TextView
    private lateinit var tvSavingsValue: TextView
    private lateinit var tvTargetValue: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvProgressText: TextView
    private lateinit var tvPercentage: TextView
    private lateinit var tvSavingsRate: TextView
    private lateinit var tvMonthsToGoal: TextView
    private lateinit var tvExpensesBreakdown: TextView
    private lateinit var tvRemainingBreakdown: TextView
    private lateinit var tvTopCategory: TextView
    private lateinit var btnEditBudget: MaterialButton
    private lateinit var btnAddExpense: MaterialButton

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        repository = PaymentPlannerRepository(requireContext())
        sharedPrefs = requireContext().getSharedPreferences("payment_planner_prefs", Context.MODE_PRIVATE)

        initializeViews(view)
        loadDashboardData()
        setupClickListeners()
    }

    private fun initializeViews(view: View) {
        tvSalaryValue = view.findViewById(R.id.tvSalaryValue)
        tvExpensesValue = view.findViewById(R.id.tvExpensesValue)
        tvSavingsValue = view.findViewById(R.id.tvSavingsValue)
        tvTargetValue = view.findViewById(R.id.tvTargetValue)
        progressBar = view.findViewById(R.id.progressBar)
        tvProgressText = view.findViewById(R.id.tvProgressText)
        tvPercentage = view.findViewById(R.id.tvPercentage)
        tvSavingsRate = view.findViewById(R.id.tvSavingsRate)
        tvMonthsToGoal = view.findViewById(R.id.tvMonthsToGoal)
        tvExpensesBreakdown = view.findViewById(R.id.tvExpensesBreakdown)
        tvRemainingBreakdown = view.findViewById(R.id.tvRemainingBreakdown)
        tvTopCategory = view.findViewById(R.id.tvTopCategory)
        btnEditBudget = view.findViewById(R.id.btnEditBudget)
        btnAddExpense = view.findViewById(R.id.btnAddExpense)
    }

    private fun setupClickListeners() {
        btnEditBudget.setOnClickListener {
            showBudgetDialog()
        }

        btnAddExpense.setOnClickListener {
            // Navigate to expenses fragment or show dialog
            android.widget.Toast.makeText(requireContext(), "Add expense feature - use bottom navigation", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadDashboardData() {
        lifecycleScope.launch {
            val userId = sharedPrefs.getLong("current_user_id", -1)
            if (userId == -1L) return@launch

            val calendar = Calendar.getInstance()
            val startOfMonth = getStartOfMonth(calendar.time)
            val endOfMonth = getEndOfMonth(calendar.time)

            val totalExpenses = repository.getTotalExpensesForPeriod(userId, startOfMonth, endOfMonth)
            val budgetGoal = repository.getBudgetGoalForCurrentMonth(userId)

            // Default values (can be customized by user)
            val monthlySalary = budgetGoal?.maxGoal ?: 5000.0
            val remaining = monthlySalary - totalExpenses
            val targetSavings = budgetGoal?.targetSavings ?: 10000.0

            tvSalaryValue.text = "R${String.format("%.0f", monthlySalary)}"
            tvExpensesValue.text = "R${String.format("%.0f", totalExpenses)}"
            tvSavingsValue.text = "R${String.format("%.0f", remaining)}"
            tvTargetValue.text = "R${String.format("%.0f", targetSavings)}"

            // Progress calculation
            val progressPercent = if (targetSavings > 0) {
                ((remaining / targetSavings) * 100).coerceIn(0.0, 100.0)
            } else 0.0

            progressBar.progress = progressPercent.toInt()
            tvProgressText.text = "R${String.format("%.0f", remaining)} of R${String.format("%.0f", targetSavings)}"
            tvPercentage.text = "${String.format("%.0f", progressPercent)}% of your goal reached"

            // Savings rate
            val savingsRate = if (monthlySalary > 0) (remaining / monthlySalary * 100) else 0.0
            tvSavingsRate.text = "${String.format("%.0f", savingsRate)}% Savings rate"

            // Months to goal calculation
            val monthlySavings = remaining / 12
            val monthsToGoal = if (monthlySavings > 0 && targetSavings > remaining) {
                ((targetSavings - remaining) / monthlySavings).toInt()
            } else 0
            tvMonthsToGoal.text = if (monthsToGoal > 0) "$monthsToGoal Months to goal" else "Goal reached!"

            // Budget breakdown
            tvExpensesBreakdown.text = "R${String.format("%.0f", totalExpenses)}"
            tvRemainingBreakdown.text = "R${String.format("%.0f", remaining)}"

            // Category totals for current month
            loadCategoryTotals(startOfMonth, endOfMonth)
        }
    }

    private suspend fun loadCategoryTotals(startDate: Date, endDate: Date) {
        val userId = sharedPrefs.getLong("current_user_id", -1)
        if (userId == -1L) return

        val categoryTotals = repository.getCategoryTotals(userId, startDate, endDate)
        if (categoryTotals.isNotEmpty()) {
            val topCategory = categoryTotals.first()
            tvTopCategory.text = "Top category: ${topCategory.categoryName}: R${String.format("%.0f", topCategory.totalAmount)}"
        } else {
            tvTopCategory.text = "No expenses yet"
        }
    }

    private fun showBudgetDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_budget_setup, null)

        val etMonthlySalary = dialogView.findViewById<TextInputEditText>(R.id.etMonthlySalary)
        val etMonthlyExpenses = dialogView.findViewById<TextInputEditText>(R.id.etMonthlyExpenses)
        val etSavingsGoal = dialogView.findViewById<TextInputEditText>(R.id.etSavingsGoal)

        lifecycleScope.launch {
            val userId = sharedPrefs.getLong("current_user_id", -1)
            if (userId != -1L) {
                val existingGoal = repository.getBudgetGoalForCurrentMonth(userId)
                if (existingGoal != null) {
                    etMonthlySalary.setText(existingGoal.maxGoal.toString())
                    etMonthlyExpenses.setText(existingGoal.minGoal.toString())
                    etSavingsGoal.setText(existingGoal.targetSavings.toString())
                } else {
                    // Set default values from screenshots
                    etMonthlySalary.setText("5000")
                    etMonthlyExpenses.setText("3000")
                    etSavingsGoal.setText("10000")
                }
            }
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Financial Setup")
            .setView(dialogView)
            .setPositiveButton("Save and Continue") { _, _ ->
                val salary = etMonthlySalary.text.toString().toDoubleOrNull() ?: 0.0
                val expenses = etMonthlyExpenses.text.toString().toDoubleOrNull() ?: 0.0
                val savingsGoal = etSavingsGoal.text.toString().toDoubleOrNull() ?: 10000.0

                lifecycleScope.launch {
                    val userId = sharedPrefs.getLong("current_user_id", -1)
                    if (userId != -1L) {
                        // Save both min (expenses) and max (salary) goals
                        repository.saveBudgetGoal(userId, expenses, salary, savingsGoal)
                        android.widget.Toast.makeText(requireContext(), "Budget updated!", android.widget.Toast.LENGTH_SHORT).show()
                        loadDashboardData()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun getStartOfMonth(date: Date): Date {
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.time
    }

    private fun getEndOfMonth(date: Date): Date {
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return calendar.time
    }
}
