package com.scanpay.app.ui.scanner

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import com.journeyapps.barcodescanner.*
import com.scanpay.app.data.api.*
import com.scanpay.app.data.request.ScanQRRequest
import com.scanpay.app.databinding.ActivityQrScannerBinding
import com.scanpay.app.ui.payment.PaymentConfirmationActivity
import com.scanpay.app.utils.Constants
import kotlinx.coroutines.*

class QRScannerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityQrScannerBinding
    private var processing = false
    private var torch = false
    private val cameraPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) binding.barcodeView.resume()
        else Toast.makeText(this, "Camera access is needed to scan. You can also upload a QR image.", Toast.LENGTH_LONG).show()
    }
    private val upload = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) lifecycleScope.launch {
            try {
                val text = withContext(Dispatchers.IO) {
                    // Bound decoded image dimensions to avoid loading a full-resolution camera image.
                    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, options) }
                    options.inSampleSize = maxOf(1, maxOf(options.outWidth, options.outHeight) / 1600)
                    options.inJustDecodeBounds = false
                    val bitmap = contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, options) }
                        ?: error("Cannot open image")
                    try {
                        val pixels = IntArray(bitmap.width * bitmap.height)
                        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
                        MultiFormatReader().decode(BinaryBitmap(HybridBinarizer(RGBLuminanceSource(bitmap.width, bitmap.height, pixels))),
                            mapOf(DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE), DecodeHintType.TRY_HARDER to true)).text
                    } finally { bitmap.recycle() }
                }
                validate(text)
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { Toast.makeText(this@QRScannerActivity, "No readable QR code found in this image", Toast.LENGTH_LONG).show() }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQrScannerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.barcodeView.barcodeView.decoderFactory = DefaultDecoderFactory(listOf(BarcodeFormat.QR_CODE))
        binding.barcodeView.setStatusText("")
        binding.barcodeView.decodeContinuous(object : BarcodeCallback {
            override fun barcodeResult(result: BarcodeResult?) { if (!processing && result != null) validate(result.text) }
        })
        binding.btnBack.setOnClickListener { finish() }
        binding.btnFlashlight.setOnClickListener {
            torch = !torch
            if (torch) binding.barcodeView.setTorchOn() else binding.barcodeView.setTorchOff()
        }
        binding.btnUpload.setOnClickListener { upload.launch("image/*") }
        binding.btnManualEntry.setOnClickListener {
            val input = EditText(this).apply { hint = "Paste ScanPay QR contents"; maxLines = 5 }
            MaterialAlertDialogBuilder(this).setTitle("Enter QR contents").setView(input)
                .setPositiveButton("Verify merchant") { _, _ -> validate(input.text.toString().trim()) }
                .setNegativeButton("Cancel", null).show()
        }
        binding.btnHelp.setOnClickListener {
            MaterialAlertDialogBuilder(this).setTitle("Scan & Pay")
                .setMessage("Scan a merchant's ScanPay QR or upload its image. Confirm the merchant and amount, then enter your PIN in the M-Pesa prompt.")
                .setPositiveButton("Got it", null).show()
        }
        binding.btnThemeToggle.setOnClickListener {
            val night = resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK == android.content.res.Configuration.UI_MODE_NIGHT_YES
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(if (night) androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO else androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES)
        }
        if (!hasCamera()) cameraPermission.launch(Manifest.permission.CAMERA)
    }
    private fun hasCamera() = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    private fun validate(payload: String) {
        if (processing) return
        processing = true
        lifecycleScope.launch {
            try {
                val scan = ApiClient.getService().scanQRCode(ScanQRRequest(payload)).awaitBody()
                val vendor = scan.vendor
                val qr = scan.qrCode
                check(scan.valid && vendor != null && qr != null) { scan.error ?: "Invalid QR code" }
                startActivity(Intent(this@QRScannerActivity, PaymentConfirmationActivity::class.java).apply {
                    putExtra(Constants.EXTRA_VENDOR_ID, vendor.id); putExtra(Constants.EXTRA_VENDOR_NAME, vendor.getDisplayName())
                    putExtra(Constants.EXTRA_SHORTCODE, vendor.businessShortcode); putExtra(Constants.EXTRA_SHORTCODE_TYPE, vendor.shortcodeType)
                    putExtra(Constants.EXTRA_QR_CODE_ID, qr.id); putExtra(Constants.EXTRA_IS_DYNAMIC, scan.isDynamic)
                    scan.amount?.let { putExtra(Constants.EXTRA_AMOUNT, it) }
                })
                finish()
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                Toast.makeText(this@QRScannerActivity, e.message, Toast.LENGTH_LONG).show()
                delay(1500); processing = false
            }
        }
    }
    override fun onResume() { super.onResume(); if (hasCamera()) binding.barcodeView.resume() }
    override fun onPause() { binding.barcodeView.pause(); super.onPause() }
}

