package com.drogpulseai.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.drogpulseai.R;
import com.drogpulseai.utils.LanguageManager;

import java.util.List;

/**
 * Adaptateur personnalisé pour la liste des langues avec indication de la langue sélectionnée
 */
public class LanguageAdapter extends ArrayAdapter<LanguageManager.LanguageItem> {

    private final String currentLanguageCode;
    private final LayoutInflater inflater;

    /**
     * Constructeur
     * @param context Contexte de l'application
     * @param languageItems Liste des langues disponibles
     * @param currentLanguageCode Code de la langue actuellement sélectionnée
     */
    public LanguageAdapter(Context context, List<LanguageManager.LanguageItem> languageItems, String currentLanguageCode) {
        super(context, R.layout.item_language, languageItems);
        this.currentLanguageCode = currentLanguageCode;
        this.inflater = LayoutInflater.from(context);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        // Réutiliser la vue si possible
        View view = convertView;
        if (view == null) {
            view = inflater.inflate(R.layout.item_language, parent, false);
        }

        // Récupérer les vues
        TextView tvLanguageName = view.findViewById(R.id.tv_language_name);
        ImageView ivLanguageCheck = view.findViewById(R.id.iv_language_check);

        // Obtenir l'élément de langue à cette position
        LanguageManager.LanguageItem item = getItem(position);

        if (item != null) {
            // Définir le nom de la langue
            tvLanguageName.setText(item.getName());

            // Afficher la coche si c'est la langue actuelle
            boolean isSelected = item.getCode().equals(currentLanguageCode);

            // Cas spécial pour "auto" - vérifier si l'option auto est sélectionnée
            if ("auto".equals(item.getCode()) && "auto".equals(currentLanguageCode)) {
                isSelected = true;
            }

            ivLanguageCheck.setVisibility(isSelected ? View.VISIBLE : View.GONE);
        }

        return view;
    }
}