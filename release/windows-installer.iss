#ifndef AppVersion
  #define AppVersion "2026.10.31"
#endif
#ifndef OutputName
  #define OutputName "apsu-game-windows-x64"
#endif

[Setup]
AppId={{2E1238DE-5C1A-4500-9DF7-B193C45FE581}
AppName=As Águas de Apsu
AppVersion={#AppVersion}
AppPublisher=As Águas de Apsu
DefaultDirName={localappdata}\Programs\As Águas de Apsu
DefaultGroupName=As Águas de Apsu
UninstallDisplayIcon={app}\ApsuGame.exe
ArchitecturesAllowed=x64
ArchitecturesInstallIn64BitMode=x64
PrivilegesRequired=lowest
OutputDir=..\dist\installers
OutputBaseFilename={#OutputName}
Compression=lzma2
SolidCompression=yes
WizardStyle=modern

[Files]
Source: "..\dist\app-image\ApsuGame\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
Name: "{autoprograms}\As Águas de Apsu"; Filename: "{app}\ApsuGame.exe"
Name: "{autodesktop}\As Águas de Apsu"; Filename: "{app}\ApsuGame.exe"

[Run]
Filename: "{app}\ApsuGame.exe"; Description: "Jogar agora"; Flags: postinstall nowait skipifsilent
