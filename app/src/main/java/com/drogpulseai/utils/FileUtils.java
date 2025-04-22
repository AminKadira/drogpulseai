package com.drogpulseai.utils;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class FileUtils {

    public static File getFileFromUri(Context context, Uri uri) {
        try {
            // Essayer d'obtenir le chemin réel si possible
            String realPath = getRealPathFromURI(context, uri);

            if (realPath != null) {
                return new File(realPath);
            }

            // Fallback: Copier le fichier en mémoire interne
            String fileName = "photo_" + System.currentTimeMillis() + ".jpg";
            File outputFile = new File(context.getCacheDir(), fileName);

            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            FileOutputStream outputStream = new FileOutputStream(outputFile);

            byte[] buffer = new byte[4 * 1024]; // 4k buffer
            int read;

            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }

            outputStream.flush();
            outputStream.close();
            inputStream.close();

            return outputFile;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static String getRealPathFromURI(Context context, Uri contentUri) {
        Cursor cursor = null;
        try {
            String[] proj = { MediaStore.Images.Media.DATA };
            cursor = context.getContentResolver().query(contentUri, proj, null, null, null);

            if (cursor == null) {
                return null;
            }

            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } catch (Exception e) {
            return null;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }
}