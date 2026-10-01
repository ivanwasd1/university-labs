$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
[Console]::OutputEncoding = [System.Text.UTF8Encoding]::new()

$projectRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$releaseRoot = Join-Path $projectRoot 'release'
$appImage = Join-Path $releaseRoot 'ColorModelsLab'
$portableZip = Join-Path $releaseRoot 'ColorModelsLab-portable.zip'
$packageInput = Join-Path $projectRoot 'target\package-input'
$sourceStage = Join-Path $projectRoot 'target\source-package\ColorModelsLab'
$sourceZip = Join-Path $releaseRoot 'ColorModelsLab-source.zip'

& (Join-Path $projectRoot 'build.bat')
if ($LASTEXITCODE -ne 0) {
    throw "Maven build failed with exit code $LASTEXITCODE"
}

$jpackageCommand = Get-Command jpackage -ErrorAction SilentlyContinue
if ($null -eq $jpackageCommand) {
    throw 'jpackage was not found. Install JDK 17 or newer.'
}

New-Item -ItemType Directory -Force -Path $releaseRoot | Out-Null
if (Test-Path -LiteralPath $appImage) {
    Remove-Item -LiteralPath $appImage -Recurse -Force
}
if (Test-Path -LiteralPath $portableZip) {
    Remove-Item -LiteralPath $portableZip -Force
}
if (Test-Path -LiteralPath $packageInput) {
    Remove-Item -LiteralPath $packageInput -Recurse -Force
}
New-Item -ItemType Directory -Force -Path $packageInput | Out-Null
Copy-Item -LiteralPath (Join-Path $projectRoot 'target\ColorModelsLab.jar') -Destination $packageInput

Write-Host 'Creating the self-contained Windows application...'
& $jpackageCommand.Source `
    --type app-image `
    --name ColorModelsLab `
    --dest $releaseRoot `
    --input $packageInput `
    --main-jar ColorModelsLab.jar `
    --main-class ru.university.colormodels.App `
    --add-modules java.desktop `
    --app-version 1.0 `
    --vendor 'University project'
if ($LASTEXITCODE -ne 0) {
    throw "jpackage failed with exit code $LASTEXITCODE"
}
Copy-Item -LiteralPath (Join-Path $projectRoot 'README.md') -Destination $appImage
Copy-Item -LiteralPath (Join-Path $projectRoot 'docs') -Destination $appImage -Recurse
Compress-Archive -LiteralPath $appImage -DestinationPath $portableZip -CompressionLevel Optimal

if (Test-Path -LiteralPath (Split-Path -Parent $sourceStage)) {
    Remove-Item -LiteralPath (Split-Path -Parent $sourceStage) -Recurse -Force
}
New-Item -ItemType Directory -Force -Path $sourceStage | Out-Null
New-Item -ItemType Directory -Force -Path (Join-Path $sourceStage 'src\main'), (Join-Path $sourceStage 'src\test') | Out-Null
Copy-Item -LiteralPath (Join-Path $projectRoot 'src\main\java') -Destination (Join-Path $sourceStage 'src\main') -Recurse
Copy-Item -LiteralPath (Join-Path $projectRoot 'src\test\java') -Destination (Join-Path $sourceStage 'src\test') -Recurse
foreach ($sourceSet in @('main', 'test')) {
    $resources = Join-Path $projectRoot "src\$sourceSet\resources"
    if (Test-Path -LiteralPath $resources) {
        Copy-Item -LiteralPath $resources -Destination (Join-Path $sourceStage "src\$sourceSet") -Recurse
    }
}
Copy-Item -LiteralPath (Join-Path $projectRoot 'scripts') -Destination $sourceStage -Recurse
Copy-Item -LiteralPath (Join-Path $projectRoot 'docs') -Destination $sourceStage -Recurse
Copy-Item -LiteralPath (Join-Path $projectRoot 'README.md') -Destination $sourceStage
Copy-Item -LiteralPath (Join-Path $projectRoot '.gitignore') -Destination $sourceStage
Copy-Item -LiteralPath (Join-Path $projectRoot 'pom.xml') -Destination $sourceStage
Copy-Item -LiteralPath (Join-Path $projectRoot 'build.bat') -Destination $sourceStage
Copy-Item -LiteralPath (Join-Path $projectRoot 'test.bat') -Destination $sourceStage
Copy-Item -LiteralPath (Join-Path $projectRoot 'run.bat') -Destination $sourceStage
Copy-Item -LiteralPath (Join-Path $projectRoot 'package.bat') -Destination $sourceStage
if (Test-Path -LiteralPath $sourceZip) {
    Remove-Item -LiteralPath $sourceZip -Force
}
Compress-Archive -LiteralPath $sourceStage -DestinationPath $sourceZip -CompressionLevel Optimal

Write-Host "Portable package: $portableZip"
Write-Host "Source package: $sourceZip"
