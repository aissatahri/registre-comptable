Building the native launcher and installer

Steps to produce a native Windows launcher (launcher.exe) and an Inno Setup installer.

1) Build the application JAR
- From project root, run in PowerShell:
  .\mvnw.cmd -DskipTests package
- The shaded JAR will be created at `target\registre-comptable-1.1.0-all.jar`.

2) Build the native launcher using Launch4j
- Install Launch4j (https://launch4j.sourceforge.net/) and ensure `launch4j.exe` is available, or set the `LAUNCH4J` environment variable to point to the executable.
- From the project root run (PowerShell):
  .\installer\build-launcher.ps1
- This script will run Launch4j using `installer\launch4j.xml` and produce `launcher.exe` in the `installer\` directory.
- You can edit `installer\launch4j.xml` to customize icon, bundled JRE path, or JVM options.

3) Build the Inno Setup installer
- If you want the installer to include the native launcher, copy the generated `launcher.exe` into the `installer` folder (it may already be written there by the Launch4j run).
- Open `installer\registre.iss` in Inno Setup and compile, or run from command line using ISCC.exe:
  "C:\Program Files (x86)\Inno Setup 6\ISCC.exe" installer\registre.iss
- The script will detect whether `installer\launcher.exe` is present and, if so, include it and create shortcuts that use it. Otherwise it will fallback to using `wscript.exe` + `launcher.vbs`.

Notes
- The installer already includes `app.ico` and a silent helper script `launcher.vbs` as a compatibility fallback. Prefer including the native `launcher-1.1.0.exe` produced by Launch4j to avoid relying on scripts.
- The application stores the chosen DB path in `%LOCALAPPDATA%\\RegistreComptable\\config.properties` under `db.path`.
- On first run the app will ask the user where to create/open the DB and can persist the choice.
