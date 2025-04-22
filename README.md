# Documentation Technique - DrogPulseAI

## 1. Introduction

DrogPulseAI est une application mobile Android de gestion de contacts et de produits développée spécifiquement pour les professionnels du secteur de la quincaillerie. L'application suit une architecture client-serveur avec une application mobile Android comme frontend et une API PHP comme backend, connectée à une base de données MySQL.

**Objectifs du projet :**
- Faciliter la gestion des contacts professionnels
- Fournir un système de gestion de stock de produits
- Assurer la sécurité des données utilisateur
- Offrir une interface intuitive et performante

## 2. Architecture Globale

L'architecture du système DrogPulseAI est composée de trois couches principales :

```
┌─────────────────┐        ┌─────────────────┐        ┌─────────────────┐
│   Application   │        │      API        │        │      Base       │
│    Android      │◄─────► │      PHP        │◄─────► │    de données   │
│    (Client)     │   HTTP  │    (Serveur)    │  SQL   │     MySQL      │
└─────────────────┘        └─────────────────┘        └─────────────────┘
```

### Flux de données
1. L'utilisateur interagit avec l'application Android
2. L'application effectue des requêtes HTTP vers l'API REST PHP
3. L'API traite les requêtes, effectue les opérations sur la base de données et renvoie les résultats
4. L'application affiche les données à l'utilisateur

## 3. Composants du Système

### 3.1 Application Android (Frontend)

#### Structure du projet

```
app/
├── build.gradle.kts           # Configuration Gradle
├── src/main/
    ├── java/com/drogpulseai/  # Code source Java
    │   ├── activities/        # Écrans de l'application
    │   ├── adapters/          # Adaptateurs pour RecyclerView
    │   ├── api/               # Configuration API
    │   ├── models/            # Modèles de données
    │   └── utils/             # Utilitaires
    ├── res/                   # Ressources
    └── AndroidManifest.xml    # Configuration Android
```

#### Principaux composants

1. **Activities** : Écrans de l'application
    - `LoginActivity` - Authentification
    - `RegisterActivity` - Inscription
    - `MainActivity` - Écran principal (liste des contacts)
    - `ContactFormActivity` - Formulaire de création/modification de contact
    - `ContactSearchActivity` - Recherche de contacts
    - `ProductListActivity` - Liste des produits
    - `ProductFormActivity` - Formulaire de création/modification de produit
    - `ProductSearchActivity` - Recherche de produits

2. **Models** : Classes de données
    - `User` - Modèle utilisateur
    - `Contact` - Modèle contact
    - `Product` - Modèle produit

3. **API** : Configuration client API
    - `ApiClient` - Configuration de Retrofit
    - `ApiService` - Définition des endpoints

4. **Utils** : Classes utilitaires
    - `SessionManager` - Gestion de session
    - `LocationUtils` - Gestion de la géolocalisation
    - `ConnectionChecker` - Vérification de connexion
    - `FileUtils` - Gestion des fichiers

#### Technologies utilisées

- **Langage** : Java
- **SDK minimum** : API 24 (Android 7.0 Nougat)
- **SDK cible** : API 34 (Android 14)
- **Bibliothèques principales** :
    - Retrofit (2.9.0) - Client HTTP pour les appels API
    - Gson (2.10) - Parsing JSON
    - OkHttp (4.11.0) - Client HTTP
    - Glide (4.15.1) - Chargement d'images
    - Google Play Services Location (21.0.1) - Services de localisation
    - Material Components (1.9.0) - Composants d'interface utilisateur
    - RecyclerView & SwipeRefreshLayout - Affichage de listes

### 3.2 API REST PHP (Backend)

#### Structure du projet

```
api/
├── auth/                 # Authentification
│   ├── login.php
│   └── register.php
├── contacts/             # Gestion des contacts
│   ├── list.php
│   ├── search.php
│   ├── details.php
│   ├── create.php
│   ├── update.php
│   └── delete.php
├── products/             # Gestion des produits
│   ├── list.php
│   ├── search.php
│   ├── details.php
│   ├── create.php
│   ├── update.php
│   ├── delete.php
│   └── upload_photo.php
├── config/               # Configuration
│   └── database.php
├── utils/                # Utilitaires
│   └── response.php
└── uploads/              # Dossier des fichiers uploadés
    └── products/         # Photos de produits
```

