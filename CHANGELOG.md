# Changelog

## [1.3.0] - 2025-12-02
### Nettoyage de la base de données
- Suppression des colonnes obsolètes de la table `operations`:
  - `montant` (redondant avec `solde`)
  - `op` (colonne legacy)
  - `date_entree`, `date_rejet`
  - `decision`, `motif_rejet`, `date_reponse`, `contenu_reponse`
  - `mois`
- Simplification du schéma: 18 colonnes essentielles au lieu de 25+
- Migration automatique de la base de données existante
- Mise à jour des DAO (OperationDAO, RecapDAO) pour utiliser uniquement les colonnes actuelles

### Export Excel optimisé
- Réduction des colonnes exportées à 19 colonnes essentielles
- Suppression des colonnes non pertinentes de l'export
- Amélioration des performances d'export

### Code cleanup
- Suppression de toutes les méthodes de migration legacy dans Database.java
- Code simplifié et plus maintenable

## [1.2.0] - 2025-12-01
### Améliorations visuelles
- Remplacement des cercles par des PieCharts dans la vue récapitulative annuelle
- Ajout du segment "Solde fin" (bleu) dans les graphiques mensuels
- Agrandissement des PieCharts (250x250) pour une meilleure lisibilité
- Suppression des étiquettes sur les graphiques pour un rendu plus épuré

### Corrections et améliorations
- Correction de la lecture de version depuis version.txt dans "À propos"
- Mise à jour de la date à "Dec.25" dans l'écran de connexion et "À propos"
- Amélioration de l'affichage des valeurs sous chaque graphique mensuel

## [1.1.0] - 2025-11-29
- Reverted floating hamburger feature (restored top bar behavior)
- UI fixes: ensure top bar collapse/restore behaves correctly
- Small CSS and controller cleanups

## [1.0.0]
- Initial release
