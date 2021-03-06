package com.example.myfridgeapp

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.icu.text.SimpleDateFormat
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.myfridgeapp.databinding.ActivityBarcodeScanBinding
import com.example.myfridgeapp.databinding.ActivityMainBinding

import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.jsoup.Connection
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.parser.Parser
import org.jsoup.select.Elements
import java.lang.Exception
import java.util.*
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class BarcodeScan : AppCompatActivity() {
    lateinit var binding: ActivityBarcodeScanBinding
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    private val scope = CoroutineScope(Dispatchers.IO)
    lateinit var cameraProvider: ProcessCameraProvider
    lateinit var barcodeDetection: ImageAnalysis
    var permissions =
        arrayOf(
            Manifest.permission.CAMERA,
        )

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { it ->
            if (it) {
                Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show()
                getPermission()
            } else {
                Toast.makeText(this, "?????????????????? ?????? > ?????? ??????\n ????????? ????????? ????????????.", Toast.LENGTH_LONG).show()

            }
        }

    private lateinit var intentResult: Intent

    // ?????? ?????? format + permissions
    companion object {
        private const val TAG = "CameraXApp"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS =
            mutableListOf(
                Manifest.permission.CAMERA,
            )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBarcodeScanBinding.inflate(layoutInflater)
        setContentView(binding.root)
        getPermission()
        init()
        cameraExecutor = Executors.newSingleThreadExecutor()

    }

    private fun init() {
        // intentResult ?????????
        intentResult = Intent()

        binding.apply {
            textButton.setOnClickListener {
                // text ???????????? ??????
                intentResult.putExtra("productName", "none")
                setResult(RESULT_CANCELED, intentResult)
                finish()
            }
        }


    }

    // Camera ?????????
    private fun startCamera() {
        // camera ??? lifecycle ??? bind ???
        val cameraProvideFuture = ProcessCameraProvider.getInstance(this)

        cameraProvideFuture.addListener(
            {
                cameraProvider = cameraProvideFuture.get()

                // ????????? + build + surfaceProvider ??? ????????? (surface: ??? ????????? ???????????? view?)
                val preview = Preview.Builder()
                    .build()
                    .also {
                        it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                    }

                // barcode ???????????? ??????
                barcodeDetection = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)   //?
                    .build()
                    .also {
                        it.setAnalyzer(cameraExecutor, ImageAnalyzer())
                    }

                // imageCapture ??????
                imageCapture = ImageCapture.Builder().build()

                // default camera: back camera
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    cameraProvider.unbindAll()

                    // ????????? ????????? ?????? ?????????..? ???????
                    // camera ????????? ?????????.
                    cameraProvider.bindToLifecycle(
                        this, cameraSelector, preview, imageCapture, barcodeDetection
                    )

                } catch (exc: Exception) {
                    // app ??? ????????? focus ??? ????????? ?????? ???..
                    Log.i(TAG, "Binding failed", exc)

                }
            }, ContextCompat.getMainExecutor(this)  // main thread

        )
    }


    // ?????? permission ??? ?????????????????? ??????
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    // camera ????????? ??????
    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }


    // permission request
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                // ?????? ????????? ?????? ?????? ?????? ????????? ?????? ???????????? ???????????? ?????? ????????? ?????????.
                Toast.makeText(this, "Permission not granted", Toast.LENGTH_SHORT).show()
                callPermissionDlg()

            }
        }
    }

    private fun getPermission() {
        when {
            (ContextCompat.checkSelfPermission(
                this, Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED)
            -> {
                startCamera()
            }
            (ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.CAMERA
            ))
            -> {
                callPermissionDlg()

            }
            else
            -> {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    // API ???????????? ?????????
    private fun getXMLData(productID: String): Boolean {
        var pName = ""
        scope.launch {
            lateinit var doc: Document

            // API ??? 1000??? ????????? ??????
            searchloop@ for (i: Int in 0..250000 step (1000)) {
                doc =
                    Jsoup.connect("https://openapi.foodsafetykorea.go.kr/api/cb3605d6af0442069388/C005/xml/${i + 1}/${i + 1000}/BAR_CD=${productID}")
                        .parser(Parser.xmlParser()).get()

                val resultCode: Elements = doc.select("RESULT")
                // ?????? page ??? ???????????? ?????? ???
                if (resultCode.select("CODE").toString() == "INFO-200") {
                    continue
                } else {
                    val productNames = doc.select("PRDLST_NM")
                    for (tmp in productNames) {
                        withContext(Dispatchers.Main) {
                            pName = tmp.text()
//                            binding.textView2.text = pName
                            Log.i("XML Read", pName)

                        }

                    }
                    intentResult.putExtra("productName", pName)
                    setResult(RESULT_OK, intentResult)
                    finish()
                    break@searchloop

                }

            }


        }

        return true

    }

    @SuppressLint("UnsafeOptInUsageError")
    inner class ImageAnalyzer() : ImageAnalysis.Analyzer {
        override fun analyze(imageProxy: ImageProxy) {
            val mediaImage = imageProxy.image
            if (mediaImage != null) {
                // mediaImage ????????? capture ??? image ??? ???????????? ???????
                val image =
                    InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                Log.i("ImageAnalyzer", "Processing..!")

                val options = BarcodeScannerOptions.Builder()
                    .setBarcodeFormats(
                        Barcode.FORMAT_EAN_13,
                    )
                    .build()

                // option ??? ??? ?????? ??????
                val scanner = BarcodeScanning.getClient(options)

                scanner.process(image)
                    .addOnSuccessListener { barcodes ->
                        Log.i("Barcode Scanner", "Detecting..!")

                        for (barcode in barcodes) {
                            val bounds = barcode.boundingBox
                            val corners = barcode.cornerPoints
                            val format = barcode.format //EAN_13

                            val id = barcode.rawValue!!.toString()
                            val value = barcode.valueType   // TYPE_PRODUCT
                            Log.i(
                                "Barcode Scanner",
                                "Success!: id: $id, valueType: $value format: $format"
                            )

//                            binding.textView.text = id
                            if (getXMLData(id)) {
                                cameraProvider.unbind(barcodeDetection)
                            }

                        }
                    }
                    .addOnFailureListener {
                        Log.i("Barcode Scanner", "failed")

                    }
                    .addOnCompleteListener {
                        imageProxy.image?.close()
                        imageProxy.close()

                    }
            }
        }
    }

    private fun callPermissionDlg() {
        val builder = AlertDialog.Builder(this)
        builder.setMessage("??? ????????? ?????? ????????? ???????????????")
            .setTitle("?????? ??????")
            .setPositiveButton("??????") { _, _ ->
                // ?????? ?????? ??????
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
            .setNegativeButton("??????") { dlg, _ ->
                dlg.dismiss()
                // ????????? ?????? ???????????? ?????????

            }
        val dlg = builder.create()
        dlg.show()
    }

}