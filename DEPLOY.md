# Déploiement et démarrage — RegistreComptable

Ce document explique comment builder, lancer et déployer l'application sur Windows, ainsi que le comportement de la base SQLite et les scripts fournis.

## Résumé
- L'application utilise SQLite. Par défaut le fichier est `registre.db` dans le répertoire de travail ou l'emplacement défini par la propriété système Java `-Ddb.url`.
- Un dialogue au démarrage permet de créer/ouvrir une base et de mémoriser le chemin dans `%LOCALAPPDATA%\RegistreComptable\config.properties`.
- Les scripts `start.ps1` et `start.bat` fournis lancent l'application en utilisant `%LOCALAPPDATA%\RegistreComptable\registre.db`.
- L'application propose aussi un menu **Changer de base...** pour ouvrir/créer une DB et la recharger à chaud.

---

## Prérequis
- JDK 17+ (ex : Zulu OpenJDK 22 recommandé dans les scripts)
- JavaFX SDK (ex : 21.0.2) — modules `javafx.controls`, `javafx.fxml`, `javafx.graphics`.
- Maven wrapper (`mvnw.cmd`) inclus pour builder.

## Builder (dev)
Depuis la racine du projet :

```powershell
.\mvnw.cmd -DskipTests package
```

Le build produit les classes dans `target/classes` et les dépendances Maven sont amenées via le repo local. Pour production pensez à créer un jar exécutable (fat-jar) ou utiliser `jpackage`.

## Scripts fournis
- `start.ps1` : script PowerShell (Windows) qui crée `%LOCALAPPDATA%\RegistreComptable` si besoin et lance l'app en passant `-Ddb.url` pointant vers `%LOCALAPPDATA%`.
- `start.bat` : même fonction pour CMD.

Avant d'exécuter les scripts, éditez les chemins `JAVA` et `JAVAFX` en haut des scripts si vos installations diffèrent.

Exemple d'utilisation (PowerShell) :

```powershell
.\start.ps1
```

Ces scripts appellent :
- `-Ddb.url="jdbc:sqlite:C:\Users\<user>\AppData\Local\RegistreComptable\registre.db"`
- Ajustez le `classpath` dans le script si vous créez un fat-jar ou changez l'organisation des fichiers.

## Démarrage manuel en développement
Si vous préférez démarrer depuis votre JVM installée :

```powershell
& "C:\Program Files\Zulu\zulu-22\bin\java.exe" `
  --module-path "C:\javafx-sdk-21.0.2\lib" `
  --add-modules javafx.controls,javafx.fxml,javafx.graphics `
  -Ddb.url="jdbc:sqlite:D:\chemin\vers\registre.db" `
  -cp "target\classes;target\lib\*" `
  com.app.registre.Main
```

Adaptez le `--module-path` et le `-cp` selon votre environnement et packaging.

## Comportement de la base de données
- Par défaut la valeur est `jdbc:sqlite:registre.db` (fichier dans le working directory).
- Vous pouvez forcer un chemin absolu via `-Ddb.url="jdbc:sqlite:C:/chemin/registre.db"`.
- Au premier lancement (si aucun `config.properties` n'est trouvé), l'application propose :
  - Créer une nouvelle base (Save Dialog)
  - Ouvrir une base existante (Open Dialog)
  - Utiliser l'emplacement par défaut
- Si vous choisissez « Se souvenir de ce choix », le chemin est enregistré dans `%LOCALAPPDATA%\RegistreComptable\config.properties` sous la clé `db.path`.
- Les migrations sont automatiques : avant une migration destructive, une copie de sauvegarde est créée nommée `registre.db.bak.YYYYMMDDHHmmss` dans le même dossier.

### Changer la base à chaud
- Menu : **Changer de base...** dans la sidebar. Permet de créer ou ouvrir un `.db` et de le charger immédiatement.
- L'application ferme les fenêtres secondaires et réinitialise le singleton `Database` pour basculer sur la nouvelle DB sans redémarrage. Certaines vues très anciennes peuvent nécessiter une fermeture/re-ouverture manuelle pour éviter des références obsolètes.

## Sauvegarde / Restauration
- Sauvegardes automatiques lors de migration : `registre.db.bak.<timestamp>`.
- Sauvegardes manuelles : copiez le fichier `.db` vers un emplacement sûr.
- Pour restaurer : remplacez `registre.db` par une copie de sauvegarde.

## Packaging pour distribution (recommandé)
Option recommandée : `jpackage` pour créer un exécutable Windows (.exe/.msi) incluant un runtime minimal via `jlink`.

Étapes (exemple simplifié) :
1. Construire le jar : `.\mvnw.cmd -DskipTests package`
2. Créer une image runtime (jlink) incluant JavaFX jmods (ou utilisez un runtime JDK complet compatible).
3. Lancer `jpackage` :

```powershell
jpackage --name RegistreComptable --app-version 1.0.0 `
  --input target --main-jar registre-comptable-1.0.0.jar --main-class com.app.registre.Main `
  --runtime-image path\to\runtime-image `
  --win-dir-chooser --win-per-user-install --icon path\to\icon.ico
```

### Option : EXE vs MSI

Le script `package-jpackage.ps1` fourni avec le projet accepte maintenant un paramètre `-PACKAGE_TYPE` permettant de choisir le type d'installateur généré par `jpackage`. Par défaut le script produit un installateur Windows au format `.exe` (option `exe`). Si vous préférez un MSI vous pouvez exécuter le script avec `-PACKAGE_TYPE msi`.

Exemples :

```powershell
# Utilise le type par défaut (exe)
./package-jpackage.ps1

# Forcer un MSI
./package-jpackage.ps1 -PACKAGE_TYPE msi
```

Remarques:
- `jpackage` et `jlink` nécessitent le JDK (distribution qui propose `jmods` pour JavaFX ou vous devez fournir les jmods javafx).
- L'installeur peut créer un dossier dans `%LOCALAPPDATA%` et ajouter un raccourci qui lance le binaire avec l'option `-Ddb.url` si vous voulez forcer l'emplacement par défaut.

## Bonnes pratiques et recommandations
- Pour une application desktop mono‑utilisateur, SQLite est approprié. Pour usage multi‑utilisateurs simultané sur réseau, migrez vers une base client-serveur (Postgres/MySQL).
- Mettez en place une sauvegarde régulière du `.db` (Task Scheduler, script).
- Pour la sécurité, remplacez le hash SHA‑256 des mots de passe par bcrypt/Argon2 pour la production.

## Dépannage rapide
- Si l'application ne peut pas écrire la DB : vérifiez les permissions du dossier et l'emplacement du fichier.
- Pour forcer un chemin différent sans utiliser les scripts : lancer Java avec `-Ddb.url="jdbc:sqlite:C:/autre/chemin/registre.db"`.
- Si vous rencontrez des erreurs après un changement de DB : fermer les fenêtres secondaires et les rouvrir ou redémarrer l'application.

---

Si vous voulez, je peux ajouter :
- Un petit `DEPLOY.md` en français plus détaillé (screenshots),
- Un script `package-jpackage.ps1` prêt à l'emploi (requiert que vous fournissiez chemins JDK/JavaFX),
- Migration de la gestion des mots de passe vers bcrypt/Argon2.

Indiquez ce que vous préférez et je l'ajoute.
