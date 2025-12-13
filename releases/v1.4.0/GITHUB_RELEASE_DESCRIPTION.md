# 📊 Registre Comptable v1.4.0 - Dashboard & Filtres Avancés

## 🎯 Nouveautés principales

### 📊 Tableau de bord interactif
- **KPIs en temps réel** : Solde actuel, recettes/dépenses du mois avec code couleur
- **Graphique d'évolution** : Visualisation mensuelle du solde sur l'année
- **Sélecteurs année/mois** : Navigation facile dans l'historique comptable
- **Alertes intelligentes** : Détection automatique des déficits et soldes faibles
- **Statistiques annuelles** : Vue complète avec solde initial et balance annuelle
- **Sélection automatique** du dernier mois saisi pour un accès rapide

### 🔍 Filtres améliorés
- **Nouveau filtre par année** dans la vue Registre
- **Mémorisation intelligente** : Sélection automatique du dernier mois/année au chargement
- **Préservation des filtres** lors des modifications/suppressions
- **Filtre mois étendu** avec fallback sur date_visa

### 📝 Nouveaux champs ART/PAR/LIG
- Colonnes `art`, `par`, `lig` ajoutées au schéma
- Intégration complète dans le formulaire et la table
- Alignement centré pour meilleure lisibilité

## ✨ Améliorations UI/UX

- ✅ Dashboard responsive avec ScrollPane
- ✅ Réduction des paddings pour optimiser l'espace
- ✅ Pagination fixée à 13 lignes
- ✅ Hauteur du graphique limitée à 260px
- ✅ Icônes d'alerte simplifiées (⚠/ℹ)

## 🐛 Corrections

- Fix conflit rowFactory (coloration recettes/décisions)
- Correction filtre mois avec date_visa en fallback
- Amélioration du rendu des icônes dans les alertes

## 📦 Installation

**Prérequis** : Java 17 ou supérieur

```bash
java -jar registre-comptable-1.4.0.jar
```

## 📊 Statistiques de la release

- **24 fichiers modifiés**
- **+1 328 / -150 lignes**
- **4 nouveaux fichiers** (Dashboard, DAO, Stats, FileManager)

## 🔗 Fichiers

- **registre-comptable-1.4.0.jar** : Application complète (shaded JAR)
- **update4j.xml** : Configuration système de mise à jour
- **RELEASE_NOTES.md** : Notes détaillées

---

**Release complète** | [Changelog](https://github.com/aissatahri/registre-comptable/blob/main/CHANGELOG.md) | **Tag**: `v1.4.0` | **Commit**: `69a5fc0`

© A.Tahri - Décembre 2025
