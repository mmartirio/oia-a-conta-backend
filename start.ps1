#Requires -Version 5.1
<#
.SYNOPSIS
  Inicializa o projeto Oia a Conta.
  Se os containers ja estiverem rodando, reinicia-os automaticamente.
.DESCRIPTION
  Uso: .\start.ps1 [-KeepData] [-NoBuild] [-Force]

  -KeepData   Mantem o volume do PostgreSQL (nao apaga os dados)
  -NoBuild    Sobe os containers sem rebuildar as imagens
  -Force      Forca rebuild completo mesmo que os containers ja estejam rodando
#>
param(
    [switch]$KeepData,
    [switch]$NoBuild,
    [switch]$Force
)

$ErrorActionPreference = 'Continue'

function Write-Step  { param($msg) Write-Host "`n==> $msg" -ForegroundColor Cyan }
function Write-Ok    { param($msg) Write-Host "    OK  $msg" -ForegroundColor Green }
function Write-Warn  { param($msg) Write-Host "    !!  $msg" -ForegroundColor Yellow }
function Write-Fatal { param($msg) Write-Host "`nERRO: $msg" -ForegroundColor Red; exit 1 }

# --- Pre-requisitos ---
Write-Step "Verificando pre-requisitos"

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    Write-Fatal "Docker nao encontrado. Instale o Docker Desktop e tente novamente."
}

docker info 2>&1 | Out-Null
if ($LASTEXITCODE -ne 0) {
    Write-Fatal "Docker Desktop nao esta rodando. Inicie o Docker Desktop e tente novamente."
}
Write-Ok "Docker esta rodando"

# --- Verifica .env ---
if (-not (Test-Path ".env")) {
    Write-Warn ".env nao encontrado - criando com valores padrao"
    $envContent = @'
JWT_SECRET=oia_a_conta_super_secret_key_2024_must_be_at_least_32_chars
VITE_GOOGLE_CLIENT_ID=
ALLOWED_ORIGINS=http://localhost,http://localhost:80,http://localhost:3000
GMAIL_USERNAME=martiriotecnologia@gmail.com
GMAIL_APP_PASSWORD=
MP_ACCESS_TOKEN=
MP_WEBHOOK_SECRET=
EVOLUTION_API_KEY=oia_evolution_key_2024
EVOLUTION_INSTANCE_NAME=oiaaconta
EVOLUTION_WEBHOOK_SECRET=
'@
    $envContent | Set-Content -Path ".env" -Encoding utf8
    Write-Ok ".env criado com valores padrao (edite antes de usar em producao)"
} else {
    Write-Ok ".env encontrado"
}

# --- Detecta se o projeto ja esta rodando ---
$runningContainers = docker compose ps -q 2>&1 | Where-Object { $_ -match '\S' }
$projetoRodando = $runningContainers.Count -gt 0

if ($projetoRodando -and -not $Force) {
    Write-Step "Projeto ja esta em execucao ($($runningContainers.Count) containers) - reiniciando"
    docker compose restart 2>&1 | Out-Null
    if ($LASTEXITCODE -ne 0) {
        Write-Fatal "Falha ao reiniciar. Tente: .\start.ps1 -Force"
    }
    Write-Ok "Containers reiniciados"
} else {
    # --- Para e limpa ---
    Write-Step "Parando containers existentes"
    docker compose down --remove-orphans 2>&1 | Out-Null
    Write-Ok "Containers parados"

    if ($KeepData) {
        Write-Warn "-KeepData ativo: volume do PostgreSQL preservado"
    } else {
        Write-Step "Removendo volumes"
        docker compose down -v 2>&1 | Out-Null
        Write-Ok "Volumes removidos"
    }

    if ($Force) {
        Write-Step "Limpando build cache do Docker (-Force)"
        docker builder prune -f 2>&1 | Out-Null
        Write-Ok "Build cache limpo"
    }

    # --- Build ---
    if ($NoBuild) {
        Write-Warn "-NoBuild ativo: pulando rebuild das imagens"
    } else {
        Write-Step "Construindo imagens"

        $buildServices = @(
            "discovery-service",
            "auth-service",
            "catalog-service",
            "table-service",
            "notification-service",
            "order-service",
            "whatsapp-service",
            "billing-service",
            "api-gateway",
            "frontend"
        )

        $buildArgs = if ($Force) { @("--no-cache") } else { @() }

        foreach ($svc in $buildServices) {
            Write-Host "    Buildando $svc..." -ForegroundColor DarkCyan
            docker compose build @buildArgs $svc 2>&1 | Out-Null
            if ($LASTEXITCODE -ne 0) {
                Write-Fatal "Falha ao construir '$svc'. Verifique: docker compose logs $svc"
            }
            Write-Ok "$svc OK"
        }
    }

    # --- Sobe ---
    Write-Step "Iniciando todos os containers"
    docker compose up -d 2>&1 | Out-Null
    if ($LASTEXITCODE -ne 0) {
        Write-Fatal "Falha ao iniciar os containers."
    }

    # Garante db_billing caso o volume ja existisse sem ele
    Write-Step "Verificando bancos de dados"
    Start-Sleep -Seconds 5
    docker exec oia-postgres psql -U oiaconta -d db_auth -c "SELECT 1 FROM pg_database WHERE datname='db_billing'" 2>&1 | Out-Null
    if ($LASTEXITCODE -ne 0) {
        docker exec oia-postgres psql -U oiaconta -d db_auth -c "CREATE DATABASE db_billing;" 2>&1 | Out-Null
        docker exec oia-postgres psql -U oiaconta -d db_auth -c "GRANT ALL PRIVILEGES ON DATABASE db_billing TO oiaconta;" 2>&1 | Out-Null
        Write-Ok "db_billing criado"
        docker compose restart billing-service 2>&1 | Out-Null
    } else {
        Write-Ok "Bancos de dados OK"
    }
}

