package com.example.prototype.ui.fragments

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.prototype.R
import com.example.prototype.data.PaymentPlannerRepository
import com.example.prototype.data.entity.Category
import com.example.prototype.data.entity.Expense
import com.google.android.material.button.MaterialButton
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class ExpensesFragment : Fragment() {
    private lateinit var repository: PaymentPlannerRepository
    private lateinit var sharedPrefs: SharedPreferences
    private lateinit var expenseAdapter: ExpenseAdapter
    private var categories = listOf<Category>()
    private var currentStartDate: Date? = null
    private var currentEndDate: Date? = null
    private var currentPhotoPath: String? = null

    // View references
    private lateinit var rvExpenses: RecyclerView
    private lateinit var tvEmptyState: TextView
    private lateinit var tvTotalExpenses: TextView
    private lateinit var btnDateRange: MaterialButton
    private lateinit var tvDateRange: TextView
    private lateinit var fabAddExpense: FloatingActionButton

    companion object {
        private const val REQUEST_IMAGE_CAPTURE = 1
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_expenses, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repository = PaymentPlannerRepository(requireContext())
        sharedPrefs = requireContext().getSharedPreferences("payment_planner_prefs", Context.MODE_PRIVATE)

        initializeViews(view)
        setupRecyclerView()
        setupDateRangePicker()
        loadCategories()
        loadExpenses()

        fabAddExpense.setOnClickListener {
            showAddExpenseDialog()
        }
    }

    private fun initializeViews(view: View) {
        rvExpenses = view.findViewById(R.id.rvExpenses)
        tvEmptyState = view.findViewById(R.id.tvEmptyState)
        tvTotalExpenses = view.findViewById(R.id.tvTotalExpenses)
        btnDateRange = view.findViewById(R.id.btnDateRange)
        tvDateRange = view.findViewById(R.id.tvDateRange)
        fabAddExpense = view.findViewById(R.id.fabAddExpense)
    }

    private fun setupRecyclerView() {
        expenseAdapter = ExpenseAdapter(
            onItemClick = { expense ->
                showExpenseDetail(expense)
            },
            onDeleteClick = { expense ->
                deleteExpense(expense)
            }
        )
        rvExpenses.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = expenseAdapter
        }
    }

    private fun setupDateRangePicker() {
        btnDateRange.setOnClickListener {
            val datePicker = MaterialDatePicker.Builder.dateRangePicker()
                .setTitleText("Select Date Range")
                .build()

            datePicker.addOnPositiveButtonClickListener { selection ->
                val startDate = Date(selection.first)
                val endDate = Date(selection.second)
                currentStartDate = startDate
                currentEndDate = endDate

                val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                tvDateRange.text = "${dateFormat.format(startDate)} - ${dateFormat.format(endDate)}"
                loadExpenses(startDate, endDate)
            }

            datePicker.show(parentFragmentManager, "date_range_picker")
        }

        // Set default to current month
        val calendar = Calendar.getInstance()
        currentStartDate = getStartOfMonth(calendar.time)
        currentEndDate = getEndOfMonth(calendar.time)
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        tvDateRange.text = "This Month (${dateFormat.format(currentStartDate)} - ${dateFormat.format(currentEndDate)})"
    }

    private fun loadCategories() {
        lifecycleScope.launch {
            repository.getAllCategories().collect { categoryList ->
                categories = categoryList
            }
        }
    }

    private fun loadExpenses(startDate: Date = currentStartDate ?: Date(), endDate: Date = currentEndDate ?: Date()) {
        lifecycleScope.launch {
            val userId = sharedPrefs.getLong("current_user_id", -1)
            if (userId == -1L) return@launch

            repository.getExpensesByDateRange(userId, startDate, endDate).collect { expenses ->
                expenseAdapter.submitList(expenses)

                // Calculate total
                val total = expenses.sumOf { it.amount }
                tvTotalExpenses.text = "Total: R${String.format("%.2f", total)}"

                if (expenses.isEmpty()) {
                    tvEmptyState.visibility = View.VISIBLE
                    rvExpenses.visibility = View.GONE
                } else {
                    tvEmptyState.visibility = View.GONE
                    rvExpenses.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun showAddExpenseDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_expense, null)

        val spinnerCategory = dialogView.findViewById<Spinner>(R.id.spinnerCategory)
        val etDescription = dialogView.findViewById<TextInputEditText>(R.id.etDescription)
        val etAmount = dialogView.findViewById<TextInputEditText>(R.id.etAmount)
        val etDate = dialogView.findViewById<TextInputEditText>(R.id.etDate)
        val etStartTime = dialogView.findViewById<TextInputEditText>(R.id.etStartTime)
        val etEndTime = dialogView.findViewById<TextInputEditText>(R.id.etEndTime)
        val btnTakePhoto = dialogView.findViewById<MaterialButton>(R.id.btnTakePhoto)
        val tvPhotoStatus = dialogView.findViewById<TextView>(R.id.tvPhotoStatus)

        // Setup category spinner
        val categoryNames = categories.map { it.name }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categoryNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategory.adapter = adapter

        // Setup date picker
        etDate.setOnClickListener {
            showDatePicker(etDate)
        }

        // Setup time pickers
        etStartTime.setOnClickListener {
            showTimePicker(etStartTime)
        }

        etEndTime.setOnClickListener {
            showTimePicker(etEndTime)
        }

        // Set default date to today
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        etDate.setText(dateFormat.format(Date()))

        // Photo capture
        btnTakePhoto.setOnClickListener {
            dispatchTakePictureIntent()
            tvPhotoStatus.text = "Photo captured!"
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Add Expense")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                if (spinnerCategory.selectedItemPosition >= 0 && categories.isNotEmpty()) {
                    val category = categories[spinnerCategory.selectedItemPosition]
                    val description = etDescription.text.toString()
                    val amount = etAmount.text.toString().toDoubleOrNull() ?: 0.0
                    val date = try {
                        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(etDate.text.toString()) ?: Date()
                    } catch (e: Exception) {
                        Date()
                    }
                    val startTime = etStartTime.text.toString()
                    val endTime = etEndTime.text.toString()

                    if (description.isEmpty() || amount <= 0) {
                        Toast.makeText(requireContext(), "Please fill required fields", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }

                    lifecycleScope.launch {
                        val userId = sharedPrefs.getLong("current_user_id", -1)
                        if (userId != -1L) {
                            val expense = Expense(
                                userId = userId,
                                categoryId = category.id,
                                description = description,
                                amount = amount,
                                date = date,
                                startTime = startTime,
                                endTime = endTime,
                                photoPath = currentPhotoPath
                            )
                            repository.addExpense(expense)
                            Toast.makeText(requireContext(), "Expense added", Toast.LENGTH_SHORT).show()
                            loadExpenses()
                            currentPhotoPath = null
                        }
                    }
                } else {
                    Toast.makeText(requireContext(), "No categories available. Please add categories first.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showExpenseDetail(expense: Expense) {
        val category = categories.find { it.id == expense.categoryId }
        val message = buildString {
            append("Description: ${expense.description}\n")
            append("Amount: R${String.format("%.2f", expense.amount)}\n")
            append("Category: ${category?.name ?: "Unknown"}\n")
            append("Date: ${SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(expense.date)}\n")
            append("Time: ${expense.startTime} - ${expense.endTime}\n")
            append(if (expense.photoPath != null) "📷 Has photo attached" else "No photo")
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Expense Details")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .setNeutralButton("View Photo") { _, _ ->
                expense.photoPath?.let { viewPhoto(it) }
            }
            .show()
    }

    private fun deleteExpense(expense: Expense) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Expense")
            .setMessage("Are you sure you want to delete this expense?")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    repository.deleteExpense(expense)
                    Toast.makeText(requireContext(), "Expense deleted", Toast.LENGTH_SHORT).show()
                    loadExpenses()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun dispatchTakePictureIntent() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(requireContext().packageManager) != null) {
            val photoFile = createImageFile()
            photoFile?.also {
                val photoURI = FileProvider.getUriForFile(
                    requireContext(),
                    "${requireContext().packageName}.fileprovider",
                    it
                )
                currentPhotoPath = it.absolutePath
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
            }
        } else {
            Toast.makeText(requireContext(), "Camera not available", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createImageFile(): File? {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return try {
            File.createTempFile(
                "JPEG_${timeStamp}_",
                ".jpg",
                storageDir
            ).apply {
                currentPhotoPath = absolutePath
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun viewPhoto(photoPath: String) {
        val file = File(photoPath)
        if (file.exists()) {
            val bitmap = BitmapFactory.decodeFile(photoPath)
            val imageView = ImageView(requireContext()).apply {
                setImageBitmap(bitmap)
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Expense Photo")
                .setView(imageView)
                .setPositiveButton("Close", null)
                .show()
        } else {
            Toast.makeText(requireContext(), "Photo not found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showTimePicker(editText: TextInputEditText) {
        val calendar = Calendar.getInstance()
        val timePicker = TimePickerDialog(
            requireContext(),
            { _, hour, minute ->
                editText.setText(String.format("%02d:%02d", hour, minute))
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        )
        timePicker.show()
    }

    private fun showDatePicker(editText: TextInputEditText) {
        val calendar = Calendar.getInstance()
        val datePicker = DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                editText.setText(String.format("%04d-%02d-%02d", year, month + 1, day))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePicker.show()
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

    // Expense Adapter Inner Class
    inner class ExpenseAdapter(
        private val onItemClick: (Expense) -> Unit,
        private val onDeleteClick: (Expense) -> Unit
    ) : RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder>() {

        private var expenses = listOf<Expense>()

        fun submitList(list: List<Expense>) {
            expenses = list
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_expense, parent, false)
            return ExpenseViewHolder(view)
        }

        override fun onBindViewHolder(holder: ExpenseViewHolder, position: Int) {
            holder.bind(expenses[position])
        }

        override fun getItemCount() = expenses.size

        inner class ExpenseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val tvDescription: TextView = itemView.findViewById(R.id.tvDescription)
            private val tvAmount: TextView = itemView.findViewById(R.id.tvAmount)
            private val tvCategory: TextView = itemView.findViewById(R.id.tvCategory)
            private val tvDate: TextView = itemView.findViewById(R.id.tvDate)
            private val ivPhotoIcon: ImageView = itemView.findViewById(R.id.ivPhotoIcon)
            private val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)

            fun bind(expense: Expense) {
                val category = categories.find { it.id == expense.categoryId }
                tvDescription.text = expense.description
                tvAmount.text = "R${String.format("%.2f", expense.amount)}"
                tvCategory.text = category?.name ?: "Unknown"
                tvCategory.setBackgroundColor(category?.color ?: 0xFF4CAF50.toInt())
                tvDate.text = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(expense.date)

                ivPhotoIcon.visibility = if (expense.photoPath != null) View.VISIBLE else View.GONE

                itemView.setOnClickListener { onItemClick(expense) }
                btnDelete.setOnClickListener { onDeleteClick(expense) }
            }
        }
    }
}