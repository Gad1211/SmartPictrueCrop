param(
    [string]$JdkHome = "D:\JetBrains\JDK21"
)

$ErrorActionPreference = "Stop"

if (-not (Test-Path $JdkHome)) {
    throw "JDK path not found: $JdkHome"
}

$env:JAVA_HOME = $JdkHome
$env:Path = "$env:JAVA_HOME\bin;$env:Path"

Write-Host "Using JAVA_HOME=$env:JAVA_HOME"
java -version

mvn clean package

$jarName = "ai-pic-handler-1.0.0-SNAPSHOT-all.jar"
$targetPath = Join-Path $PSScriptRoot "target"
$jarPath = Join-Path $targetPath $jarName
if (-not (Test-Path $jarPath)) {
    throw "Packable jar not found: $jarPath"
}

$modelConfigSource = Join-Path $PSScriptRoot "modelConfig.json"
if (Test-Path $modelConfigSource) {
    Copy-Item $modelConfigSource (Join-Path $targetPath "modelConfig.json") -Force
    Write-Host "Included modelConfig.json into package input."
} else {
    Write-Warning "modelConfig.json not found in project root, app will fallback to bundled default config."
}

$defaultModelSource = Join-Path $PSScriptRoot "models\YOLO11"
if (Test-Path $defaultModelSource) {
    $defaultModelTarget = Join-Path $targetPath "models\YOLO11"
    if (Test-Path $defaultModelTarget) {
        Remove-Item -Recurse -Force $defaultModelTarget
    }
    New-Item -Path (Split-Path $defaultModelTarget -Parent) -ItemType Directory -Force | Out-Null
    Copy-Item $defaultModelSource $defaultModelTarget -Recurse -Force
    Write-Host "Included default YOLO11 model folder into package input."
} else {
    Write-Warning "Default model folder models\YOLO11 not found; portable package will not contain bundled YOLO11 files."
}

$distPath = Join-Path $PSScriptRoot "dist"
if (Test-Path $distPath) {
    Remove-Item -Recurse -Force $distPath
}
New-Item -Path $distPath -ItemType Directory | Out-Null

jpackage `
  --type app-image `
  --name "PicCrop" `
  --input $targetPath `
  --main-jar $jarName `
  --main-class com.aipichandler.app.MainApp `
  --dest $distPath `
  --vendor "PicCrop"

$appImagePath = Join-Path $distPath "PicCrop"
$launcherPath = Join-Path $appImagePath "PicCrop.exe"
if (-not (Test-Path $launcherPath)) {
    throw "Portable launcher not generated: $launcherPath"
}

$portableZipPath = Join-Path $distPath "PicCrop-portable.zip"
if (Test-Path $portableZipPath) {
    Remove-Item $portableZipPath -Force
}

Compress-Archive -Path $appImagePath -DestinationPath $portableZipPath -CompressionLevel Optimal

Write-Host "Portable folder: $appImagePath"
Write-Host "Portable zip: $portableZipPath"
