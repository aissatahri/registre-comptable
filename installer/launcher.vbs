Set WshShell = CreateObject("WScript.Shell")
Set fso = CreateObject("Scripting.FileSystemObject")

sFull = WScript.ScriptFullName
sDir = Left(sFull, Len(sFull) - Len(WScript.ScriptName))

' Find the best matching JAR in the install folder
bestJar = ""
Set fld = fso.GetFolder(sDir)
For Each f in fld.Files
    fn = LCase(f.Name)
    If Left(fn, 18) = "registre-comptable-" And Right(fn, 4) = ".jar" Then
        If bestJar = "" Then
            bestJar = f.Name
        Else
            If f.Name > bestJar Then bestJar = f.Name
        End If
    End If
Next

If bestJar <> "" Then
    sJar = sDir & bestJar
Else
    sJar = sDir & "registre-comptable-1.1.0-all.jar"
End If

' Prefer bundled javaw
sRuntime = sDir & "runtime\bin\javaw.exe"

Function Which(exe)
  On Error Resume Next
  Dim res
  res = ""
  Set oExec = WshShell.Exec("cmd /c where " & exe)
  Do While oExec.Status = 0
    WScript.Sleep 50
  Loop
  If oExec.ExitCode = 0 Then
    res = Trim(oExec.StdOut.ReadAll())
  End If
  Which = res
End Function

javaPath = ""
If fso.FileExists(sRuntime) Then
  javaPath = sRuntime
Else
  jw = Which("javaw.exe")
  If jw <> "" Then javaPath = Split(jw, vbCrLf)(0)
End If

If javaPath = "" Then
  MsgBox "Java non trouvé. Installez une JRE/JDK ou incluez un runtime dans le dossier 'runtime'.", vbExclamation, "Java introuvable"
  WScript.Quit 1
End If

cmd = Chr(34) & javaPath & Chr(34) & " -jar " & Chr(34) & sJar & Chr(34)

' Run without console (javaw) and do not wait
WshShell.Run cmd, 0, False

WScript.Quit 0
