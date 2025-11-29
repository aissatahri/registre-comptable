Set WshShell = CreateObject("WScript.Shell")
Dim fso, scriptPath, appDir, javaPath, cmd
Set fso = CreateObject("Scripting.FileSystemObject")
scriptPath = WScript.ScriptFullName
appDir = fso.GetParentFolderName(scriptPath)
javaPath = appDir & "\\runtime\\bin\\javaw.exe"
If fso.FileExists(javaPath) Then
    cmd = Chr(34) & javaPath & Chr(34) & " -jar " & Chr(34) & appDir & "\\registre-comptable-1.0.0-all.jar" & Chr(34)
Else
    cmd = "javaw -jar " & Chr(34) & appDir & "\\registre-comptable-1.0.0-all.jar" & Chr(34)
End If
WshShell.Run cmd, 0, False
