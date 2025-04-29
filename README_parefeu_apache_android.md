
# 📡 Accès à un serveur Apache local depuis un appareil Android

Lorsque vous développez une application Android utilisant un serveur local (via XAMPP ou WAMP), il est crucial de permettre les connexions depuis d'autres appareils du réseau, comme un smartphone Android physique. Cette section explique **comment configurer le pare-feu Windows et Apache** pour autoriser ces connexions.

---

## 🔐 Autoriser Apache (httpd.exe) dans le pare-feu Windows

Le pare-feu de Windows peut bloquer les connexions entrantes vers votre serveur Apache. Pour autoriser les requêtes :

### Étapes :

1. Ouvrir les paramètres du pare-feu :
   - Menu Démarrer → tapez `Pare-feu Windows Defender` → **Entrée**.

2. Cliquez sur **"Autoriser une application via le pare-feu"**.

3. Cliquez sur **"Modifier les paramètres"**, puis **"Autoriser une autre application..."** si `httpd.exe` n’apparaît pas.

4. Cliquez sur **Parcourir** et sélectionnez le fichier :
   - XAMPP : `C:\xampp\apache\bin\httpd.exe`
   - WAMP : `C:\wamp64\bin\apache\apache2.x.x\bin\httpd.exe`

5. Cliquez sur **Ajouter**, puis cochez **Réseau Privé** et **Public**.

6. Cliquez sur OK et redémarrez Apache via XAMPP ou WAMP.

> 🛡️ Tester ensuite en accédant à `http://<votre_ip_local>` depuis le navigateur de votre téléphone.

---

## 🌐 Configuration d'Apache pour écouter sur toutes les interfaces

Par défaut, Apache écoute uniquement sur `127.0.0.1`, ce qui empêche les connexions extérieures.

### Étapes :

1. Ouvrez le fichier `httpd.conf` :
   - XAMPP : `C:\xampp\apache\conf\httpd.conf`
   - WAMP : `C:\wamp64\bin\apache\apache2.x.x\conf\httpd.conf`

2. Recherchez la ligne :
   ```apache
   Listen 127.0.0.1:80
   ```

3. Remplacez-la par :
   ```apache
   Listen 0.0.0.0:80
   ```

   > Cela permet à Apache d'écouter sur toutes les interfaces réseau.

4. Sauvegardez et redémarrez Apache via XAMPP/WAMP.

---

## 📱 Tester depuis un téléphone Android

1. Assurez-vous que le **PC et le smartphone** sont **connectés au même réseau Wi-Fi**.

2. Sur le téléphone, ouvrez un navigateur et allez à :
   ```
   http://192.168.1.xx
   ```
   Remplacez par l'adresse IP de votre PC (trouvée avec `ipconfig` sur Windows).

3. Vous devriez voir la page d'accueil Apache ou celle de votre API locale.

---

## ✅ Bonnes pratiques

- Utilisez des **variables d’environnement ou des classes `Config`** pour séparer les URL locales des URL de production.
- Exemple de configuration :
  ```java
  public class Config {
      public static final boolean DEBUG = true;
      public static final String BASE_URL = DEBUG
          ? "http://192.168.1.10/api/"
          : "https://votre-domaine.com/api/";
  }
  ```

---

## 📚 Références

- [Microsoft Support – Autoriser une application via le pare-feu](https://support.microsoft.com/fr-fr/windows/autoriser-une-application-%C3%A0-travers-le-pare-feu-3cfcefdb-0bcc-4b8a-9e2a-480d9b44e7d3)
- [Apache HTTP Server Documentation – Listen Directive](https://httpd.apache.org/docs/2.4/bind.html)