# --- Aguarda health checks ---
Write-Step "Aguardando servicos ficarem saudaveis"

$checks = @(
    @{ name = "PostgreSQL";           url = $null;                                   container = "oia-postgres"      },
    @{ name = "Redis";                url = $null;                                   container = "oia-redis"         },
    @{ name = "Discovery (Eureka)";   url = "http://localhost:8761/actuator/health"; container = "oia-discovery"    },
    @{ name = "Auth Service";         url = "http://localhost:8081/actuator/health"; container = "oia-auth"         },
    @{ name = "Catalog Service";      url = "http://localhost:8082/actuator/health"; container = "oia-catalog"      },
    @{ name = "Table Service";        url = "http://localhost:8083/actuator/health"; container = "oia-table"        },
    @{ name = "Notification Service"; url = "http://localhost:8085/actuator/health"; container = "oia-notification" },
    @{ name = "Order Service";        url = "http://localhost:8084/actuator/health"; container = "oia-order"        },
    @{ name = "WhatsApp Service";     url = "http://localhost:8086/actuator/health"; container = "oia-whatsapp"     },
    @{ name = "Billing Service";      url = "http://localhost:8088/actuator/health"; container = "oia-billing"      },
    @{ name = "API Gateway";          url = "http://localhost:8090/actuator/health"; container = "oia-gateway"      },
    @{ name = "Frontend (Nginx)";     url = "http://localhost";                      container = "oia-frontend"     }
)

$maxWait  = 180
$interval = 5

foreach ($svc in $checks) {
    $elapsed = 0
    $ok = $false

    while ($elapsed -lt $maxWait) {
        if ($svc.url) {
            try {
                $resp = Invoke-WebRequest -Uri $svc.url -UseBasicParsing -TimeoutSec 3 -ErrorAction Stop
                if ($resp.StatusCode -lt 400) { $ok = $true; break }
            } catch { }
        } else {
            $status = docker inspect --format='{{.State.Health.Status}}' $svc.container 2>$null
            if ($status -eq "healthy") { $ok = $true; break }
        }

        Start-Sleep -Seconds $interval
        $elapsed += $interval
        Write-Host "    Aguardando $($svc.name)... ($elapsed s)" -ForegroundColor DarkGray
    }

    if ($ok) {
        Write-Ok "$($svc.name) esta pronto"
    } else {
        Write-Warn "$($svc.name) nao respondeu em ${maxWait}s - verifique: docker compose logs $($svc.container)"
    }
}

# --- Status final ---
Write-Step "Status dos containers"
docker compose ps

Write-Host ""
Write-Host "======================================================" -ForegroundColor Green
Write-Host "  Oia a Conta esta no ar!" -ForegroundColor Green
Write-Host "======================================================" -ForegroundColor Green
Write-Host "  Frontend      http://localhost" -ForegroundColor Green
Write-Host "  API Gateway   http://localhost:8090" -ForegroundColor Green
Write-Host "  Eureka        http://localhost:8761" -ForegroundColor Green
Write-Host "  Evolution API http://localhost:8087" -ForegroundColor Green
Write-Host "======================================================" -ForegroundColor Green
