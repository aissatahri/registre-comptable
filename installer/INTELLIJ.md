Aligner l'Artifact IntelliJ avec le JAR Maven
===============================================

Contexte
--------
IntelliJ peut produire un artefact dans `out/artifacts/...` en utilisant sa configuration d'Artifact. Maven (utilisé par VS Code et par CI) produit le JAR versionné dans `target/`. Pour les releases et les installateurs, utilisez toujours le JAR dans `target/`.

Options pour aligner
--------------------

1) Construire avec Maven depuis IntelliJ (recommandé)

- Ouvrir `View -> Tool Windows -> Maven`.
- Exécuter la phase `package` (ou créer une Run Configuration Maven pour `clean package`).
- Le JAR versionné sera dans `target\` (même que `./mvnw.cmd -DskipTests package`).

2) Modifier l'Artifact IntelliJ pour utiliser le JAR Maven

- File -> Project Structure -> Artifacts.
- Sélectionner l'artifact (p.ex. `registre-comptable:jar`) et changer l'`Output directory` vers `target`.
- Si l'Artifact inclut une construction interne (Build on make), supprimer cette étape et laisser IntelliJ copier le JAR existant depuis `target`.

3) Alternative rapide: toujours prendre le JAR depuis `target` pour l'installateur

- Quand tu crées l'installateur (Launch4j / Inno), pointe le launcher/installeur vers `target\registre-comptable-<version>-all.jar`.

Commandes utiles (PowerShell):

```powershell
.\mvnw.cmd -DskipTests clean package
```

Notes
-----
- Ne pas committer `out/` ni `out/artifacts` dans le dépôt; préfère utiliser `target/` ou les artefacts CI pour les releases.
- Si tu veux que je modifie un fichier `.idea/artifacts` committé, fournis-le et je l'édite pour pointer vers `target`.
