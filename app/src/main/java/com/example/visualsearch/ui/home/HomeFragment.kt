package com.example.visualsearch.ui.home

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.example.visualsearch.CameraActivity
import com.example.visualsearch.R
import com.example.visualsearch.databinding.FragmentHomeBinding
import com.example.visualsearch.model.FilterOptions
import com.example.visualsearch.model.MarketplaceType
import com.example.visualsearch.util.MarketplaceUrlBuilder
import com.example.visualsearch.remote.gemini.GeminiApiClient
import com.example.visualsearch.remote.gemini.SearchQuery
import com.example.visualsearch.ui.dialog.FilterDialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.visualsearch.ui.adapter.MarketplaceAdapter
import com.example.visualsearch.util.MarketplaceAppChecker
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import java.io.IOException
import com.example.visualsearch.ui.history.ScanHistoryViewModel
import androidx.navigation.fragment.navArgs
import androidx.room.util.query

class HomeFragment : Fragment() {
    private lateinit var historyViewModel: ScanHistoryViewModel
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val args: HomeFragmentArgs by navArgs()

    private lateinit var geminiApiClient: GeminiApiClient
    private var isProcessing = false
    private var currentSearchQuery: SearchQuery? = null

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val imageUri = result.data?.data
            if (imageUri != null) {
                try {
                    val bitmap = MediaStore.Images.Media.getBitmap(requireActivity().contentResolver, imageUri)
                    displayImage(bitmap)
                    processImage(bitmap)
                } catch (e: IOException) {
                    Toast.makeText(requireContext(), "Image loading error", Toast.LENGTH_SHORT).show()
                    Log.e(TAG, "Error loading image from gallery", e)
                }
            }
        }
    }

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val imagePath = result.data?.getStringExtra("image_path")
            if (imagePath != null) {
                try {
                    val bitmap = BitmapFactory.decodeFile(imagePath)
                    displayImage(bitmap)
                    processImage(bitmap)
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Image loading error: ${e.message}", Toast.LENGTH_SHORT).show()
                    Log.e(TAG, "Error processing camera result", e)
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
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        historyViewModel = ViewModelProvider(this).get(ScanHistoryViewModel::class.java)
        geminiApiClient = GeminiApiClient(getString(R.string.gemini_api_key))
        setupRecyclerView()
        setupButtons()
        setupButtonAnimations()
        binding.btnCloseResults.setOnClickListener {
            binding.resultsContainer.visibility = View.GONE
        }
        
        // Process arguments if they exist (when coming from scan detail)
        handleNavigationArguments()
    }
    
    private fun handleNavigationArguments() {
        // Check if we have navigation arguments (from ScanDetailFragment)
        if (args.query != null && args.imagePath != null) {
            try {
                // Create SearchQuery object from arguments
                val searchQuery = SearchQuery(
                    query = args.query ?: "",
                    productType = args.productType ?: "",
                    brand = args.brand ?: "",
                    modelName = args.model ?: "",
                    color = args.color ?: ""
                )
                
                // Load and display the image
                val bitmap = BitmapFactory.decodeFile(args.imagePath)
                displayImage(bitmap)
                
                // Display the search results directly
                displayResult(searchQuery)
                
                // Update currentSearchQuery for marketplace searches
                currentSearchQuery = searchQuery
                
                Log.d(TAG, "Successfully processed navigation arguments")
            } catch (e: Exception) {
                Log.e(TAG, "Error processing navigation arguments", e)
                Toast.makeText(requireContext(), "Error loading previous scan: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupRecyclerView() {
        binding.recyclerViewMarketplaces.layoutManager = LinearLayoutManager(context)
    }

    private fun setupButtons() {
        binding.btnSelectImage.setOnClickListener {
            checkPermissionAndPickImage()
        }

        binding.btnScanWithCamera.setOnClickListener {
            openCameraScanner()
        }

        binding.btnFilter.setOnClickListener {
            showFilterDialog()
        }

        binding.btnFindSimilar.setOnClickListener {
            findSimilarProducts()
        }

        binding.btnShare.setOnClickListener {
            shareResults()
        }
    }

    private fun setupButtonAnimations() {
        val pulseAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.pulse_scale)

        binding.btnSelectImage.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                v.startAnimation(pulseAnimation)
            }
            false
        }

        binding.btnScanWithCamera.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                v.startAnimation(pulseAnimation)
            }
            false
        }
    }

    private fun checkPermissionAndPickImage() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        Dexter.withContext(requireContext())
            .withPermission(permission)
            .withListener(object : PermissionListener {
                override fun onPermissionGranted(response: PermissionGrantedResponse) {
                    openImagePicker()
                }

                override fun onPermissionDenied(response: PermissionDeniedResponse) {
                    Toast.makeText(requireContext(), "Permission required to select image", Toast.LENGTH_SHORT).show()
                }

                override fun onPermissionRationaleShouldBeShown(permission: PermissionRequest, token: PermissionToken) {
                    token.continuePermissionRequest()
                }
            }).check()
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        imagePickerLauncher.launch(intent)
    }

    private fun openCameraScanner() {
        val intent = Intent(requireContext(), CameraActivity::class.java)
        cameraLauncher.launch(intent)
    }

    private fun displayImage(bitmap: Bitmap) {
        binding.tvPlaceholder.visibility = View.GONE
        Glide.with(this).load(bitmap).into(binding.imageView)
    }

    private fun processImage(bitmap: Bitmap) {
        if (isProcessing) return

        isProcessing = true
        showLoading(true)
        hideResult()

        geminiApiClient.analyzeImage(bitmap, object : GeminiApiClient.GeminiApiListener {
            override fun onSuccess(searchQuery: SearchQuery) {
                activity?.runOnUiThread {
                    isProcessing = false
                    showLoading(false)
                    displayResult(searchQuery)
                    historyViewModel.saveScanWithBitmap(searchQuery, bitmap)
                }
            }

            override fun onError(e: Exception) {
                activity?.runOnUiThread {
                    isProcessing = false
                    showLoading(false)
                    Toast.makeText(requireContext(), "Image analysis error: ${e.message}", Toast.LENGTH_SHORT).show()
                    Log.e(TAG, "Image analysis error", e)
                }
            }
        })
    }

    private fun showLoading(isLoading: Boolean) {
        val fadeAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.fade_in_out)

        if (isLoading) {
            binding.progressBar.startAnimation(fadeAnimation)
            binding.tvLoading.startAnimation(fadeAnimation)
            binding.tvPlaceholder.visibility = View.GONE
        }

        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.tvLoading.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnSelectImage.isEnabled = !isLoading
        binding.btnScanWithCamera.isEnabled = !isLoading
    }

    private fun hideResult() {
        binding.resultsContainer.visibility = View.GONE
    }

    private fun displayResult(searchQuery: SearchQuery) {
        val resultBuilder = StringBuilder()

        resultBuilder.append("Search query: ").append(searchQuery.query).append("\n\n")

        if (searchQuery.productType.isNotEmpty()) {
            resultBuilder.append("Product type: ").append(searchQuery.productType).append("\n\n")
        }

        if (searchQuery.brand.isNotEmpty()) {
            resultBuilder.append("Brand: ").append(searchQuery.brand).append("\n\n")
        }

        if (searchQuery.modelName.isNotEmpty()) {
            resultBuilder.append("Model: ").append(searchQuery.modelName).append("\n\n")
        }

        if (searchQuery.color.isNotEmpty()) {
            resultBuilder.append("Color: ").append(searchQuery.color)
        }

        binding.resultsContainer.visibility = View.VISIBLE
        binding.tvGarbageType.text = "Product analysis:"
        binding.tvInstructions.text = resultBuilder.toString().trim()
        binding.tvEstimatedCost.visibility = View.VISIBLE
        binding.tvEstimatedCost.text = "Generated search query: \"${searchQuery.query}\""
        currentSearchQuery = searchQuery.copy()
        showActionButtonsAndMarketplaces(searchQuery)
        val slideUpAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.slide_up)
        binding.resultsContainer.startAnimation(slideUpAnimation)

        val toastMessage = StringBuilder("Product: ${searchQuery.productType}")
        if (searchQuery.brand.isNotEmpty()) {
            toastMessage.append(", Brand: ${searchQuery.brand}")
        }
        Toast.makeText(requireContext(), toastMessage.toString(), Toast.LENGTH_LONG).show()
    }

    private fun setupMarketplaceButtons(query: String) {}

    private fun showFilterDialog(marketplaceType: MarketplaceType, query: String) {
        val dialogFragment = FilterDialogFragment.newInstance(marketplaceType, currentSearchQuery?.brand)
        dialogFragment.setFilterDialogListener(object : FilterDialogFragment.FilterDialogListener {
            override fun onFilterOptionsSelected(marketplaceType: MarketplaceType, filterOptions: FilterOptions, applyToAll: Boolean) {
                if (applyToAll) {
                    Toast.makeText(requireContext(), getString(R.string.filters_applied_all), Toast.LENGTH_SHORT).show()
                    val adapter = (binding.recyclerViewMarketplaces.adapter as? MarketplaceAdapter)
                    adapter?.updateFilters(filterOptions)
                } else {
                    val url = MarketplaceUrlBuilder.buildSearchUrl(marketplaceType, query, filterOptions)
                    openMarketplaceSearch(url)
                }
            }
        })
        dialogFragment.show(parentFragmentManager, "FilterDialog")
    }

    private fun openMarketplaceSearch(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Failed to open browser: ${e.message}", Toast.LENGTH_SHORT).show()
            Log.e(TAG, "URL opening error", e)
        }
    }

    private fun showActionButtonsAndMarketplaces(searchQuery: SearchQuery) {
        setupMarketplacesList(searchQuery)
    }

    private fun setupMarketplacesList(searchQuery: SearchQuery) {
        val adapter = MarketplaceAdapter(requireContext(), searchQuery) { marketplaceType ->
            openMarketplace(marketplaceType, searchQuery)
        }
        binding.recyclerViewMarketplaces.adapter = adapter
    }

    private fun openMarketplace(marketplaceType: MarketplaceType, searchQuery: SearchQuery) {
        val marketplaceAppChecker = MarketplaceAppChecker(requireContext())
        val filterOptions = FilterOptions()

        try {
            val intent = marketplaceAppChecker.getMarketplaceIntent(
                marketplaceType,
                searchQuery.query,
                filterOptions
            )
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(
                requireContext(),
                "Failed to open marketplace: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
            Log.e(TAG, "Marketplace opening error", e)
        }
    }

    private fun showFilterDialog() {
        val dialogFragment = FilterDialogFragment.newInstance(MarketplaceType.WILDBERRIES, currentSearchQuery?.brand)
        dialogFragment.setFilterDialogListener(object : FilterDialogFragment.FilterDialogListener {
            override fun onFilterOptionsSelected(marketplaceType: MarketplaceType, filterOptions: FilterOptions, applyToAll: Boolean) {
                val adapter = (binding.recyclerViewMarketplaces.adapter as? MarketplaceAdapter)
                adapter?.updateFilters(filterOptions)
                Toast.makeText(requireContext(), getString(R.string.filters_applied), Toast.LENGTH_SHORT).show()
            }
        })
        dialogFragment.show(parentFragmentManager, "FilterDialog")
    }

    private fun findSimilarProducts() {
        Toast.makeText(
            requireContext(),
            "Searching for similar products",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun shareResults() {
        try {
            val searchQuery = currentSearchQuery ?: return
            val shareText = "I found this product using the Visual Search app: ${searchQuery.query}\n\n" +
                    "Product type: ${searchQuery.productType}\n" +
                    "Brand: ${searchQuery.brand}\n" +
                    "Model: ${searchQuery.modelName}"

            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, shareText)
                type = "text/plain"
            }

            startActivity(Intent.createChooser(shareIntent, "Share search results"))
        } catch (e: Exception) {
            Toast.makeText(
                requireContext(),
                "Failed to share: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
            Log.e(TAG, "Sharing error", e)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "HomeFragment"
    }
}