#### Endpoints API

1. **Authentification**
    - `POST /auth/login.php` - Connexion utilisateur
    - `POST /auth/register.php` - Inscription utilisateur

2. **Gestion des contacts**
    - `GET /contacts/list.php?user_id=X` - Liste des contacts
    - `GET /contacts/search.php?user_id=X&query=Y` - Recherche de contacts
    - `GET /contacts/details.php?id=X` - Détails d'un contact
    - `POST /contacts/create.php` - Création d'un contact
    - `PUT /contacts/update.php` - Mise à jour d'un contact
    - `DELETE /contacts/delete.php?id=X` - Suppression d'un contact

3. **Gestion des produits**
    - `GET /products/list.php?user_id=X` - Liste des produits
    - `GET /products/search.php?user_id=X&query=Y` - Recherche de produits
    - `GET /products/details.php?id=X` - Détails d'un produit
    - `POST /products/create.php` - Création d'un produit
    - `PUT /products/update.php` - Mise à jour d'un produit
    - `DELETE /products/delete.php?id=X` - Suppression d'un produit
    - `POST /products/upload_photo.php` - Upload d'une photo de produit

#### Format des réponses

Toutes les réponses de l'API suivent le même format JSON :

```json
{
    "success": true|false,
    "message": "Message informatif",
    "data": { /* Données optionnelles */ }
}
```

### 3.3 Base de données MySQL

#### Schéma de la base de données

```
┌────────────────┐       ┌─────────────────┐
│     users      │       │    contacts     │
├────────────────┤       ├─────────────────┤
│ id (PK)        │       │ id (PK)         │
│ nom            │       │ nom             │
│ prenom         │1      │ prenom          │
│ telephone      ├─────►n│ telephone       │
│ email (unique) │       │ email           │
│ password       │       │ notes           │
│ latitude       │       │ latitude        │
│ longitude      │       │ longitude       │
│ created_at     │       │ user_id (FK)    │
│ updated_at     │       │ created_at      │
└────────────────┘       │ updated_at      │
                         └─────────────────┘
                                 ▲
                                 │
                                 │1
                         ┌───────────────┐
                         │   products    │
                         ├───────────────┤
                         │ id (PK)       │
                         │ reference     │
                         │ label         │
                         │ name          │
                         │ description   │
                         │ photo_url     │
                         │ barcode       │
                         │ quantity      │
                         │ user_id (FK)  │
                         │ created_at    │
                         │ updated_at    │
                         └───────────────┘
```

#### Définition des tables

1. **users**
```sql
CREATE TABLE `users` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `nom` varchar(100) NOT NULL,
  `prenom` varchar(100) NOT NULL,
  `telephone` varchar(20) NOT NULL,
  `latitude` double NOT NULL,
  `longitude` double NOT NULL,
  `email` varchar(100) NOT NULL UNIQUE,
  `password` varchar(255) NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
);
```

2. **contacts**
```sql
CREATE TABLE `contacts` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `nom` varchar(100) NOT NULL,
  `prenom` varchar(100) NOT NULL,
  `telephone` varchar(20) NOT NULL,
  `email` varchar(100) NULL,
  `notes` text NULL,
  `latitude` double NOT NULL,
  `longitude` double NOT NULL,
  `user_id` int(11) NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
);
```

3. **products**
```sql
CREATE TABLE `products` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `reference` varchar(50) NOT NULL UNIQUE,
  `label` varchar(100) NOT NULL,
  `name` varchar(150) NOT NULL,
  `description` text,
  `photo_url` varchar(255),
  `barcode` varchar(50),
  `quantity` int(11) DEFAULT 0,
  `user_id` int(11) NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
);
```

## 4. Guide de configuration de l'environnement

### 4.1 Configuration de l'environnement de développement Android

