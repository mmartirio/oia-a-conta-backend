#!/usr/bin/env bash
#
# Inicializa o projeto Oia a Conta.
# Sempre para os containers que estiverem rodando e reinicializa o projeto inteiro do zero.
#
# Uso: ./start.sh [--keep-data] [--no-build] [--force]
#
#   --keep-data   Mantem o volume do PostgreSQL (nao apaga os dados)
#   --no-build    Sobe os containers sem rebuildar as imagens
#   --force       Forca rebuild completo (sem cache) das imagens

set -u

KEEP_DATA=0
NO_BUILD=0
FORCE=0

for arg in "$@"; do
    case "$arg" in
        --keep-data) KEEP_DATA=1 ;;
        --no-build)  NO_BUILD=1 ;;
        --force)     FORCE=1 ;;
        *) echo "Argumento desconhecido: $arg" >&2; exit 1 ;;
    esac
done

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

write_step()  { printf "\n\033[36m==> %s\033[0m\n" "$1"; }
write_ok()    { printf "    \033[32mOK  %s\033[0m\n" "$1"; }
write_warn()  { printf "    \033[33m!!  %s\033[0m\n" "$1"; }
write_fatal() { printf "\n\033[31mERRO: %s\033[0m\n" "$1"; exit 1; }

# --- Pre-requisitos ---
write_step "Verificando pre-requisitos"

if ! command -v docker >/dev/null 2>&1; then
    write_fatal "Docker nao encontrado. Instale o Docker e tente novamente."
fi

COMPOSE_CMD=""
if docker compose version >/dev/null 2>&1; then
    COMPOSE_CMD="docker compose"
elif command -v docker-compose >/dev/null 2>&1; then
    COMPOSE_CMD="docker-compose"
else
    write_fatal "Docker Compose nao encontrado. Instale o plugin 'docker compose' ou o comando 'docker-compose' e tente novamente."
fi

compose() { $COMPOSE_CMD "$@"; }

start_docker_if_needed() {
    if docker info >/dev/null 2>&1; then
        write_ok "Docker esta rodando"
        return 0
    fi

    if [[ "$(uname -s)" == "Darwin" ]] && [[ -d "/Applications/Docker.app" ]]; then
        write_warn "Docker Desktop nao respondeu. Tentando iniciar automaticamente..."
        open -a Docker

        for _ in $(seq 1 12); do
            sleep 5
            if docker info >/dev/null 2>&1; then
                write_ok "Docker Desktop iniciado com sucesso"
                return 0
            fi
        done
    fi

    return 1
}

if ! start_docker_if_needed; then
    write_fatal "Docker nao esta rodando. Inicie o Docker manualmente e tente novamente."
fi

# --- Verifica .env ---
if [[ ! -f ".env" ]]; then
    write_warn ".env nao encontrado - criando com valores padrao"
    cat > .env <<'EOF'
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
EOF
    write_ok ".env criado com valores padrao (edite antes de usar em producao)"
else
    write_ok ".env encontrado"
fi

# --- Para e reinicializa tudo (sempre) ---
# Independente do projeto ja estar rodando ou nao, paramos qualquer container
# existente e subimos o projeto inteiro do zero, para garantir que todos os
# servicos fiquem com o estado/config mais recente.
running_count=$(compose ps --quiet 2>/dev/null | grep -c '\S' || true)
if [[ "$running_count" -gt 0 ]]; then
    write_step "Projeto ja esta em execucao ($running_count containers) - parando tudo para reinicializar"
else
    write_step "Parando containers existentes (se houver)"
fi
compose down --remove-orphans >/dev/null 2>&1
write_ok "Containers parados"

if [[ "$KEEP_DATA" -eq 1 ]]; then
    write_warn "--keep-data ativo: volume do PostgreSQL preservado"
else
    write_step "Removendo volumes"
    compose down --volumes >/dev/null 2>&1
    write_ok "Volumes removidos"
fi

if [[ "$FORCE" -eq 1 ]]; then
    write_step "Limpando build cache do Docker (--force)"
    docker builder prune -f >/dev/null 2>&1
    write_ok "Build cache limpo"
fi

# --- Build ---
if [[ "$NO_BUILD" -eq 1 ]]; then
    write_warn "--no-build ativo: pulando rebuild das imagens"
else
    write_step "Construindo imagens"

    build_services=(
        discovery-service
        auth-service
        catalog-service
        table-service
        notification-service
        order-service
        whatsapp-service
        billing-service
        evolution-api
        api-gateway
        frontend
    )

    build_args=()
    if [[ "$FORCE" -eq 1 ]]; then
        build_args+=(--no-cache)
    fi

    for svc in "${build_services[@]}"; do
        echo "    Buildando $svc..."
        if ! compose build "${build_args[@]}" "$svc" >/dev/null 2>&1; then
            write_fatal "Falha ao construir '$svc'. Verifique: docker compose logs $svc"
        fi
        write_ok "$svc OK"
    done
fi

# --- Sobe todos os containers ---
write_step "Iniciando todos os containers"
if ! compose up --detach >/dev/null 2>&1; then
    write_fatal "Falha ao iniciar os containers."
fi

# Garante os bancos necessarios caso o volume ja existisse sem eles
# (ex: primeira inicializacao num volume novo - o Postgres so cria sozinho
# o banco do POSTGRES_DB (db_auth); os outros vem do init-db.sql, que so
# roda automaticamente numa inicializacao "a frio" do volume).
write_step "Aguardando PostgreSQL ficar pronto"

pg_ready=0
elapsed=0
while [[ "$elapsed" -lt 60 ]]; do
    status=$(docker inspect --format='{{.State.Health.Status}}' oia-postgres 2>/dev/null)
    if [[ "$status" == "healthy" ]]; then
        pg_ready=1
        break
    fi
    sleep 3
    elapsed=$((elapsed + 3))
