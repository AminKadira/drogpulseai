package com.drogpulseai.utils;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Utility class for file operations, enhanced with image processing
 */
public class FileUtils {

    private static final String TAG = "FileUtils";
    private static final int DEFAULT_JPEG_QUALITY = 85;

    /**
     * Get a file from a content Uri
     */
    public static File getFileFromUri(Context context, Uri uri) {
        try {
            // Try to get real path if possible
            String realPath = getRealPathFromURI(context, uri);

            if (realPath != null) {
                return new File(realPath);
            }

            // Fallback: Copy file to internal storage
            String fileName = "file_" + System.currentTimeMillis() + getFileExtension(uri.toString());
            File outputFile = new File(context.getCacheDir(), fileName);

            try (InputStream inputStream = context.getContentResolver().openInputStream(uri);
                 OutputStream outputStream = new FileOutputStream(outputFile)) {

                if (inputStream == null) {
                    Log.e(TAG, "Failed to open input stream");
                    return null;
                }

                byte[] buffer = new byte[4 * 1024]; // 4k buffer
                int read;

                while ((read = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, read);
                }

                outputStream.flush();
            }

            return outputFile;
        } catch (Exception e) {
            Log.e(TAG, "Error getting file from Uri", e);
            return null;
        }
    }

    /**
     * Get the real file path from a content Uri
     */
    private static String getRealPathFromURI(Context context, Uri contentUri) {
        String[] projection = {MediaStore.Images.Media.DATA};

        try (Cursor cursor = context.getContentResolver().query(contentUri, projection, null, null, null)) {
            if (cursor == null) {
                return null;
            }

            int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(columnIndex);
        } catch (Exception e) {
            Log.e(TAG, "Error getting real path", e);
            return null;
        }
    }

    /**
     * Get file extension from path or URL
     */
    private static String getFileExtension(String path) {
        if (path == null) {
            return ".jpg";
        }

        int lastDot = path.lastIndexOf('.');
        if (lastDot >= 0) {
            return path.substring(lastDot);
        } else {
            return ".jpg";
        }
    }

    /**
     * Compress and resize an image
     * @param context The context
     * @param imageUri The image Uri
     * @param maxWidth Maximum width
     * @param maxHeight Maximum height
     * @return The processed image file
     */
    public static File compressAndResizeImage(Context context, Uri imageUri, int maxWidth, int maxHeight) {
        try {
            // Get input stream
            InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
            if (inputStream == null) {
                return null;
            }

            // Get original bitmap dimensions
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(inputStream, null, options);
            inputStream.close();

            // Calculate sample size
            options.inSampleSize = calculateInSampleSize(options, maxWidth, maxHeight);

            // Decode bitmap with sample size
            options.inJustDecodeBounds = false;
            inputStream = context.getContentResolver().openInputStream(imageUri);
            if (inputStream == null) {
                return null;
            }

            Bitmap originalBitmap = BitmapFactory.decodeStream(inputStream, null, options);
            inputStream.close();

            if (originalBitmap == null) {
                return null;
            }

            // Fix orientation if needed
            Bitmap rotatedBitmap = fixOrientation(context, imageUri, originalBitmap);

            // Scale if still too large
            Bitmap scaledBitmap = scaleBitmap(rotatedBitmap, maxWidth, maxHeight);

            // Create output file
            File outputFile = new File(context.getCacheDir(), "compressed_" + System.currentTimeMillis() + ".jpg");

            // Write to file
            try (FileOutputStream out = new FileOutputStream(outputFile)) {
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, DEFAULT_JPEG_QUALITY, out);
            }

            // Recycle bitmaps
            if (scaledBitmap != rotatedBitmap) {
                rotatedBitmap.recycle();
            }
            if (originalBitmap != rotatedBitmap) {
                originalBitmap.recycle();
            }

            return outputFile;
        } catch (IOException e) {
            Log.e(TAG, "Error compressing image", e);
            return null;
        }
    }

    /**
     * Calculate sample size for bitmap decoding
     */
    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    /**
     * Fix image orientation based on EXIF data
     */
    private static Bitmap fixOrientation(Context context, Uri imageUri, Bitmap bitmap) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
            if (inputStream == null) {
                return bitmap;
            }

            ExifInterface exif = new ExifInterface(inputStream);
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

            Matrix matrix = new Matrix();
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    matrix.postRotate(90);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    matrix.postRotate(180);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    matrix.postRotate(270);
                    break;
                default:
                    return bitmap;
            }

            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        } catch (IOException e) {
            Log.e(TAG, "Error fixing orientation", e);
            return bitmap;
        }
    }

    /**
     * Scale bitmap if needed
     */
    private static Bitmap scaleBitmap(Bitmap bitmap, int maxWidth, int maxHeight) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        if (width <= maxWidth && height <= maxHeight) {
            return bitmap;
        }

        float ratio = Math.min((float) maxWidth / width, (float) maxHeight / height);

        int newWidth = Math.round(width * ratio);
        int newHeight = Math.round(height * ratio);

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
    }
}