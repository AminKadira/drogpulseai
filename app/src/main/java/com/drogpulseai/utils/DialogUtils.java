package com.drogpulseai.utils;

import android.content.Context;

import androidx.appcompat.app.AlertDialog;

/**
 * Utility class for creating standardized dialogs
 */
public class DialogUtils {

    /**
     * Show a confirmation dialog
     * @param context The context
     * @param title The dialog title
     * @param message The dialog message
     * @param onConfirm Callback to execute when confirmed
     */
    public static void showConfirmationDialog(Context context, String title, String message, Runnable onConfirm) {
        new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Oui", (dialog, which) -> {
                    if (onConfirm != null) {
                        onConfirm.run();
                    }
                })
                .setNegativeButton("Non", null)
                .show();
    }

    /**
     * Show an error dialog
     * @param context The context
     * @param title The dialog title
     * @param message The error message
     */
    public static void showErrorDialog(Context context, String title, String message) {
        new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    /**
     * Show an information dialog
     * @param context The context
     * @param title The dialog title
     * @param message The information message
     */
    public static void showInfoDialog(Context context, String title, String message) {
        new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .setIcon(android.R.drawable.ic_dialog_info)
                .show();
    }

    /**
     * Show a choice dialog
     * @param context The context
     * @param title The dialog title
     * @param items The items to choose from
     * @param onItemSelected Callback when an item is selected
     */
    public static void showChoiceDialog(Context context, String title, String[] items, OnItemSelectedListener onItemSelected) {
        new AlertDialog.Builder(context)
                .setTitle(title)
                .setItems(items, (dialog, which) -> {
                    if (onItemSelected != null) {
                        onItemSelected.onItemSelected(which);
                    }
                })
                .show();
    }

    /**
     * Interface for item selection callback
     */
    public interface OnItemSelectedListener {
        void onItemSelected(int position);
    }
}