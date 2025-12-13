; Inno Setup script for Registre Comptable
; Adjust paths and filenames as needed before compiling

; Helper to pass the correct SourceJar path at compile time
; Script is located in the `installer\` folder, so reference the jar one level up
#define SourceJar "..\\target\\registre-comptable-1.4.1.jar"

; If a native launcher was built with Launch4j, include it and use it for shortcuts
#if FileExists('launcher-1.4.1.exe')
#define HAS_NATIVE_LAUNCHER 1
#else
#define HAS_NATIVE_LAUNCHER 0
#endif

[Setup]
AppName=Registre Comptable
AppVersion=1.4.1
DefaultDirName={localappdata}\RegistreComptable
DefaultGroupName=Registre Comptable
DisableProgramGroupPage=no
OutputBaseFilename=RegistreComptable-1.4.1-setup
OutputDir=Output
Compression=lzma
SolidCompression=yes
; Installer icon file (relative to this script when compiling)
SetupIconFile=app.ico

; If you want a single EXE with runtime, include the runtime folder into installer (see [Files])

[Files]
; Main shaded JAR produced by Maven (adjust name if different)
Source: "{#SourceJar}"; DestDir: "{app}"; Flags: ignoreversion



; Include the application icon so it can be used for shortcuts
Source: "app.ico"; DestDir: "{app}"; Flags: ignoreversion
; Include the silent VBS launcher (preferred to avoid console flash). Legacy helpers removed in favor of native launcher.
Source: "launcher.vbs"; DestDir: "{app}"; Flags: ignoreversion
#if HAS_NATIVE_LAUNCHER
Source: "launcher-1.4.1.exe"; DestDir: "{app}"; Flags: ignoreversion
#endif

; Optionally include a bundled runtime if you created one with jlink/jre
; Place your runtime under project root `runtime\` before building installer
; Only include runtime files when the folder exists to avoid compile errors
#if DirExists('..\\runtime')
Source: "..\\runtime\\*"; DestDir: "{app}\\runtime"; Flags: recursesubdirs createallsubdirs overwritereadonly
#endif

; Include optional README or license (one level up)
Source: "..\\CHANGELOG.md"; DestDir: "{app}"; Flags: ignoreversion

[Icons]
; Shortcut: uses wscript to launch run.vbs (wscript hides the script window)
; Create shortcuts. If a native launcher exists, prefer it; otherwise use wscript + vbs
#if HAS_NATIVE_LAUNCHER
Name: "{group}\Registre Comptable"; Filename: "{app}\launcher-1.4.1.exe"; WorkingDir: "{app}"; IconFilename: "{app}\app.ico"
Name: "{commondesktop}\Registre Comptable"; Filename: "{app}\launcher-1.4.1.exe"; WorkingDir: "{app}"; IconFilename: "{app}\app.ico"; Tasks: desktopicon
#else
Name: "{group}\Registre Comptable"; Filename: "{sys}\wscript.exe"; Parameters: """{app}\launcher.vbs"""; WorkingDir: "{app}"; IconFilename: "{app}\app.ico"
Name: "{commondesktop}\Registre Comptable"; Filename: "{sys}\wscript.exe"; Parameters: """{app}\launcher.vbs"""; WorkingDir: "{app}"; IconFilename: "{app}\app.ico"; Tasks: desktopicon
#endif

[Run]
; Offer to run the app after installation using the launcher cmd (ShellExecute)
; Run the launcher after install; prefer native exe if present
#if HAS_NATIVE_LAUNCHER
Filename: "{app}\launcher-1.4.1.exe"; WorkingDir: "{app}"; Flags: nowait postinstall skipifsilent shellexec
#else
Filename: "{sys}\wscript.exe"; Parameters: """{app}\launcher.vbs"""; WorkingDir: "{app}"; Flags: nowait postinstall skipifsilent
#endif

[Tasks]
Name: "desktopicon"; Description: "Create a &desktop icon"; GroupDescription: "Additional icons:";



