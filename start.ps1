#Requires -Version 5.1
<#
.SYNOPSIS
  Inicializa o projeto Oia a Conta.
  Sempre para os containers que estiverem rodando e reinicializa o projeto inteiro do zero.
.DESCRIPTION
  Uso: .\start.ps1 [-KeepData] [-NoBuild] [-Force]

  -KeepData   Mantem o volume do PostgreSQL (nao apaga os dados)
  -NoBuild    Sobe os containers sem rebuildar as imagens
  -Force      Forca rebuild completo (sem cache) das imagens
#>
param(
    [switch]$KeepData,
    [switch]$NoBuild,
    [switch]$Force
)

$ErrorActionPreference = 'Continue'
$scriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $scriptRoot

function Write-Step  { param($msg) Write-Host "`n==> $msg" -ForegroundColor Cyan }
function Write-Ok    { param($msg) Write-Host "    OK  $msg" -ForegroundColor Green }
function Write-Warn  { param($msg) Write-Host "    !!  $msg" -ForegroundColor Yellow }
function Write-Fatal { param($msg) Write-Host "`nERRO: $msg" -ForegroundColor Red; exit 1 }

function Get-ComposeCommand {
    if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
        return $null
    }

    & docker compose version 2>$null | Out-Null
    if ($LASTEXITCODE -eq 0) {
        return 'docker-compose-plugin'
    }

    if (Get-Command docker-compose -ErrorAction SilentlyContinue) {
        return 'docker-compose'
    }

    return $null
}

function Invoke-Compose {
    param([Parameter(ValueFromRemainingArguments = $true)][string[]]$Arguments)

    if ($script:ComposeCommand -eq 'docker-compose') {
        & docker-compose @Arguments
    } else {
        & docker compose @Arguments
    }
}

function Start-DockerDesktopIfNeeded {
    docker info 2>&1 | Out-Null
    if ($LASTEXITCODE -eq 0) {
        Write-Ok "Docker esta rodando"
        return $true
    }

    if ($IsWindows -or $PSVersionTable.PSEdition -eq 'Desktop') {
        $dockerDesktop = Join-Path $env:ProgramFiles 'Docker\Docker\Docker Desktop.exe'
        if (Test-Path $dockerDesktop) {
            Write-Warn "Docker Desktop nao respondeu. Tentando iniciar automaticamente..."
            Start-Process $dockerDesktop -WindowStyle Hidden

            for ($i = 0; $i -lt 12; $i++) {
                Start-Sleep -Seconds 5
                docker info 2>&1 | Out-Null
                if ($LASTEXITCODE -eq 0) {
                    Write-Ok "Docker Desktop iniciado com sucesso"
                    return $true
                }
            }
        }
    }

    return $false
}

# --- Pre-requisitos ---
Write-Step "Verificando pre-requisitos"

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    Write-Fatal "Docker nao encontrado. Instale o Docker Desktop e tente novamente."
}

$script:ComposeCommand = Get-ComposeCommand
if (-not $script:ComposeCommand) {
    Write-Fatal "Docker Compose nao encontrado. Instale o plugin 'docker compose' ou o comando 'docker-compose' e tente novamente."
}

if (-not (Start-DockerDesktopIfNeeded)) {
    Write-Fatal "Docker Desktop nao esta rodando. Abra o Docker Desktop manualmente e tente novamente."
}

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

# --- Para e reinicializa tudo (sempre) ---
# Independente do projeto ja estar rodando ou nao, paramos qualquer container
# existente e subimos o projeto inteiro do zero, para garantir que todos os
# servicos fiquem com o estado/config mais recente.
$runningContainers = @(Invoke-Compose ps --quiet 2>&1 | Where-Object { $_ -match '\S' })
if ($runningContainers.Count -gt 0) {
    Write-Step "Projeto ja esta em execucao ($($runningContainers.Count) containers) - parando tudo para reinicializar"
} else {
    Write-Step "Parando containers existentes (se houver)"
}
Invoke-Compose down --remove-orphans 2>&1 | Out-Null
Write-Ok "Containers parados"

if ($KeepData) {
    Write-Warn "-KeepData ativo: volume do PostgreSQL preservado"
} else {
    Write-Step "Removendo volumes"
    Invoke-Compose down --volumes 2>&1 | Out-Null
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
        "evolution-api",
        "api-gateway",
        "frontend"
    )

    $buildArgs = if ($Force) { @("--no-cache") } else { @() }

    foreach ($svc in $buildServices) {
        Write-Host "    Buildando $svc..." -ForegroundColor DarkCyan
        Invoke-Compose build @buildArgs $svc 2>&1 | Out-Null
        if ($LASTEXITCODE -ne 0) {
            Write-Fatal "Falha ao construir '$svc'. Verifique: docker compose logs $svc"
        }
        Write-Ok "$svc OK"
    }
}

