<#
  Prepara os dados do OSRM (roteamento por ruas) usados pro cálculo de frete
  por km e pra sugestão de rota do entregador.

  Passo ÚNICO e manual — baixa o extrato do Brasil inteiro (vários GB) e
  processa com osrm-extract/partition/customize, o que pode levar de dezenas
  de minutos a algumas horas dependendo da máquina. Rode uma vez antes de
  subir o serviço "osrm" do docker-compose.yml. Rodar de novo é seguro (pula
  o download/processamento se os arquivos já existirem) — use -Force pra
  refazer do zero.

  Uso:
    .\infra\osrm\prepare-data.ps1
    .\infra\osrm\prepare-data.ps1 -Force
#>
param(
    [switch]$Force
)

$ErrorActionPreference = 'Stop'

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot '..\..')
$dataDir = Join-Path $repoRoot 'osrm-data'
$pbfName = 'brazil-latest.osm.pbf'
$osrmName = 'brazil-latest.osrm'
$downloadUrl = 'https://download.geofabrik.de/south-america/brazil-latest.osm.pbf'

if (-not (Test-Path $dataDir)) {
    New-Item -ItemType Directory -Path $dataDir | Out-Null
}

$pbfPath = Join-Path $dataDir $pbfName
$osrmPath = Join-Path $dataDir $osrmName

if ($Force) {
    Write-Host "-Force: removendo dados processados existentes..."
    Get-ChildItem $dataDir -Filter "$osrmName*" -ErrorAction SilentlyContinue | Remove-Item -Force
}

if ((Test-Path $osrmPath) -and -not $Force) {
    Write-Host "Dados já processados em $osrmPath — nada a fazer (use -Force pra refazer)."
    exit 0
}

if (-not (Test-Path $pbfPath)) {
    Write-Host "Baixando $downloadUrl (pode levar bastante tempo, é um arquivo de vários GB)..."
    Invoke-WebRequest -Uri $downloadUrl -OutFile $pbfPath
} else {
    Write-Host "$pbfName já baixado, pulando download."
}

$dockerDataMount = "${dataDir}:/data"

Write-Host "Rodando osrm-extract (perfil car — o mais próximo de veículo motorizado disponível pronto)..."
docker run --rm -v $dockerDataMount osrm/osrm-backend osrm-extract -p /opt/car.lua "/data/$pbfName"

Write-Host "Rodando osrm-partition..."
docker run --rm -v $dockerDataMount osrm/osrm-backend osrm-partition "/data/$($pbfName -replace '\.osm\.pbf$','.osrm')"

Write-Host "Rodando osrm-customize..."
docker run --rm -v $dockerDataMount osrm/osrm-backend osrm-customize "/data/$($pbfName -replace '\.osm\.pbf$','.osrm')"

Write-Host "Pronto. Suba o serviço com: docker compose up -d osrm"