1. **Prérequis**
    - Android Studio (dernière version)
    - JDK 8 ou supérieur
    - SDK Android (API level 24 minimum)

2. **Récupération du projet**
   ```bash
   git clone [URL_DU_REPOSITORY]
   ```

3. **Ouverture du projet**
    - Ouvrir Android Studio
    - Sélectionner "Open an existing project"
    - Naviguer vers le dossier du projet cloné

4. **Configuration de l'API**
    - Ouvrir `app/src/main/java/com/drogpulseai/api/ApiClient.java`
    - Modifier la constante `BASE_URL` pour pointer vers votre serveur API
   ```java
   public static final String BASE_URL = "http://votre-serveur.com/drogpulseai_Api/api/";
   ```

5. **Exécution**
    - Connecter un appareil Android ou configurer un émulateur
    - Cliquer sur "Run" (▶) dans Android Studio

### 4.2 Configuration du serveur backend

1. **Prérequis**
    - Serveur web (Apache, Nginx)
    - PHP 7.4 ou supérieur
    - MySQL 5.7 ou supérieur
    - Extensions PHP requises : PDO, JSON, GD (pour le traitement des images)

2. **Installation des fichiers**
    - Copier le dossier `api` dans le répertoire web du serveur
    - Créer les dossiers d'upload et configurer les permissions
   ```bash
   mkdir -p api/uploads/products
   chmod -R 755 api/uploads
   ```

3. **Configuration de la base de données**
    - Créer une base de données MySQL
   ```sql
   CREATE DATABASE drogpulseai;
   ```
    - Créer un utilisateur et lui attribuer les droits
   ```sql
   CREATE USER 'drogpulseai_user'@'localhost' IDENTIFIED BY 'password';
   GRANT ALL PRIVILEGES ON drogpulseai.* TO 'drogpulseai_user'@'localhost';
   FLUSH PRIVILEGES;
   ```
    - Importer les tables SQL (scripts fournis dans la section 3.3)
    - Modifier les informations de connexion dans `api/config/database.php`
   ```php
   private $host = "localhost";
   private $db_name = "drogpulseai";
   private $username = "drogpulseai_user";
   private $password = "password";
   ```

4. **Test de l'API**
    - Vérifier que l'API est accessible via le navigateur
   ```
   http://votre-serveur.com/drogpulseai_Api/api/ping.php
   ```
    - Vous devriez recevoir une réponse JSON indiquant que le serveur est connecté

## 5. Guides de développement

### 5.1 Ajout d'une nouvelle activité Android

1. **Création de la classe d'activité**
    - Créer une nouvelle classe Java dans le package `com.drogpulseai.activities`
    - Faire hériter de `AppCompatActivity`
    - Implémenter la méthode `onCreate()`

   ```java
   package com.drogpulseai.activities;

   import android.os.Bundle;
   import androidx.appcompat.app.AppCompatActivity;
   import com.drogpulseai.R;

   public class NouvelleActivity extends AppCompatActivity {
       @Override
       protected void onCreate(Bundle savedInstanceState) {
           super.onCreate(savedInstanceState);
           setContentView(R.layout.activity_nouvelle);
           
           // Initialisation des vues et des écouteurs
       }
   }
   ```

2. **Création du layout XML**
    - Créer un nouveau fichier XML dans `res/layout/`
   ```xml
   <?xml version="1.0" encoding="utf-8"?>
   <androidx.constraintlayout.widget.ConstraintLayout 
       xmlns:android="http://schemas.android.com/apk/res/android"
       xmlns:app="http://schemas.android.com/apk/res-auto"
       xmlns:tools="http://schemas.android.com/tools"
       android:layout_width="match_parent"
       android:layout_height="match_parent"
       tools:context=".activities.NouvelleActivity">
       
       <!-- Contenu de l'activité -->
       
   </androidx.constraintlayout.widget.ConstraintLayout>
   ```

3. **Déclaration dans le Manifest**
    - Ouvrir `AndroidManifest.xml`
    - Ajouter la déclaration de l'activité
   ```xml
   <activity
       android:name=".activities.NouvelleActivity"
       android:exported="false" />
   ```

