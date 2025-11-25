# Registre Comptable (JavaFX)

Application de gestion comptable en JavaFX pour suivre les opérations (registre), les paiements, et produire des récapitulatifs avec stockage local SQLite et import/export Excel.

## Fonctionnalités
- Registre des opérations avec colonnes complètes (OP, OV/CHEQ, IMP, Nature, Budg, Montant, Dates, Décision, etc.).
- Recherche et filtres par mois et par nature, statistiques de synthèse en bas de page.
- Statistiques du menu dynamiques: nombre d’opérations et total des recettes se mettent à jour immédiatement après import/ajout/suppression.
- Contrôle d’affichage des colonnes via un menu "Colonnes" (Registre, Par Mois, Paiements).
- Case à cocher centrée pour la colonne de sélection et case d’en-tête pour cocher/décocher toutes les lignes visibles.
- Vue "Par Mois": résumé (compte, total, moyenne), tableau des opérations du mois, export Excel.
- Vue "Paiements": liste, totaux par année (INV/EXP/TOTAL), actions d’édition/suppression, mise à jour des recettes du menu.
- Import/Export Excel des opérations (Apache POI), avec messages d’erreur contextualisés si le fichier est ouvert ailleurs.
- Base de données SQLite initialisée automatiquement (tables `operations` et `paiements`).

## Prérequis
- JDK 17+
- Maven (le wrapper `mvnw`/`mvnw.cmd` est fourni)
- Windows ou Linux/MacOS

## Lancer l’application
Sur Windows:
```
./mvnw.cmd javafx:run
```

Sur Linux/MacOS:
```
./mvnw javafx:run
```

Alternative (profil `run` via `exec-maven-plugin`):
```
./mvnw.cmd -P run exec:java
```

## Construction et distribution
- Construire le jar:
```
./mvnw.cmd -DskipTests clean package
```

- Générer une image distribuable (jlink) avec le profil `dist`:
```
./mvnw.cmd -P dist javafx:jlink
```
Un zip de l’image sera produit (configuré dans le `pom.xml`).

## Configuration de la base
- Par défaut: `jdbc:sqlite:registre.db` à la racine du projet.
- Vous pouvez changer l’URL via la propriété de JVM:
```
java -Ddb.url="jdbc:sqlite:C:/chemin/mon_registre.db" -jar target/registre-comptable-1.0.0.jar
```

## Structure du projet
- Code: `src/main/java/com/app/registre/`
  - `Main`: point d’entrée de l’application
  - `controller`: contrôleurs JavaFX (Registre, Paiement, Mois, Menu, Dialogues)
  - `dao`: accès SQLite (Database, OperationDAO, PaiementDAO, RecapDAO)
  - `model`: modèles `Operation`, `Paiement`
  - `util`: utilitaires (Excel)
- Ressources: `src/main/resources/`
  - `view`: FXML des vues
  - `images`: icônes
  - `style.css`: styles

## Raccourcis
- `Ctrl+B`: afficher/masquer la barre latérale du menu.

## Notes
- Les fichiers FXML sont compatibles avec les dépendances JavaFX configurées dans le `pom.xml`.
- Lors de l’import Excel, fermez le classeur dans Excel avant d’importer pour éviter les erreurs d’accès.

## Licence
Ce projet est destiné à un usage interne. Adaptez la licence selon vos besoins.

