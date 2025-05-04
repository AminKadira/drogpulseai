package com.drogpulseai.utils;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.ImageView;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.drogpulseai.R;
import com.drogpulseai.api.ApiClient;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Helper class for handling image selection and display
 */
public class ImageHelper {

    private static final String TAG = "ImageHelper";

    // Request codes
    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int TAKE_PHOTO_REQUEST = 2;

    // Callback interface
    public interface ImageSelectionCallback {
        void onImageSelected(Uri imageUri);
    }

    private final Activity activity;
    private final ImageSelectionCallback callback;
    private String currentPhotoPath;
    private Uri selectedImageUri;

    public ImageHelper(Activity activity, ImageSelectionCallback callback) {
        this.activity = activity;
        this.callback = callback;
    }

    /**
     * Show dialog for selecting image source (camera or gallery)
     */
    public void showImageSourceDialog() {

        String[] options = {"Prendre une photo", "Choisir depuis la galerie"};

        new AlertDialog.Builder(activity)
                .setTitle("Ajouter une photo")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        dispatchTakePictureIntent();
                    } else {
                        openGallery();
                    }
                })
                .show();
    }

    /**
     * Open gallery for image selection
     */
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        activity.startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    /**
     * Launch camera for taking a photo
     */
    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        // Ensure there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(activity.getPackageManager()) != null) {
            // Create a file to save the image
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Log.e(TAG, "Error creating image file", ex);
                DialogUtils.showErrorDialog(activity, "Erreur", "Impossible de créer le fichier image");
                return;
            }

            // Continue only if the file was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(activity,
                        "com.drogpulseai.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                activity.startActivityForResult(takePictureIntent, TAKE_PHOTO_REQUEST);
            }
        } else {
            DialogUtils.showErrorDialog(activity, "Erreur", "Aucune application de caméra disponible");
        }
    }

    /**
     * Create a file for storing the image
     */
    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );

        // Save the file path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    /**
     * Handle activity result for image selection
     */
    public void handleActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK) {
            return;
        }

        if (requestCode == PICK_IMAGE_REQUEST && data != null && data.getData() != null) {
            // Image selected from gallery
            selectedImageUri = data.getData();
            notifyImageSelected(selectedImageUri);
        } else if (requestCode == TAKE_PHOTO_REQUEST) {
            // Photo taken with camera
            if (currentPhotoPath != null) {
                // Add photo to gallery
                galleryAddPic();

                // Create URI
                File f = new File(currentPhotoPath);
                selectedImageUri = Uri.fromFile(f);
                notifyImageSelected(selectedImageUri);
            }
        }
    }

    /**
     * Add photo to the gallery
     */
    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(currentPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        activity.sendBroadcast(mediaScanIntent);
    }

    /**
     * Notify callback of image selection
     */
    private void notifyImageSelected(Uri imageUri) {
        if (callback != null) {
            callback.onImageSelected(imageUri);
        }
    }

    /**
     * Display image in ImageView
     */
    public void displayImage(String photoUrl, ImageView imageView) {
        if (photoUrl == null || photoUrl.isEmpty()) {
            return;
        }

        // Set up Glide request options
        RequestOptions options = new RequestOptions()
                .placeholder(R.drawable.ic_image_placeholder)
                .error(R.drawable.ic_image_error)
                .diskCacheStrategy(DiskCacheStrategy.ALL);

        // Build complete URL if needed
        String fullUrl;
        if (photoUrl.startsWith("http") || photoUrl.startsWith("https")) {
            fullUrl = photoUrl;
        } else {
            String baseUrl = ApiClient.getBaseUrl();
            if (!baseUrl.endsWith("/") && !photoUrl.startsWith("/")) {
                baseUrl += "/";
            }
            fullUrl = baseUrl + photoUrl;
        }

        // Load image with Glide
        Glide.with(activity)
                .load(fullUrl)
                .apply(options)
                .into(imageView);
    }

    /**
     * Clean up resources
     */
    public void cleanup() {
        // Delete temporary photo file if it exists
        if (currentPhotoPath != null) {
            File file = new File(currentPhotoPath);
            if (file.exists()) {
                file.delete();
            }
        }
    }
}