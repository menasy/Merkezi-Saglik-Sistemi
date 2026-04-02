package com.menasy.merkezisagliksistemi.ui.doctor.examination

import android.os.Bundle
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.menasy.merkezisagliksistemi.R
import com.menasy.merkezisagliksistemi.data.model.Medicine
import com.menasy.merkezisagliksistemi.databinding.DialogMedicineSelectionBinding

class MedicineSelectionBottomSheet : BottomSheetDialogFragment() {

    private var _binding: DialogMedicineSelectionBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: MedicineSelectionAdapter
    private var allMedicines: List<Medicine> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        allMedicines = readMedicinesFromArgs()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogMedicineSelectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupBottomSheetBehavior()
        setupAdapter()
        setupSearch()
        setupButtons()
        loadInitialList()
    }

    private fun setupBottomSheetBehavior() {
        (dialog as? BottomSheetDialog)?.behavior?.apply {
            state = BottomSheetBehavior.STATE_EXPANDED
            skipCollapsed = true
            isDraggable = true
        }
    }

    private fun setupAdapter() {
        adapter = MedicineSelectionAdapter { selectedIds ->
            updateSelectionInfo(selectedIds.size)
            binding.btnAddMedicines.isEnabled = selectedIds.isNotEmpty()
        }
        binding.rvMedicines.adapter = adapter
    }

    private fun setupSearch() {
        binding.etSearch.doAfterTextChanged { text ->
            filterList(text?.toString().orEmpty())
        }
    }

    private fun setupButtons() {
        binding.btnClose.setOnClickListener {
            dismiss()
        }

        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        binding.btnAddMedicines.setOnClickListener {
            val selectedIds = adapter.getSelectedIds()
            if (selectedIds.isNotEmpty()) {
                publishSelectionResult(selectedIds.toList())
            }
            dismiss()
        }
    }

    private fun loadInitialList() {
        updateListState(allMedicines, isSearchResult = false)
    }

    private fun filterList(query: String) {
        val normalizedQuery = query.trim()
        val filteredList = if (normalizedQuery.isEmpty()) {
            allMedicines
        } else {
            allMedicines.filter { medicine ->
                medicine.medicineName.contains(normalizedQuery, ignoreCase = true) ||
                    medicine.dosage.contains(normalizedQuery, ignoreCase = true) ||
                    medicine.frequency.contains(normalizedQuery, ignoreCase = true) ||
                    medicine.usageDescription.contains(normalizedQuery, ignoreCase = true)
            }
        }
        updateListState(filteredList, isSearchResult = normalizedQuery.isNotEmpty())
    }

    private fun updateListState(medicines: List<Medicine>, isSearchResult: Boolean) {
        adapter.submitList(medicines)

        when {
            medicines.isNotEmpty() -> {
                binding.rvMedicines.isVisible = true
                binding.layoutEmpty.isVisible = false
            }
            isSearchResult -> {
                binding.rvMedicines.isVisible = false
                binding.layoutEmpty.isVisible = true
                binding.tvEmptyMessage.text = getString(R.string.doctor_examination_no_medicine_found)
            }
            allMedicines.isEmpty() -> {
                binding.rvMedicines.isVisible = false
                binding.layoutEmpty.isVisible = true
                binding.tvEmptyMessage.text = getString(R.string.doctor_examination_all_medicines_added)
            }
            else -> {
                binding.rvMedicines.isVisible = false
                binding.layoutEmpty.isVisible = true
                binding.tvEmptyMessage.text = getString(R.string.doctor_examination_no_medicine_found)
            }
        }
    }

    private fun updateSelectionInfo(count: Int) {
        if (count > 0) {
            binding.tvSelectionInfo.text = getString(
                R.string.doctor_examination_selected_count_format,
                count
            )
            binding.tvSelectionInfo.isVisible = true
        } else {
            binding.tvSelectionInfo.isVisible = false
        }
    }

    private fun publishSelectionResult(selectedIds: List<String>) {
        parentFragmentManager.setFragmentResult(
            RESULT_REQUEST_KEY,
            Bundle().apply {
                putStringArrayList(RESULT_SELECTED_MEDICINE_IDS, ArrayList(selectedIds))
            }
        )
    }

    private fun readMedicinesFromArgs(): List<Medicine> {
        val args = arguments ?: return emptyList()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            args.getParcelableArrayList(ARG_MEDICINES, Medicine::class.java).orEmpty()
        } else {
            @Suppress("DEPRECATION")
            args.getParcelableArrayList<Medicine>(ARG_MEDICINES).orEmpty()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "MedicineSelectionBottomSheet"
        const val RESULT_REQUEST_KEY = "medicine_selection_result_request"
        const val RESULT_SELECTED_MEDICINE_IDS = "result_selected_medicine_ids"
        private const val ARG_MEDICINES = "arg_medicines"

        fun newInstance(medicines: List<Medicine>): MedicineSelectionBottomSheet {
            return MedicineSelectionBottomSheet().apply {
                arguments = Bundle().apply {
                    putParcelableArrayList(ARG_MEDICINES, ArrayList(medicines))
                }
            }
        }
    }
}
