# Release Notes - v1.4.0

**Date de release** : 6 décembre 2025

## 🎯 Nouvelles fonctionnalités

### 📊 Tableau de bord
- **Vue Dashboard complète** avec indicateurs de performance (KPIs)
  - Solde actuel avec code couleur (vert/rouge)
  - Recettes et dépenses du mois sélectionné
  - Sous-répartition EXP/INV des dépenses
  - Statistiques annuelles complètes
- **Graphique d'évolution** du solde mensuel sur l'année
- **Sélecteurs année/mois** pour naviguer dans l'historique
- **Alertes automatiques** :
  - ⚠ Solde négatif
  - ⚠ Déficit mensuel
  - ⚠ Déficit annuel
  - ℹ Solde faible (< 10 000 DH)
- **Affichage du solde initial** et balance annuelle (Recettes - Dépenses)
- **Sélection automatique** du dernier mois saisi au chargement

### 🔍 Filtres améliorés (Vue Registre)
- **Nouveau filtre par année** avec ComboBox déroulant
- **Mémorisation automatique** : sélection par défaut du dernier mois/année saisi
- **Préservation des filtres** lors des rechargements (ajout/modification/suppression)
- **Filtre mois étendu** : recherche par `date_emission` et `date_visa` en fallback

### 📝 Nouveaux champs ART/PAR/LIG
- Ajout des colonnes `art`, `par`, `lig` au schéma database
- Intégration dans le **formulaire d'édition** des opérations
- **Affichage dans la table** avec alignement centré
- Mise à jour automatique des migrations de schéma

## ✨ Améliorations UI/UX

### 🎨 Design et ergonomie
- **Dashboard responsive** avec ScrollPane pour éviter le scroll vertical excessif
- **Réduction du padding** dans toutes les vues (10px au lieu de 20px)
- **Hauteur du graphique limitée** à 260px pour meilleure mise en page
- **Pagination fixée à 13 lignes** dans le registre pour cohérence
- **Icônes d'alerte simplifiées** (⚠/ℹ au lieu des emoji composés)

### 🐛 Corrections de bugs
- **Fix rowFactory double-définition** : suppression du conflit entre coloration recettes et décisions
- **Correction filtre mois** : inclut maintenant `date_visa` comme fallback si `date_emission` absente
- **Rendu des icônes** : suppression des caractères � dans les alertes

## 📦 Contenu de la release

Ce dossier contient :
- `registre-comptable-1.4.0.jar` : Application complète (shaded JAR avec dépendances)
- `update4j.xml` : Configuration pour le système de mise à jour automatique
- `RELEASE_NOTES.md` : Ce fichier

## 🚀 Installation

1. **Prérequis** : Java 17 ou supérieur
2. **Lancement** : `java -jar registre-comptable-1.4.0.jar`

## 📋 Statistiques

- **24 fichiers modifiés**
- **1 328 lignes ajoutées**
- **150 lignes supprimées**
- **4 nouveaux fichiers** :
  - `DashboardController.java`
  - `DashboardDAO.java`
  - `DashboardStats.java`
  - `DesignationFileManager.java`

## 🔗 Liens utiles

- **Repository** : [aissatahri/registre-comptable](https://github.com/aissatahri/registre-comptable)
- **Tag** : `v1.4.0`
- **Branche** : `release-v1.4.0`
- **Commit** : `69a5fc0`

---

© A.Tahri - Décembre 2025
