# Changelog

## [1.4.0] - 2025-12-06
### Nouvelles fonctionnalités
- **Tableau de bord** : Ajout d'une nouvelle vue dashboard avec KPIs (solde actuel, recettes/dépenses du mois, statistiques annuelles)
  - Graphique d'évolution du solde mensuel sur l'année
  - Sélecteurs année/mois pour naviguer dans l'historique
  - Alertes automatiques (solde négatif, déficit mensuel/annuel, solde faible)
  - Affichage du solde initial et balance annuelle
  - Par défaut, sélectionne automatiquement le dernier mois saisi
- **Vue Registre** : Filtres améliorés avec mémorisation par défaut
  - Ajout d'un filtre par année
  - Sélection automatique du dernier mois/année saisi au chargement
  - Préservation des filtres année/mois lors des rechargements
- **Champs ART/PAR/LIG** : Nouveaux champs dans le formulaire opération et table
  - Ajout des colonnes `art`, `par`, `lig` au schéma database
  - Intégration dans le dialog d'édition
  - Affichage dans la table avec alignement centré

### Améliorations UI/UX
- Dashboard responsive avec ScrollPane et layout optimisé
- Réduction du padding/espacement dans les vues pour éviter le scroll vertical excessif
- Icônes d'alerte simplifiées (⚠/ℹ au lieu des emoji composés)
- Hauteur du graphique limitée à 260px pour meilleure mise en page
- Pagination fixée à 13 lignes dans le registre

### Corrections
- Fix rowFactory double-définition dans RegistreController (suppression conflit coloration recettes/décisions)
- Correction filtre mois pour inclure date_visa en fallback

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
