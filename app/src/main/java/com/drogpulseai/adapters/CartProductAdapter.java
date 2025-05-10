package com.drogpulseai.adapters;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.drogpulseai.R;
import com.drogpulseai.api.ApiClient;
import com.drogpulseai.models.Product;
import com.drogpulseai.models.ProductCartItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class CartProductAdapter extends RecyclerView.Adapter<CartProductAdapter.ViewHolder> {

    private static final String TAG = "CartProductAdapter";
    private static final int DEFAULT_QUANTITY = 1;
    private static final int MIN_QUANTITY = 1;
    private static final int MAX_QUANTITY = 99999;

    private final List<Product> allProducts; // Liste de tous les produits
    private final List<Product> filteredProducts; // Liste filtrée pour la recherche
    private final LayoutInflater inflater;
    private final Context context;
    private final Map<Integer, Integer> productQuantities = new HashMap<>(); // Map des quantités (ID produit -> quantité)
    private final Map<Integer, Double> productPrices = new HashMap<>(); // Map des prix de vente (ID produit -> prix)
    private final Set<Integer> selectedProducts = new HashSet<>(); // Ensemble des IDs de produits sélectionnés
    private final OnProductSelectionChangeListener listener;
    private String currentSearchQuery = ""; // Requête de recherche actuelle

    public interface OnProductSelectionChangeListener {
        void onSelectionChanged(int count, int totalItems);
    }

    public CartProductAdapter(Context context, List<Product> products, OnProductSelectionChangeListener listener) {
        this.context = context;
        this.allProducts = new ArrayList<>();
        this.allProducts.addAll(products);
        this.filteredProducts = new ArrayList<>(this.allProducts);
        this.inflater = LayoutInflater.from(context);
        this.listener = listener;

        Log.d(TAG, "Adaptateur initialisé avec " + this.allProducts.size() + " produits");
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.item_cart_product, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Product product = filteredProducts.get(position);
        int productId = product.getId();

        // Log de debug pour voir les valeurs
        Log.d(TAG, "Produit: " + product.getName() +
                " | Prix min: " + product.getPrixMinVente() +
                " | Prix conseillé: " + product.getPrixVenteConseille());

        // Afficher les informations du produit
        holder.tvReference.setText(product.getReference());
        holder.tvName.setText(product.getName());

        // Afficher la quantité en stock
        holder.tvQuantity.setVisibility(View.VISIBLE);
        holder.tvQuantity.setText(String.valueOf(product.getQuantity()));

        // Afficher le prix minimum de vente
        holder.tvPrixMin.setVisibility(View.VISIBLE);
        if (product.getPrixMinVente() > 0) {
            holder.tvPrixMin.setText(String.format(Locale.getDefault(), "%.2f", product.getPrixMinVente()));
        } else {
            holder.tvPrixMin.setText("Min");
        }

        // Afficher le prix de vente conseillé
        holder.tvPrixConseille.setVisibility(View.VISIBLE);
        if (product.getPrixVenteConseille() > 0) {
            holder.tvPrixConseille.setText(String.format(Locale.getDefault(), "%.2f", product.getPrixVenteConseille()));
        } else {
            holder.tvPrixConseille.setText("Cons.");
        }

        // Afficher le prix normal
        double productPrice = product.getPrice();
        holder.tvPrice.setVisibility(View.VISIBLE);
        holder.tvPrice.setText(String.format(Locale.getDefault(), "%.2f", productPrice));

        // Gérer l'état de sélection
        boolean isSelected = selectedProducts.contains(productId);
        holder.checkBox.setChecked(isSelected);

        // Gestion de la visibilité du conteneur parent
        holder.inputLayoutsContainer.setVisibility(isSelected ? View.VISIBLE : View.GONE);
        holder.inputPriceLayoutContainer.setVisibility(isSelected? View.VISIBLE : View.GONE);

        // Mettre à jour les valeurs des champs si sélectionné
        if (isSelected) {
            int quantity = productQuantities.getOrDefault(productId, DEFAULT_QUANTITY);
            holder.etQuantityValue.setText(String.valueOf(quantity));

            // Pour le prix de vente, privilégier le prix conseillé si disponible
            double salePrice = 0;
            if (product.getPrixVenteConseille() > 0) {
                salePrice = product.getPrixVenteConseille();
            } else if (productPrice > 0) {
                salePrice = productPrice;
            }

            // Si un prix a déjà été saisi manuellement, l'utiliser
            if (productPrices.containsKey(productId)) {
                salePrice = productPrices.get(productId);
            }
            holder.etSalePrice.setText(String.format(Locale.getDefault(), "%.2f", salePrice));
        }

        // Charger l'image avec Glide
        loadProductImage(holder.ivThumbnail, product);

        // Configurer les clics sur l'élément et la checkbox
        View.OnClickListener selectionClickListener = v -> {
            toggleSelection(productId);
            notifyItemChanged(holder.getAdapterPosition());

            // Notifier le listener du changement de sélection
            if (listener != null) {
                listener.onSelectionChanged(selectedProducts.size(), getTotalItemCount());
            }
        };

        holder.itemView.setOnClickListener(selectionClickListener);
        holder.checkBox.setOnClickListener(selectionClickListener);

        // Ajouter des TextWatcher pour suivre les changements dans les champs de saisie
        setupEditTextListeners(holder, productId);
    }

    // Méthode pour configurer les listeners sur les champs de saisie
    private void setupEditTextListeners(ViewHolder holder, int productId) {
        // Pour la quantité
        holder.etQuantityValue.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() > 0) {
                    try {
                        int quantity = Integer.parseInt(s.toString());
                        if (quantity > 0 && quantity <= MAX_QUANTITY) {
                            productQuantities.put(productId, quantity);

                            // Notifier le listener du changement de quantité
                            if (listener != null) {
                                listener.onSelectionChanged(selectedProducts.size(), getTotalItemCount());
                            }
                        }
                    } catch (NumberFormatException e) {
                        // Ignorer
                    }
                }
            }
        });

        // Pour le prix de vente
        holder.etSalePrice.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() > 0) {
                    try {
                        String priceStr = s.toString().replace(",", ".");
                        double price = Double.parseDouble(priceStr);
                        if (price > 0) {
                            productPrices.put(productId, price);

                            // Si le prix saisi est inférieur au prix minimum, afficher un avertissement
                            Product product = getProductById(productId);
                            if (product != null && product.getPrixMinVente() > 0 && price < product.getPrixMinVente()) {
                                // Vous pouvez ajouter ici un code pour afficher un avertissement visuel
                                Log.w(TAG, "Prix saisi inférieur au prix minimum recommandé pour le produit " + productId);
                            }
                        }
                    } catch (NumberFormatException e) {
                        // Ignorer
                    }
                }
            }
        });
    }

    private Product getProductById(int productId) {
        for (Product product : allProducts) {
            if (product.getId() == productId) {
                return product;
            }
        }
        return null;
    }

    private void toggleSelection(int productId) {
        if (selectedProducts.contains(productId)) {
            // Déselectionner le produit
            selectedProducts.remove(productId);
            productQuantities.remove(productId);
            productPrices.remove(productId);
        } else {
            // Sélectionner le produit
            selectedProducts.add(productId);
            productQuantities.put(productId, DEFAULT_QUANTITY);

            // Initialiser le prix de vente avec le prix conseillé ou le prix normal
            Product product = getProductById(productId);
            if (product != null) {
                double salePrice = 0;
                if (product.getPrixVenteConseille() > 0) {
                    salePrice = product.getPrixVenteConseille();
                } else if (product.getPrice() > 0) {
                    salePrice = product.getPrice();
                }

                if (salePrice > 0) {
                    productPrices.put(productId, salePrice);
                }
            }
        }
    }

    @Override
    public int getItemCount() {
        return filteredProducts.size();
    }

    /**
     * Obtenir le nombre total d'articles (somme des quantités)
     */
    private int getTotalItemCount() {
        int total = 0;
        for (Integer productId : selectedProducts) {
            total += productQuantities.getOrDefault(productId, DEFAULT_QUANTITY);
        }
        return total;
    }

    /**
     * Méthode pour charger l'image du produit
     */
    private void loadProductImage(ImageView imageView, Product product) {
        String photoUrl = product.getPhotoUrl();

        // Définir une image par défaut
        RequestOptions options = new RequestOptions()
                .placeholder(R.drawable.ic_image_placeholder)
                .error(R.drawable.ic_image_error)
                .diskCacheStrategy(DiskCacheStrategy.ALL);

        if (photoUrl != null && !photoUrl.isEmpty()) {
            // Construire l'URL complète si nécessaire
            String fullUrl;

            if (photoUrl.startsWith("http") || photoUrl.startsWith("https")) {
                // URL déjà complète
                fullUrl = photoUrl;
            } else {
                // Construire l'URL complète
                String baseUrl = ApiClient.getBaseUrl();
                if (!baseUrl.endsWith("/") && !photoUrl.startsWith("/")) {
                    baseUrl += "/";
                }
                fullUrl = baseUrl + photoUrl;
            }

            // Charger l'image avec Glide
            Glide.with(context)
                    .load(fullUrl)
                    .apply(options)
                    .into(imageView);
        } else {
            // Pas d'URL d'image, utiliser le placeholder par défaut
            Glide.with(context)
                    .load(R.drawable.ic_image_placeholder)
                    .into(imageView);
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public LinearLayout inputPriceLayoutContainer;
        TextView tvReference, tvName, tvQuantity, tvPrice, tvPrixMin, tvPrixConseille;
        ImageView ivThumbnail;
        CheckBox checkBox;
        LinearLayout inputLayoutsContainer; // Nouveau conteneur parent
        LinearLayout quantityInputLayout, salePriceLayout;
        EditText etQuantityValue, etSalePrice;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvReference = itemView.findViewById(R.id.tv_product_reference);
            tvName = itemView.findViewById(R.id.tv_product_name);
            tvQuantity = itemView.findViewById(R.id.tv_product_quantity);
            tvPrice = itemView.findViewById(R.id.tv_product_price);
            tvPrixMin = itemView.findViewById(R.id.tv_product_prix_min);
            tvPrixConseille = itemView.findViewById(R.id.tv_product_prix_conseille);
            ivThumbnail = itemView.findViewById(R.id.iv_product_thumbnail);
            checkBox = itemView.findViewById(R.id.checkbox_select_product);

            // Nouveau conteneur parent
            inputLayoutsContainer = itemView.findViewById(R.id.input_layouts_container);
            inputPriceLayoutContainer = itemView.findViewById(R.id.input_priceManager_layouts_container);
            // Sous-éléments
            quantityInputLayout = itemView.findViewById(R.id.quantity_input_layout);
            salePriceLayout = itemView.findViewById(R.id.sale_price_layout);
            etQuantityValue = itemView.findViewById(R.id.et_quantity_value);
            etSalePrice = itemView.findViewById(R.id.et_sale_price);
        }
    }

    /**
     * Récupérer la liste des produits sélectionnés avec leurs quantités
     */
    public List<ProductCartItem> getSelectedProductItems() {
        List<ProductCartItem> items = new ArrayList<>();

        for (Product product : allProducts) {
            int productId = product.getId();
            if (selectedProducts.contains(productId)) {
                int quantity = productQuantities.getOrDefault(productId, DEFAULT_QUANTITY);
                double price = productPrices.getOrDefault(productId, product.getPrice());

                // Créer une copie du produit avec le prix modifié
                Product productCopy = new Product();
                productCopy.setId(product.getId());
                productCopy.setReference(product.getReference());
                productCopy.setLabel(product.getLabel());
                productCopy.setName(product.getName());
                productCopy.setDescription(product.getDescription());
                productCopy.setPhotoUrl(product.getPhotoUrl());
                productCopy.setBarcode(product.getBarcode());
                productCopy.setQuantity(product.getQuantity());
                productCopy.setUserId(product.getUserId());
                productCopy.setPrice(price); // Utiliser le prix de vente personnalisé
                productCopy.setPrixMinVente(product.getPrixMinVente());
                productCopy.setPrixVenteConseille(product.getPrixVenteConseille());
                productCopy.setCoutDeRevientUnitaire(product.getCoutDeRevientUnitaire());

                // Définir les champs de suivi des modifications
                productCopy.setLastUpdated(System.currentTimeMillis());
                productCopy.setDirty(true);

                items.add(new ProductCartItem(productCopy, quantity));
            }
        }

        return items;
    }

    /**
     * Vider la sélection
     */
    public void clearSelection() {
        selectedProducts.clear();
        productQuantities.clear();
        productPrices.clear();
        notifyDataSetChanged();

        if (listener != null) {
            listener.onSelectionChanged(0, 0);
        }
    }

    /**
     * Sélectionner tous les produits visibles (filtrés)
     */
    public void selectAll() {
        for (Product product : filteredProducts) {
            int productId = product.getId();
            selectedProducts.add(productId);

            // Initialiser la quantité si elle n'est pas déjà définie
            if (!productQuantities.containsKey(productId)) {
                productQuantities.put(productId, DEFAULT_QUANTITY);
            }

            // Initialiser le prix de vente avec le prix conseillé ou normal si non défini
            if (!productPrices.containsKey(productId)) {
                double salePrice = 0;
                if (product.getPrixVenteConseille() > 0) {
                    salePrice = product.getPrixVenteConseille();
                } else if (product.getPrice() > 0) {
                    salePrice = product.getPrice();
                }

                if (salePrice > 0) {
                    productPrices.put(productId, salePrice);
                }
            }
        }

        notifyDataSetChanged();

        if (listener != null) {
            listener.onSelectionChanged(selectedProducts.size(), getTotalItemCount());
        }
    }

    /**
     * Filtrer les produits en fonction de la requête de recherche
     */
    public void filter(String query) {
        currentSearchQuery = query.toLowerCase().trim();
        filteredProducts.clear();

        if (currentSearchQuery.isEmpty()) {
            // Si la requête est vide, afficher tous les produits
            filteredProducts.addAll(allProducts);
        } else {
            // Sinon, filtrer les produits selon la requête
            for (Product product : allProducts) {
                // Vérifier si le produit correspond à la requête
                if (matchesSearchQuery(product, currentSearchQuery)) {
                    filteredProducts.add(product);
                }
            }
        }

        notifyDataSetChanged();
    }

    /**
     * Vérifier si un produit correspond à la requête de recherche
     */
    private boolean matchesSearchQuery(Product product, String query) {
        // Vérifier différents champs du produit
        return product.getName().toLowerCase().contains(query)
                || product.getReference().toLowerCase().contains(query)
                || (product.getBarcode() != null && product.getBarcode().toLowerCase().contains(query))
                || (product.getDescription() != null && product.getDescription().toLowerCase().contains(query));
    }

    /**
     * Mettre à jour la liste des produits
     */
    public void updateProducts(List<Product> newProducts) {
        allProducts.clear();

        // Ajouter des valeurs de test pour certains produits si nécessaire
        for (Product product : newProducts) {
            // Si les valeurs sont absentes, leur donner des valeurs de test
            if (product.getPrixMinVente() <= 0) {
                product.setPrixMinVente(product.getPrice() * 0.7); // 70% du prix normal comme exemple
            }
            if (product.getPrixVenteConseille() <= 0) {
                product.setPrixVenteConseille(product.getPrice() * 1.2); // 120% du prix normal comme exemple
            }

            allProducts.add(product);
        }

        // Mettre également à jour la liste filtrée
        filter(currentSearchQuery);
    }

    /**
     * Réinitialiser la liste filtrée pour afficher tous les produits
     */
    public void showAllProducts() {
        currentSearchQuery = "";
        filteredProducts.clear();
        filteredProducts.addAll(allProducts);
        notifyDataSetChanged();
    }
}