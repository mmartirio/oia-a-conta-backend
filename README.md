# Oia a Conta — Backend

Microsserviços (Spring Boot) e infraestrutura Docker Compose do sistema Oia a Conta.

Este repositório foi extraído do monorepo original em 2026-08-27, preservando o histórico de commits de tudo exceto a pasta `frontend/`. A aplicação web vive em [oia-a-conta-frontend](https://github.com/mmartirio/oia-a-conta-frontend); a imagem publicada por aquele repo é referenciada pelo serviço `frontend` no `docker-compose.yml` deste.

## Serviços

| Serviço | Pasta |
|---|---|
| API Gateway | `backend/api-gateway` |
| Discovery (Eureka) | `backend/discovery-service` |
| Auth | `backend/auth-service` |
| Catalog | `backend/catalog-service` |
| Table | `backend/table-service` |
| Order | `backend/order-service` |
| Billing | `backend/billing-service` |
| Notification | `backend/notification-service` |
| iFood | `backend/ifood-service` |
| WhatsApp | `backend/whatsapp-service` |

Banco de dados: PostgreSQL único com múltiplos schemas (um por serviço), definido em `init-db.sql` e subido via Docker Compose (dados ficam em volume Docker, não versionados). Cache/mensageria: Redis.

## Rodando localmente

```bash
./start.sh          # sobe tudo do zero (Linux/macOS/Git Bash)
./start.ps1          # equivalente para PowerShell
```

Crie um `.env` na raiz com pelo menos `POSTGRES_PASSWORD` e `JWT_SECRET` (veja as referências em `docker-compose.yml`).

### Frontend em modo dev (hot reload)

O serviço `frontend` do `docker-compose.yml` usa a imagem publicada pelo repo frontend. Para desenvolver com hot reload, clone [oia-a-conta-frontend](https://github.com/mmartirio/oia-a-conta-frontend) como pasta irmã deste (`../oia-a-conta-frontend`) e rode:

```bash
docker compose -f docker-compose.yml -f docker-compose.dev.yml up frontend
```

## CI/CD

- `.github/workflows/ci-backend.yml` — build e testes Maven de cada microsserviço.
- `.github/workflows/cd-deploy.yml` — publica as imagens no GHCR e faz deploy via SSH em push para `main`/tags `v*`.
