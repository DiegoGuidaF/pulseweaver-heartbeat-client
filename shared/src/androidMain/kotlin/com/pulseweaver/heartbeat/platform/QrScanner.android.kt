package com.pulseweaver.heartbeat.platform

import android.content.pm.PackageManager
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.pulseweaver.heartbeat.ActivityHolder
import com.pulseweaver.heartbeat.ApplicationContextHolder
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

actual object QrScanner {
    actual fun isAvailable(): Boolean =
        ApplicationContextHolder.context.packageManager
            .hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)

    actual suspend fun scan(): String? {
        // The scanner launches its own activity; prefer the live one, fall back to app context.
        val context = ActivityHolder.get() ?: ApplicationContextHolder.context
        val options =
            GmsBarcodeScannerOptions
                .Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build()
        val scanner = GmsBarcodeScanning.getClient(context, options)

        return suspendCancellableCoroutine { cont ->
            scanner
                .startScan()
                .addOnSuccessListener { barcode ->
                    if (cont.isActive) cont.resume(barcode.rawValue)
                }.addOnCanceledListener {
                    if (cont.isActive) cont.resume(null)
                }.addOnFailureListener {
                    if (cont.isActive) cont.resume(null)
                }
        }
    }
}
