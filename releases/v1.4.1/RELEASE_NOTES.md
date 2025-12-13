# Release Notes — v1.4.1 (2025-12-13)

Résumé
-------
Maintenance mineure et corrections trouvées après la mise en place des nouvelles fonctionnalités de la branche `release-v1.4.0`.

Principales modifications
------------------------
- Version bump: `1.4.1` (`pom.xml`, `src/main/resources/version.txt`).
- Installeur/packaging: `installer/launch4j.xml` et `installer/registre.iss` mis à jour pour pointer vers les artefacts `1.4.1`.
- Recherche avancée: nouvelle vue asynchrone (déclenchement après 300ms) avec filtres `ART` / `PAR` / `LIG`, sélection `mois` et `année`, et export Excel des résultats filtrés.
- Export Excel: réorganisation des en-têtes (ART, PAR, LIG avant IMP) et suppression de la colonne `OV/CHEQ Type`.
- Base de données: outil de migration pour supprimer des colonnes héritées et DAO defensifs (vérification d'existence des colonnes).
- Concurrence: les recherches en arrière-plan ouvrent maintenant des connexions JDBC courtes pour éviter des conflits de Statement.

Correctifs
---------
- Évitement d'une SQLException liée à des colonnes supprimées après migration.
- Correction d'un problème de partage de statement JDBC lors de recherches asynchrones.
- Correction d'un chargement FXML pour la nouvelle vue (import manquant).

Compatibilité / Migration
-------------------------
- Si vous mettez à jour depuis une version antérieure, gardez une sauvegarde de la base de données avant d'exécuter la migration fournie.
- Les DAO acceptent désormais des schémas où certaines colonnes héritées sont absentes ; cependant, vérifiez vos scripts d'import/export personnalisés.

Packaging & distribution
------------------------
- Artefact JAR attendu: `target/registre-comptable-1.4.1.jar`
- Launcher natif (Launch4j): `launcher-1.4.1.exe` (si généré)
- Installeur Inno Setup: `RegistreComptable-1.4.1-setup.exe` (après construction)

Création du package (exemple PowerShell)
```powershell
# Build jar
.\mvnw.cmd -DskipTests=true package

# (optionnel) Générer le launcher natif avec Launch4j et construire l'installeur
# - Construire launcher (Launch4j) -> produit launcher-1.4.1.exe
# - Compiler le script Inno Setup located at installer\registre.iss
```

Assets et tag Git
-----------------
- Tag créé: `v1.4.1` sur la branche `release-v1.4.0`.
- Les notes de la release 1.4.0 restent en `releases/v1.4.0` pour l'historique.

Remarques finales
-----------------
- Après build, vérifiez `target/` pour le JAR et testez l'UI (recherche avancée et export Excel).
- Dites-moi si vous voulez que je :
  - prépare et attache les assets dans `releases/v1.4.1/` (JAR + installeur),
  - ou crée une release GitHub via l'API et y attache les fichiers.

Merci.