done

if [[ "$pg_ready" -ne 1 ]]; then
    write_fatal "PostgreSQL nao ficou pronto em 60s. Verifique: docker compose logs postgres"
fi
write_ok "PostgreSQL pronto"

write_step "Verificando bancos de dados"

required_dbs=(db_catalog db_table db_order db_billing db_whatsapp db_evolution)
db_falhou=0
for db in "${required_dbs[@]}"; do
    exists=$(docker exec oia-postgres psql -U oiaconta -d db_auth -tAc "SELECT 1 FROM pg_database WHERE datname='$db'" 2>/dev/null)
    if [[ -z "${exists// }" ]]; then
        if docker exec oia-postgres psql -U oiaconta -d db_auth -c "CREATE DATABASE $db;" >/dev/null 2>&1; then
            docker exec oia-postgres psql -U oiaconta -d db_auth -c "GRANT ALL PRIVILEGES ON DATABASE $db TO oiaconta;" >/dev/null 2>&1
            write_ok "$db criado"
        else
            write_warn "Falha ao criar $db - verifique: docker compose logs postgres"
            db_falhou=1
        fi
    else
        write_ok "$db OK"
    fi
done

if [[ "$db_falhou" -eq 1 ]]; then
    write_fatal "Um ou mais bancos nao puderam ser criados. Corrija e rode o script novamente."
fi

# Os servicos abaixo podem ter subido antes dos bancos serem criados e
# ficado em crash loop (Exited) - reinicia todos para garantir que conectem
write_step "Reiniciando servicos dependentes de banco"
compose restart catalog-service table-service order-service whatsapp-service billing-service >/dev/null 2>&1

# --- Aguarda health checks ---
write_step "Aguardando servicos ficarem saudaveis"

# name|url|container
checks=(
    "PostgreSQL||oia-postgres"
    "Redis||oia-redis"
    "Discovery (Eureka)|http://localhost:8761/actuator/health|oia-discovery"
    "Auth Service|http://localhost:8081/actuator/health|oia-auth"
    "Catalog Service|http://localhost:8082/actuator/health|oia-catalog"
    "Table Service|http://localhost:8083/actuator/health|oia-table"
    "Notification Service|http://localhost:8085/actuator/health|oia-notification"
    "Order Service|http://localhost:8084/actuator/health|oia-order"
    "WhatsApp Service|http://localhost:8086/actuator/health|oia-whatsapp"
    "Billing Service|http://localhost:8088/actuator/health|oia-billing"
    "Evolution API|http://localhost:8087|oia-evolution"
    "API Gateway|http://localhost:8090/actuator/health|oia-gateway"
    "Frontend (Nginx)|http://localhost|oia-frontend"
)

max_wait=180
interval=5

test_servico_saudavel() {
    local url="$1" container="$2"
    if [[ -n "$url" ]]; then
        local code
        code=$(curl -s -o /dev/null -w '%{http_code}' --max-time 3 "$url")
        # Alguns endpoints (ex: Evolution API sem apikey) respondem 401/403 - conta como "no ar"
        [[ -n "$code" && "$code" -lt 500 && "$code" -ge 100 ]]
        return $?
    fi
    local status
    status=$(docker inspect --format='{{.State.Health.Status}}' "$container" 2>/dev/null)
    [[ "$status" == "healthy" ]]
}

pendentes=()

for entry in "${checks[@]}"; do
    IFS='|' read -r name url container <<< "$entry"
    elapsed=0
    ok=0

    while [[ "$elapsed" -lt "$max_wait" ]]; do
        if test_servico_saudavel "$url" "$container"; then ok=1; break; fi
        sleep "$interval"
        elapsed=$((elapsed + interval))
        echo "    Aguardando $name... (${elapsed}s)"
    done

    if [[ "$ok" -eq 1 ]]; then
        write_ok "$name esta pronto"
    else
        write_warn "$name nao respondeu em ${max_wait}s"
        pendentes+=("$entry")
    fi
done

# Alguns servicos podem ter falhado ao subir por timeout de dependencia
# (ex: gateway/billing/frontend esperando o auth-service ficar saudavel
# durante um cold start pesado) - tenta recriar so o que ficou pendente.
if [[ "${#pendentes[@]}" -gt 0 ]]; then
    write_step "Tentando reiniciar servicos que nao ficaram saudaveis"
    compose up --detach >/dev/null 2>&1

    for entry in "${pendentes[@]}"; do
        IFS='|' read -r name url container <<< "$entry"
        elapsed=0
        ok=0
        while [[ "$elapsed" -lt 60 ]]; do
            if test_servico_saudavel "$url" "$container"; then ok=1; break; fi
            sleep "$interval"
            elapsed=$((elapsed + interval))
        done
        if [[ "$ok" -eq 1 ]]; then
            write_ok "$name esta pronto"
        else
            write_warn "$name continua indisponivel - verifique: docker compose logs $container"
        fi
    done
fi

# --- Status final ---
write_step "Status dos containers"
compose ps

echo ""
echo -e "\033[32m======================================================\033[0m"
echo -e "\033[32m  Oia a Conta esta no ar!\033[0m"
echo -e "\033[32m======================================================\033[0m"
echo -e "\033[32m  Frontend      http://localhost\033[0m"
echo -e "\033[32m  API Gateway   http://localhost:8090\033[0m"
echo -e "\033[32m  Eureka        http://localhost:8761\033[0m"
echo -e "\033[32m  Evolution API http://localhost:8087\033[0m"
echo -e "\033[32m======================================================\033[0m"