4. **Navigation vers l'activité**
    - Utiliser un Intent pour démarrer l'activité
   ```java
   Intent intent = new Intent(this, NouvelleActivity.class);
   startActivity(intent);
   ```

### 5.2 Ajout d'un nouvel endpoint API

1. **Création du fichier PHP**
    - Créer un nouveau fichier dans le dossier approprié de l'API
    - Exemple pour un nouvel endpoint dans la gestion des produits
   ```php
   <?php
   // api/products/nouveau_endpoint.php
   
   // Headers requis
   header("Access-Control-Allow-Origin: *");
   header("Content-Type: application/json; charset=UTF-8");
   header("Access-Control-Allow-Methods: POST");
   header("Access-Control-Max-Age: 3600");
   header("Access-Control-Allow-Headers: Content-Type, Access-Control-Allow-Headers, Authorization, X-Requested-With");
   
   // Inclure les fichiers de configuration et d'utilitaires
   include_once '../config/database.php';
   include_once '../utils/response.php';
   
   // Logique métier
   
   // Réponse
   Response::success("Opération réussie");
   ?>
   ```

2. **Ajout dans l'interface ApiService**
    - Ouvrir `app/src/main/java/com/drogpulseai/api/ApiService.java`
    - Ajouter la méthode correspondante
   ```java
   @POST("products/nouveau_endpoint.php")
   Call<Map<String, Object>> nouveauEndpoint(@Body RequestBody data);
   ```

3. **Utilisation dans l'application**
   ```java
   // Préparation des données
   Map<String, Object> requestData = new HashMap<>();
   requestData.put("param1", "valeur1");
   requestData.put("param2", "valeur2");
   
   // Appel à l'API
   apiService.nouveauEndpoint(requestData).enqueue(new Callback<Map<String, Object>>() {
       @Override
       public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
           if (response.isSuccessful() && response.body() != null) {
               // Traitement de la réponse
           }
       }
       
       @Override
       public void onFailure(Call<Map<String, Object>> call, Throwable t) {
           // Gestion des erreurs
       }
   });
   ```

### 5.3 Ajout d'un nouveau modèle de données

1. **Création de la classe modèle**
    - Créer une nouvelle classe Java dans le package `com.drogpulseai.models`
   ```java
   package com.drogpulseai.models;

   import com.google.gson.annotations.SerializedName;
   import java.io.Serializable;

   public class NouveauModele implements Serializable {
       @SerializedName("id")
       private int id;

       @SerializedName("nom")
       private String nom;

       // Autres propriétés...

       // Constructeur
       public NouveauModele(String nom) {
           this.nom = nom;
       }

       // Getters et Setters
       public int getId() { return id; }
       public void setId(int id) { this.id = id; }

       public String getNom() { return nom; }
       public void setNom(String nom) { this.nom = nom; }

       // Autres getters et setters...

       @Override
       public String toString() {
           return nom;
       }
   }
   ```

2. **Création de la table correspondante dans la base de données**
   ```sql
   CREATE TABLE `nouveau_modele` (
     `id` int(11) NOT NULL AUTO_INCREMENT,
     `nom` varchar(100) NOT NULL,
     -- Autres champs...
     `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
     `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
     PRIMARY KEY (`id`)
   );
   ```

3. **Création des endpoints API pour le nouveau modèle**
    - Créer un nouveau dossier dans l'API pour le nouveau modèle
    - Créer les fichiers CRUD (list.php, create.php, update.php, delete.php, etc.)

4. **Mise à jour de l'interface ApiService**
    - Ajouter les méthodes pour les nouveaux endpoints

## 6. Bonnes pratiques et standards de code

### 6.1 Standards de codage Java

- **Nommage**
    - Classes : PascalCase (ex: `ProductAdapter`)
    - Méthodes et variables : camelCase (ex: `getUserById`)
    - Constantes : SNAKE_CASE majuscule (ex: `MAX_RETRY_COUNT`)

- **Organisation du code**
    - Regrouper les déclarations de variables en haut des classes
    - Séparer les méthodes par des sauts de ligne
    - Commenter les sections complexes du code

