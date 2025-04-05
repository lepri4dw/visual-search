package com.example.visualsearch.ui.history

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.example.visualsearch.R
import com.example.visualsearch.databinding.FragmentScanDetailBinding
import com.example.visualsearch.remote.gemini.SearchQuery
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ScanDetailFragment : Fragment(), MenuProvider {

    private var _binding: FragmentScanDetailBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ScanHistoryViewModel
    private val args: ScanDetailFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScanDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this).get(ScanHistoryViewModel::class.java)
        viewModel.getScanById(args.scanId)

        requireActivity().addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)

        setupButtons()

        viewModel.selectedScan.observe(viewLifecycleOwner) { scan ->
            if (scan != null) {
                val imageFile = File(scan.imagePath)
                if (imageFile.exists()) {
                    Glide.with(this).load(imageFile).into(binding.imageViewDetail)
                }

                binding.textViewDetailProductType.text = scan.productType
                binding.textViewDetailQuery.text = scan.query
                binding.textViewDetailBrand.text = scan.brand
                binding.textViewDetailModel.text = scan.modelName
                binding.textViewDetailColor.text = scan.color

                val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                val formattedDate = scan.scanDate?.let { dateFormat.format(it) } ?: "N/A"
                binding.textViewDetailDate.text = formattedDate
            }
        }
    }

    private fun setupButtons() {
        binding.buttonRescan.setOnClickListener {
            navigateToHomeWithData()
        }

        binding.buttonDeleteScan.setOnClickListener {
            showDeleteConfirmationDialog()
        }
    }

    private fun navigateToHomeWithData() {
        val scan = viewModel.selectedScan.value ?: return

        findNavController().navigate(
            ScanDetailFragmentDirections.actionScanDetailToNavigationHome(
                query = scan.query,
                productType = scan.productType,
                brand = scan.brand,
                model = scan.modelName,
                color = scan.color,
                imagePath = scan.imagePath
            )
        )
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.scan_detail_menu, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.action_delete_scan -> {
                showDeleteConfirmationDialog()
                true
            }
            else -> false
        }
    }

    private fun showDeleteConfirmationDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete_scan)
            .setMessage(R.string.delete_scan_confirmation)
            .setPositiveButton(R.string.delete) { _, _ ->
                viewModel.selectedScan.value?.let { scan ->
                    viewModel.deleteScan(scan)
                    findNavController().navigateUp()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "ScanDetailFragment"
    }
}