# --- Sobe todos os containers ---
Write-Step "Iniciando todos os containers"
Invoke-Compose up --detach 2>&1 | Out-Null
if ($LASTEXITCODE -ne 0) {
    Write-Fatal "Falha ao iniciar os containers."
}

# Garante os bancos necessarios caso o volume ja existisse sem eles
Write-Step "Verificando bancos de dados"
Start-Sleep -Seconds 5

$requiredDbs = @('db_catalog', 'db_table', 'db_order', 'db_billing', 'db_whatsapp', 'db_evolution')
foreach ($db in $requiredDbs) {
    $exists = docker exec oia-postgres psql -U oiaconta -d db_auth -tAc "SELECT 1 FROM pg_database WHERE datname='$db'" 2>$null
    if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($exists)) {
        docker exec oia-postgres psql -U oiaconta -d db_auth -c "CREATE DATABASE $db;" 2>&1 | Out-Null
        docker exec oia-postgres psql -U oiaconta -d db_auth -c "GRANT ALL PRIVILEGES ON DATABASE $db TO oiaconta;" 2>&1 | Out-Null
        Write-Ok "$db criado"
    } else {
        Write-Ok "$db OK"
    }
}

# Os servicos abaixo podem ter subido antes dos bancos serem criados e
# ficado em crash loop (Exited) - reinicia todos para garantir que conectem
Write-Step "Reiniciando servicos dependentes de banco"
Invoke-Compose restart catalog-service table-service order-service whatsapp-service billing-service 2>&1 | Out-Null

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
    @{ name = "Evolution API";        url = "http://localhost:8087";                 container = "oia-evolution"    },
    @{ name = "API Gateway";          url = "http://localhost:8090/actuator/health"; container = "oia-gateway"      },
    @{ name = "Frontend (Nginx)";     url = "http://localhost";                      container = "oia-frontend"     }
)

$maxWait  = 180
$interval = 5

function Test-ServicoSaudavel {
    param($svc)
    if ($svc.url) {
        try {
            $resp = Invoke-WebRequest -Uri $svc.url -UseBasicParsing -TimeoutSec 3 -ErrorAction Stop
            return ($resp.StatusCode -lt 500)
        } catch {
            # Alguns endpoints (ex: Evolution API sem apikey) respondem 401/403 - conta como "no ar"
            if ($_.Exception.Response -and [int]$_.Exception.Response.StatusCode -lt 500) { return $true }
            return $false
        }
    }
    $status = docker inspect --format='{{.State.Health.Status}}' $svc.container 2>$null
    return ($status -eq "healthy")
}

$pendentes = [System.Collections.Generic.List[object]]::new()

foreach ($svc in $checks) {
    $elapsed = 0
    $ok = $false

    while ($elapsed -lt $maxWait) {
        if (Test-ServicoSaudavel $svc) { $ok = $true; break }
        Start-Sleep -Seconds $interval
        $elapsed += $interval
        Write-Host "    Aguardando $($svc.name)... ($elapsed s)" -ForegroundColor DarkGray
    }

    if ($ok) {
        Write-Ok "$($svc.name) esta pronto"
    } else {
        Write-Warn "$($svc.name) nao respondeu em ${maxWait}s"
        $pendentes.Add($svc)
    }
}

# Alguns servicos podem ter falhado ao subir por timeout de dependencia
# (ex: gateway/billing/frontend esperando o auth-service ficar saudavel
# durante um cold start pesado) - tenta recriar so o que ficou pendente.
if ($pendentes.Count -gt 0) {
    Write-Step "Tentando reiniciar servicos que nao ficaram saudaveis"
    Invoke-Compose up --detach 2>&1 | Out-Null

    foreach ($svc in $pendentes) {
        $elapsed = 0
        $ok = $false
        while ($elapsed -lt 60) {
            if (Test-ServicoSaudavel $svc) { $ok = $true; break }
            Start-Sleep -Seconds $interval
            $elapsed += $interval
        }
        if ($ok) {
            Write-Ok "$($svc.name) esta pronto"
        } else {
            Write-Warn "$($svc.name) continua indisponivel - verifique: Invoke-Compose logs $($svc.container)"
        }
    }
}

# --- Status final ---
Write-Step "Status dos containers"
Invoke-Compose ps

Write-Host ""
Write-Host "======================================================" -ForegroundColor Green
Write-Host "  Oia a Conta esta no ar!" -ForegroundColor Green
Write-Host "======================================================" -ForegroundColor Green
Write-Host "  Frontend      http://localhost" -ForegroundColor Green
Write-Host "  API Gateway   http://localhost:8090" -ForegroundColor Green
Write-Host "  Eureka        http://localhost:8761" -ForegroundColor Green
Write-Host "  Evolution API http://localhost:8087" -ForegroundColor Green
Write-Host "======================================================" -ForegroundColor Green