- **JavaDoc**
    - Documenter toutes les classes et méthodes publiques
    - Inclure @param et @return dans les méthodes

### 6.2 Standards de codage PHP

- **Nommage**
    - Fonctions : camelCase (ex: `getUserData`)
    - Variables : camelCase (ex: `$userName`)

- **Sécurité**
    - Toujours utiliser des requêtes préparées (PDO)
    - Valider et nettoyer les entrées utilisateur
    - Éviter d'exposer les détails techniques dans les messages d'erreur

- **Organisation du code**
    - Inclure les commentaires d'en-tête dans chaque fichier
    - Structurer le code avec des espaces et des commentaires pour une meilleure lisibilité

### 6.3 Conventions de gestion des versions

- Utiliser Git pour le contrôle de version
- Format des messages de commit : `[Type] Description courte`
    - Types : `[Feature]`, `[Fix]`, `[Refactor]`, `[Doc]`, etc.
- Créer des branches pour les nouvelles fonctionnalités
- Faire des pull requests pour les fusions

## 7. Dépannage

### 7.1 Problèmes courants dans l'application Android

1. **Erreur de connexion à l'API**
    - Vérifier que l'URL de base dans `ApiClient.java` est correcte
    - Vérifier que l'appareil/émulateur a accès à Internet
    - Vérifier que le serveur API est en ligne
    - Examiner les logs avec `logcat` pour plus de détails

2. **Crash lors du chargement des images**
    - Vérifier que les permissions Internet sont déclarées dans le Manifest
    - Vérifier que les URL des images sont correctes
    - S'assurer que Glide est correctement configuré

3. **Problèmes de géolocalisation**
    - Vérifier que les permissions de localisation sont demandées et accordées
    - Vérifier que les services de localisation sont activés sur l'appareil
    - Examiner les logs pour les erreurs `LocationUtils`

### 7.2 Problèmes courants dans l'API PHP

1. **Erreur 500 (Internal Server Error)**
    - Vérifier les logs du serveur web (`/var/log/apache2/error.log`)
    - Activer temporairement l'affichage des erreurs PHP pour le débogage
   ```php
   ini_set('display_errors', 1);
   error_reporting(E_ALL);
   ```

2. **Réponse JSON invalide**
    - Vérifier qu'aucun echo ou print n'est exécuté avant `header()`
    - S'assurer que `Content-Type: application/json` est correctement défini
    - Utiliser la classe `Response` pour formater les réponses

3. **Problèmes d'upload de fichiers**
    - Vérifier que le dossier d'upload existe et a les permissions correctes
    - Vérifier les limites de taille dans `php.ini` (`upload_max_filesize`, `post_max_size`)
    - Examiner les logs PHP pour les erreurs d'upload

### 7.3 Problèmes courants de base de données

1. **Erreur de connexion**
    - Vérifier les informations de connexion dans `database.php`
    - S'assurer que le serveur MySQL est en cours d'exécution
    - Vérifier que l'utilisateur a les droits nécessaires

2. **Erreurs SQL**
    - Vérifier la syntaxe SQL dans les requêtes
    - S'assurer que les tables et colonnes existent
    - Examiner les logs MySQL pour les erreurs détaillées

## 8. Ressources et références

### 8.1 Documentation des technologies

- [Documentation Android](https://developer.android.com/docs)
- [Documentation Retrofit](https://square.github.io/retrofit/)
- [Documentation PHP](https://www.php.net/docs.php)
- [Documentation MySQL](https://dev.mysql.com/doc/)

### 8.2 Outils recommandés

- **Android Studio** - IDE pour le développement Android
- **Postman** - Test des API REST
- **phpMyAdmin** - Administration de la base de données MySQL
- **Visual Studio Code** - Éditeur pour le code PHP
- **Android Debug Bridge (adb)** - Débogage des applications Android

### 8.3 Contacts et support

Pour toute question ou assistance, contacter :
- Responsable technique : [aminkadira@gmail.com]
- Support développement : [support@drogpulseai.com]

---

© 2025 DrogPulseAI. Tous droits réservés.