package com.example.visualsearch.client

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.api.gax.core.FixedCredentialsProvider
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.vision.v1.*
import com.google.protobuf.ByteString
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class VisionApiClient(private val context: Context) {
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()

    interface VisionApiListener {
        fun onSuccess(labels: List<String>)
        fun onError(e: Exception)
    }

    fun analyzeImage(bitmap: Bitmap, listener: VisionApiListener) {
        executor.execute {
            try {
                // Load credentials
                val credentialsStream = context.assets.open("garbagescaner-454017-827fa0fdc541.json")
                val credentials = GoogleCredentials.fromStream(credentialsStream)
                credentialsStream.close()

                // Set up the Vision API client
                val settings = ImageAnnotatorSettings.newBuilder()
                    .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
                    .build()

                // Create the image
                val stream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
                val imageBytes = ByteString.copyFrom(stream.toByteArray())
                val image = Image.newBuilder().setContent(imageBytes).build()

                // Set up the request with multiple features
                val labelDetection = Feature.newBuilder()
                    .setType(Feature.Type.LABEL_DETECTION)
                    .setMaxResults(10)
                    .build()

                val textDetection = Feature.newBuilder()
                    .setType(Feature.Type.TEXT_DETECTION)
                    .setMaxResults(10)
                    .build()

                val logoDetection = Feature.newBuilder()
                    .setType(Feature.Type.LOGO_DETECTION)
                    .setMaxResults(10)
                    .build()

                val imageProperties = Feature.newBuilder()
                    .setType(Feature.Type.IMAGE_PROPERTIES)
                    .setMaxResults(10)
                    .build()

                val request = AnnotateImageRequest.newBuilder()
                    .addFeatures(labelDetection)
                    .addFeatures(textDetection)
                    .addFeatures(logoDetection)
                    .addFeatures(imageProperties)
                    .setImage(image)
                    .build()

                // Process the request
                ImageAnnotatorClient.create(settings).use { client ->
                    val response = client.batchAnnotateImages(listOf(request))
                    val imageResponse = response.getResponsesList()[0]

                    // Log the formatted output
                    Log.d(TAG, formatLogOutput(imageResponse))

                    // Still extract labels for backward compatibility
                    val labels = imageResponse.labelAnnotationsList.map { it.description }
                    listener.onSuccess(labels)
                }
            } catch (e: IOException) {
                Log.e(TAG, "Error analyzing image", e)
                listener.onError(e)
            }
        }
    }

    private fun formatLogOutput(imageResponse: AnnotateImageResponse): String {
        val stringBuilder = StringBuilder()

        // Logo
        if (imageResponse.logoAnnotationsList.isNotEmpty()) {
            val logo = imageResponse.logoAnnotationsList[0]
            stringBuilder.append("Logo: ${logo.description} (Confidence: ${"%.3f".format(logo.score)}); ")
        }

        // Labels
        stringBuilder.append("Labels: ")
        imageResponse.labelAnnotationsList.forEach { label ->
            stringBuilder.append("${label.description} (Confidence: ${"%.3f".format(label.score)}); ")
        }

        // Text
        stringBuilder.append("Text: ")
        val textList = imageResponse.textAnnotationsList.map {
            it.description.replace("\n", " ") // Заменяем переносы строк на пробелы
        }
        stringBuilder.append(textList.joinToString(", "))

        // Colors
        if (imageResponse.imagePropertiesAnnotation?.dominantColors?.colorsList?.isNotEmpty() == true) {
            stringBuilder.append("; Colors: ")
            imageResponse.imagePropertiesAnnotation.dominantColors.colorsList.take(3).forEach { colorInfo ->
                stringBuilder.append("RGB(${colorInfo.color.red},${colorInfo.color.green},${colorInfo.color.blue}) (Confidence: ${"%.3f".format(colorInfo.score)}); ")
            }
        }

        return stringBuilder.toString().trimEnd(' ', ';')
    }

    companion object {
        private const val TAG = "VisionApiClient"
    }
}