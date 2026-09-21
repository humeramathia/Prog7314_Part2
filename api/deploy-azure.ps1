# Deploys this folder to Azure App Service. Run after: az login
# powershell -File api/deploy-azure.ps1

$ErrorActionPreference = "Stop"
$apiDir = $PSScriptRoot
$envFile = Join-Path $apiDir ".env"
$rg = "prog7314-rg"
$location = "brazilsouth"
$plan = "sportsphere-plan"
$app = "sportsphere-st10276384"

Write-Host "Deploying $apiDir -> https://$app.azurewebsites.net"

az group create --name $rg --location $location --output none
az appservice plan create --name $plan --resource-group $rg --location $location --sku B1 --is-linux --output none
az webapp create --name $app --resource-group $rg --plan $plan --runtime "NODE:20-lts" --output none
az webapp config set --name $app --resource-group $rg --startup-file "node src/server.js" --output none

$settings = @(
  "NODE_ENV=production",
  "SKIP_AUTH=false",
  "FIREBASE_PROJECT_ID=sportsphere-b3c84",
  "SCM_DO_BUILD_DURING_DEPLOYMENT=true"
)

if (Test-Path $envFile) {
  Get-Content $envFile | ForEach-Object {
    if ($_ -match "^\s*COSMOS_ENDPOINT=(.+)$") { $settings += "COSMOS_ENDPOINT=$($Matches[1].Trim())" }
    if ($_ -match "^\s*COSMOS_KEY=(.+)$") { $settings += "COSMOS_KEY=$($Matches[1].Trim())" }
    if ($_ -match "^\s*COSMOS_DATABASE=(.+)$") { $settings += "COSMOS_DATABASE=$($Matches[1].Trim())" }
  }
}

$sa = Join-Path $apiDir "firebase-service-account.json"
if (Test-Path $sa) {
  $json = ((Get-Content $sa -Raw) -replace "`r", "" -replace "`n", "").Trim()
  $settings += "FIREBASE_SERVICE_ACCOUNT=$json"
} else {
  Write-Warning "api/firebase-service-account.json is missing. Hosted API will reject tokens until you add it and re-run this script."
}

az webapp config appsettings set --name $app --resource-group $rg --settings $settings --output none

$zip = Join-Path $env:TEMP "sportsphere-api.zip"
if (Test-Path $zip) { Remove-Item $zip -Force }
$stage = Join-Path $env:TEMP "sportsphere-api-pack"
if (Test-Path $stage) { Remove-Item $stage -Recurse -Force }
New-Item $stage -ItemType Directory | Out-Null
Copy-Item (Join-Path $apiDir "package.json") $stage
Copy-Item (Join-Path $apiDir "jest.config.js") $stage -ErrorAction SilentlyContinue
Copy-Item (Join-Path $apiDir "src") (Join-Path $stage "src") -Recurse
Compress-Archive -Path (Join-Path $stage "*") -DestinationPath $zip
az webapp deployment source config-zip --resource-group $rg --name $app --src $zip
Write-Host "Hosted API: https://$app.azurewebsites.net/health"
