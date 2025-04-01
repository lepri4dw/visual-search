package com.example.visualsearch.ui.home

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.visualsearch.CameraActivity
import com.example.visualsearch.client.VisionApiClient
import com.example.visualsearch.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var visionApiClient: VisionApiClient

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imagePath = result.data?.getStringExtra("image_path")
            imagePath?.let { path ->
                try {
                    val bitmap = BitmapFactory.decodeFile(path)
                    visionApiClient.analyzeImage(bitmap, object : VisionApiClient.VisionApiListener {
                        override fun onSuccess(labels: List<String>) {
                            activity?.runOnUiThread {
                                // Display labels or whatever you want with the result
                                Toast.makeText(
                                    requireContext(),
                                    "Analysis complete: ${labels.joinToString(", ")}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }

                        override fun onError(e: Exception) {
                            activity?.runOnUiThread {
                                Toast.makeText(
                                    requireContext(),
                                    "Error analyzing image: ${e.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    })
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing camera result", e)
                    Toast.makeText(
                        requireContext(),
                        "Error processing image: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Initialize VisionApiClient
        visionApiClient = VisionApiClient(requireContext())

        // Set up camera button
        binding.cameraButton.setOnClickListener {
            openCamera()
        }

        homeViewModel.text.observe(viewLifecycleOwner) {
            binding.textHome.text = it
        }

        return root
    }

    private fun openCamera() {
        val intent = Intent(requireContext(), CameraActivity::class.java)
        cameraLauncher.launch(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "HomeFragment"
    }
}