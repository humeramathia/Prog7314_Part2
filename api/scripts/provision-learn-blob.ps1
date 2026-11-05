# Creates the Azure Blob account + public `learn` container and uploads
# the Learn JPEGs. Run after: az login
#
#   powershell -File api\scripts\provision-learn-blob.ps1
#
# Optional: $env:LEARN_MEDIA_DIR = path to the 12 JPEG files.

$ErrorActionPreference = "Stop"

$rg = "prog7314-rg"
$location = "brazilsouth"
$account = "st10276384media"
$container = "learn"
$repoRoot = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$mediaDir = $env:LEARN_MEDIA_DIR
if (-not $mediaDir) {
  $candidates = @(
    (Join-Path $repoRoot "docs\learn-media"),
    "C:\Users\Humera Mathia\.cursor\projects\c-Users-Humera-Mathia-Documents-BCAD3-2nd-Sem-PROG7314-AMBER-Prog7314-Part2\assets"
  )
  $mediaDir = $candidates | Where-Object { Test-Path $_ } | Select-Object -First 1
}

Write-Host "Resource group $rg"
Write-Host "Storage account $account"
Write-Host "Media directory $mediaDir"

az account show --output none
az group create --name $rg --location $location --output none
az storage account create `
  --name $account `
  --resource-group $rg `
  --location $location `
  --sku Standard_LRS `
  --kind StorageV2 `
  --allow-blob-public-access true `
  --output none

az storage container create `
  --name $container `
  --account-name $account `
  --auth-mode login `
  --public-access blob `
  --output none

$files = @(
  "football-techniques.jpg", "football-training.jpg",
  "cricket-techniques.jpg", "cricket-training.jpg",
  "tennis-techniques.jpg", "tennis-training.jpg",
  "basketball-techniques.jpg", "basketball-training.jpg",
  "swimming-techniques.jpg", "swimming-training.jpg",
  "athletics-techniques.jpg", "athletics-training.jpg"
)

if (-not $mediaDir) {
  throw "No media directory found. Set LEARN_MEDIA_DIR to the folder with the Learn JPEGs."
}

foreach ($name in $files) {
  $path = Join-Path $mediaDir $name
  if (-not (Test-Path -LiteralPath $path)) {
    Write-Warning "Skipping missing $name"
    continue
  }
  az storage blob upload `
    --account-name $account `
    --container-name $container `
    --name $name `
    --file $path `
    --overwrite true `
    --auth-mode login `
    --content-type "image/jpeg" `
    --output none
  Write-Host "Uploaded $name"
}

Write-Host "Public base URL: https://$account.blob.core.windows.net/$container/"
Write-Host "Example: https://$account.blob.core.windows.net/$container/football-techniques.jpg"
