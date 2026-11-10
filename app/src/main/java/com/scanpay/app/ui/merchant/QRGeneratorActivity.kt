package com.scanpay.app.ui.merchant

import android.content.ClipData
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.scanpay.app.ScanPayApplication
import com.scanpay.app.data.api.*
import com.scanpay.app.data.request.GenerateQRRequest
import com.scanpay.app.databinding.ActivityQrGeneratorBinding
import com.scanpay.app.utils.CurrencyUtils
import kotlinx.coroutines.*
import java.io.File

class QRGeneratorActivity : AppCompatActivity() {
    private lateinit var binding: ActivityQrGeneratorBinding
    private var bitmap: Bitmap? = null
    private var payload: String? = null
    private val saveImage = registerForActivityResult(ActivityResultContracts.CreateDocument("image/png")) { uri ->
        val image = bitmap
        if (uri != null && image != null) lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    requireNotNull(contentResolver.openOutputStream(uri)).use { check(image.compress(Bitmap.CompressFormat.PNG, 100, it)) }
                }
                Toast.makeText(this@QRGeneratorActivity, "QR image saved", Toast.LENGTH_SHORT).show()
            } catch (_: Exception) { Toast.makeText(this@QRGeneratorActivity, "Could not save image", Toast.LENGTH_LONG).show() }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQrGeneratorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val session = ScanPayApplication.sessionManager
        binding.tvPreviewTitle.text = session.userName
        binding.tvPreviewShortcode.text = "Till / Paybill: ${session.businessShortcode.orEmpty()}"
        binding.btnBack.setOnClickListener { finish() }
        binding.rgQrType.setOnCheckedChangeListener { _, _ ->
            binding.tilAmount.visibility = if (binding.rbDynamic.isChecked) View.VISIBLE else View.GONE
        }
        binding.btnGenerate.setOnClickListener { generate() }
        binding.btnShareQr.setOnClickListener { share() }
        binding.btnSaveQr.setOnClickListener { if (bitmap != null) saveImage.launch("ScanPay-${System.currentTimeMillis()}.png") }
        binding.btnSavedQr.setOnClickListener { loadSaved() }
    }
    private fun generate() {
        val dynamic = binding.rbDynamic.isChecked
        val amount = if (dynamic) binding.etAmount.text.toString().toDoubleOrNull() else null
        if (dynamic && (amount == null || !amount.isFinite() || amount !in 1.0..500000.0 || amount % 1.0 != 0.0)) {
            binding.etAmount.error = "Enter a whole KES amount from 1 to 500,000"; return
        }
        val ref = binding.etReference.text.toString().trim().takeIf { it.isNotBlank() }
        if (ref != null && ref.length > 12) { binding.etReference.error = "Use at most 12 characters"; return }
        lifecycleScope.launch {
            binding.btnGenerate.isEnabled = false
            try {
                val result = ApiClient.getService().generateQRCode(GenerateQRRequest(if (dynamic) "DYNAMIC" else "STATIC", amount, ref)).awaitBody()
                show(result.payload, if (amount != null) "Bill: ${CurrencyUtils.formatKes(amount)}" else "Customer enters the amount")
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { Toast.makeText(this@QRGeneratorActivity, e.message, Toast.LENGTH_LONG).show() }
            finally { binding.btnGenerate.isEnabled = true }
        }
    }
    private fun show(text: String, caption: String) {
        payload = text
        bitmap = BarcodeEncoder().encodeBitmap(text, BarcodeFormat.QR_CODE, 900, 900)
        binding.ivGeneratedQr.setImageBitmap(bitmap)
        binding.tvPreviewAmount.text = caption
        binding.cardQrPreview.visibility = View.VISIBLE
    }
    private fun loadSaved() {
        lifecycleScope.launch {
            try {
                val codes = ApiClient.getService().getMerchantQRCodes().awaitBody()
                if (codes.isEmpty()) { Toast.makeText(this@QRGeneratorActivity, "Create your first QR to see it here", Toast.LENGTH_SHORT).show(); return@launch }
                val labels = codes.map { "${it.qrType} · ${it.referenceNumber ?: "Till payment"} · ${it.createdAt.take(10)}" }
                MaterialAlertDialogBuilder(this@QRGeneratorActivity).setTitle("Your payment QRs")
                    .setItems(labels.toTypedArray()) { _, index ->
                        val qr = codes[index]
                        show(qr.payloadData, "${qr.qrType} QR · ${qr.referenceNumber ?: "Customer enters amount"}")
                    }.setNegativeButton("Close", null).show()
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { Toast.makeText(this@QRGeneratorActivity, e.message, Toast.LENGTH_LONG).show() }
        }
    }
    private fun share() {
        val image = bitmap ?: return
        lifecycleScope.launch {
            try {
                val file = withContext(Dispatchers.IO) {
                    val dir = File(cacheDir, "qr").apply { mkdirs() }
                    File(dir, "scanpay.png").also { it.outputStream().use { output -> check(image.compress(Bitmap.CompressFormat.PNG, 100, output)) } }
                }
                val uri = FileProvider.getUriForFile(this@QRGeneratorActivity, "$packageName.files", file)
                startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"; putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_TEXT, "Pay ${ScanPayApplication.sessionManager.userName} with ScanPay")
                    clipData = ClipData.newUri(contentResolver, "ScanPay QR", uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }, "Share payment QR"))
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { Toast.makeText(this@QRGeneratorActivity, "Could not share image", Toast.LENGTH_LONG).show() }
        }
    }
}

