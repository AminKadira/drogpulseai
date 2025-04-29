
## 🌐 Connexion à un serveur local (Apache) depuis un appareil Android

Si vous testez l'application Android sur un **appareil réel** et que vous utilisez un serveur Apache local (via XAMPP ou WAMP), suivez ces étapes pour permettre la communication :

### 1. Modifier l’adresse du serveur dans le code

Remplacez toute URL locale de type `http://localhost` ou `http://127.0.0.1` par l’**adresse IP locale** de votre PC, par exemple :

```java
public class Config {
    public static final String BASE_URL = "http://192.168.1.10/api/";
}
```

> Remplacez `192.168.1.10` par l'adresse IP de votre machine (obtenue via `ipconfig` ou `ifconfig`).

---

### 2. Autoriser Apache (httpd.exe) dans le pare-feu Windows

- Allez dans :  
  `Panneau de configuration > Système et sécurité > Pare-feu Windows Defender`
- Cliquez sur **"Autoriser une application via le pare-feu"**.
- Cherchez ou ajoutez manuellement `httpd.exe` (généralement dans `C:\xampp\apache\bin\httpd.exe`).
- Cochez **Privé** et **Public**.
- Redémarrez Apache.

---

### 3. Configurer Apache pour écouter sur toutes les interfaces

Dans le fichier `httpd.conf` :

```apache
# Au lieu de :
Listen 127.0.0.1:80

# Utilisez :
Listen 0.0.0.0:80
```

Chemin du fichier :

- XAMPP : `C:\xampp\apache\conf\httpd.conf`
- WAMP : `C:\wamp64\bin\apache\apache2.x.x\conf\httpd.conf`

Puis redémarrez Apache.

---

### 4. Vérifier la connexion depuis le téléphone

- Connectez votre téléphone au **même réseau Wi-Fi** que votre PC.
- Ouvrez un navigateur sur le téléphone et allez à :
  ```
  http://192.168.1.10
  ```
  Vous devriez voir la page d’accueil Apache ou votre application.

---

### ✅ Astuce de test

Ajoutez un switch dans votre code pour utiliser l’URL réseau locale en mode debug :

```java
public class Config {
    public static final boolean DEBUG = true;
    public static final String BASE_URL = DEBUG
        ? "http://192.168.1.10/api/"
        : "https://votre-domaine.com/api/";
}
```
