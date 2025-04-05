package com.example.visualsearch.ui.history

import android.os.Bundle
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
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.visualsearch.R
import com.example.visualsearch.databinding.FragmentScanHistoryBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class ScanHistoryFragment : Fragment(), MenuProvider {

    private var _binding: FragmentScanHistoryBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ScanHistoryViewModel
    private lateinit var adapter: ScanHistoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScanHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this).get(ScanHistoryViewModel::class.java)

        setupRecyclerView()
        setupActionMenu()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        adapter = ScanHistoryAdapter(requireContext()) { scanHistory ->
            // Navigate to scan detail fragment
            val action = ScanDetailFragmentArgs(scanHistory.id).toNavDirections()
            findNavController().navigate(action)
        }

        binding.recyclerViewHistory.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@ScanHistoryFragment.adapter
        }
    }

    private fun setupActionMenu() {
        requireActivity().addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun observeViewModel() {
        viewModel.allScans.observe(viewLifecycleOwner) { scans ->
            adapter.submitList(scans)

            // Show empty view if no scans
            if (scans.isEmpty()) {
                binding.textViewEmptyHistory.visibility = View.VISIBLE
                binding.recyclerViewHistory.visibility = View.GONE
            } else {
                binding.textViewEmptyHistory.visibility = View.GONE
                binding.recyclerViewHistory.visibility = View.VISIBLE
            }
        }
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.scan_history_menu, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.action_clear_history -> {
                showClearHistoryDialog()
                true
            }
            else -> false
        }
    }

    private fun showClearHistoryDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.clear_history)
            .setMessage(R.string.clear_history_confirmation)
            .setPositiveButton(R.string.clear) { _, _ ->
                viewModel.clearHistory()
                Toast.makeText(requireContext(), R.string.history_cleared, Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "ScanHistoryFragment"
    }
}