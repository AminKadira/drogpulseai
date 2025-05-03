package com.drogpulseai.utils;

import android.app.Activity;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

/**
 * Helper class for barcode scanning
 */
public class BarcodeHelper {

    private static final String TAG = "BarcodeHelper";

    // Callback interface
    public interface BarcodeCallback {
        void onBarcodeScanned(String barcode);
    }

    private final Activity activity;
    private final BarcodeCallback callback;

    public BarcodeHelper(Activity activity, BarcodeCallback callback) {
        this.activity = activity;
        this.callback = callback;
    }

    /**
     * Initialize and launch barcode scanner
     */
    public void scanBarcode() {
        IntentIntegrator integrator = new IntentIntegrator(activity);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES);
        integrator.setPrompt("Scannez un code-barres");
        integrator.setCameraId(0);  // Use default rear camera
        integrator.setBeepEnabled(true);
        integrator.setBarcodeImageEnabled(true);
        integrator.initiateScan();
    }

    /**
     * Handle scan result
     */
    public void handleScanResult(IntentResult result) {
        if (result.getContents() != null) {
            // Scan successful
            String barcode = result.getContents();
            Toast.makeText(activity, "Code-barres scanné : " + barcode, Toast.LENGTH_SHORT).show();

            // Notify callback
            if (callback != null) {
                callback.onBarcodeScanned(barcode);
            }
        } else {
            // Scan cancelled
            Toast.makeText(activity, "Scan annulé", Toast.LENGTH_SHORT).show();
        }
    }
}