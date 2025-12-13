# 📦 Registre Comptable v1.4.1 - Corrections et améliorations

Sortie: 2025-12-13

Résumé
- Corrections mineures et améliorations d'interface.
- Mise à jour d'infrastructure pour la publication (version, installeur).

Nouvelles fonctionnalités & améliorations
- Recherche avancée asynchrone : nouvelle vue "Recherche avancée" avec filtres par ART / PAR / LIG, sélection de mois et d'année, et recherche déclenchée automatiquement avec un délai de saisie (300 ms).
- Export Excel amélioré : les colonnes exportées ont été réordonnées (ART, PAR, LIG avant IMP) et la colonne `OV/CHEQ Type` a été supprimée pour simplifier l'export/import.
- Menu Base de données : style et visibilité des sous-menus améliorés pour une lecture correcte sur la barre supérieure.

Compatibilité et migration
- Migration de base de données fournie : un outil de migration a été utilisé pour supprimer des colonnes héritées (ex : `date_visa`, `ov_cheq_type`, etc.) et copier les données restantes dans une table `operations` compatible.
- Les DAO ont été rendus plus défensifs : le mapping des résultats vérifie l'existence des colonnes afin d'éviter des erreurs sur des bases migrées.
- Les requêtes de recherche exécutées en arrière-plan ouvrent désormais des connexions JDBC courtes (pour éviter les conflits de statements partagés).

Corrections de bugs
- Résolution d'une erreur liée à l'accès à des colonnes supprimées après migration.
- Correction d'un problème d'accès concurrent à la connexion SQLite lors de recherches asynchrones.
- Correction d'un chargement FXML (imports manquants) pour la nouvelle vue.

Infra / Packaging
- Version projet mise à jour en `1.4.1` (`pom.xml` et `src/main/resources/version.txt`).
- Fichiers d'installation mis à jour (`installer/launch4j.xml`, `installer/registre.iss`) pour pointer vers les artefacts 1.4.1.

Notes pour la publication
- Tag Git : `v1.4.1` - créé sur la branche `release-v1.4.0`.
- Construire le package :

```powershell
.\mvnw.cmd -DskipTests=true package
```

Remarques
- Les notes de la release 1.4.0 sont conservées dans `releases/v1.4.0` et n'ont pas été modifiées.
- Préconisations : vérifier visuellement l'UI (recherche avancée, label de résultat, export Excel) et exécuter le paquetage/installeur si besoin.

Merci — dites-moi si vous souhaitez que je génère un `RELEASE_NOTES.md` plus détaillé ou que je prépare les assets (jar/installeur).