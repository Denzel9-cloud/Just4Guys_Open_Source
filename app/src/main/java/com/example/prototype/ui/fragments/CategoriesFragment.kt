package com.example.prototype.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.prototype.R
import com.example.prototype.data.PaymentPlannerRepository
import com.example.prototype.data.entity.Category
import com.example.prototype.databinding.FragmentCategoriesBinding
import com.example.prototype.databinding.ItemCategoryBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class CategoriesFragment : Fragment() {
    private var _binding: FragmentCategoriesBinding? = null
    private val binding get() = _binding!!
    private lateinit var repository: PaymentPlannerRepository
    private lateinit var categoryAdapter: CategoryAdapter
    private var categories = listOf<Category>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCategoriesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repository = PaymentPlannerRepository(requireContext())

        setupRecyclerView()
        loadCategories()

        binding.fabAddCategory.setOnClickListener {
            showAddCategoryDialog()
        }
    }

    private fun setupRecyclerView() {
        categoryAdapter = CategoryAdapter(
            onEditClick = { category ->
                showEditCategoryDialog(category)
            },
            onDeleteClick = { category ->
                deleteCategory(category)
            }
        )
        binding.rvCategories.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = categoryAdapter
        }
    }

    private fun loadCategories() {
        lifecycleScope.launch {
            repository.getAllCategories().collect { categoryList ->
                categories = categoryList
                categoryAdapter.submitList(categoryList)

                if (categoryList.isEmpty()) {
                    binding.tvEmptyState.visibility = View.VISIBLE
                    binding.rvCategories.visibility = View.GONE
                } else {
                    binding.tvEmptyState.visibility = View.GONE
                    binding.rvCategories.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun showAddCategoryDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_category, null)
        val etName = dialogView.findViewById<TextInputEditText>(R.id.etCategoryName)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Add Category")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val name = etName.text.toString().trim()
                if (name.isNotEmpty()) {
                    lifecycleScope.launch {
                        repository.addCategory(Category(name = name))
                        Toast.makeText(requireContext(), "Category added", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEditCategoryDialog(category: Category) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_category, null)
        val etName = dialogView.findViewById<TextInputEditText>(R.id.etCategoryName)
        etName.setText(category.name)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Edit Category")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val newName = etName.text.toString().trim()
                if (newName.isNotEmpty()) {
                    lifecycleScope.launch {
                        repository.updateCategory(category.copy(name = newName))
                        Toast.makeText(requireContext(), "Category updated", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteCategory(category: Category) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Category")
            .setMessage("Delete '${category.name}'? All expenses in this category will also be deleted.")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    repository.deleteCategory(category)
                    Toast.makeText(requireContext(), "Category deleted", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // Category Adapter
    inner class CategoryAdapter(
        private val onEditClick: (Category) -> Unit,
        private val onDeleteClick: (Category) -> Unit
    ) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

        private var categories = listOf<Category>()

        fun submitList(list: List<Category>) {
            categories = list
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
            val itemBinding = ItemCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return CategoryViewHolder(itemBinding)
        }

        override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
            holder.bind(categories[position])
        }

        override fun getItemCount() = categories.size

        inner class CategoryViewHolder(private val itemBinding: ItemCategoryBinding) : RecyclerView.ViewHolder(itemBinding.root) {
            fun bind(category: Category) {
                itemBinding.tvCategoryName.text = category.name
                itemBinding.viewColor.setBackgroundColor(category.color)

                itemBinding.btnEdit.setOnClickListener { onEditClick(category) }
                itemBinding.btnDelete.setOnClickListener { onDeleteClick(category) }
            }
        }
    }
}