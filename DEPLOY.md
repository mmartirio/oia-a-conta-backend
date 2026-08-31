# Manual de criação e deploy — Oia a Conta

Guia de referência único: como o projeto é criado do zero, como roda localmente, e como o deploy de produção funciona de ponta a ponta. Escrito depois de um incidente em 2026-08-31 onde a arquitetura real (dois repos, dois destinos de deploy) não estava documentada em lugar nenhum e causou uma tarde inteira de confusão — veja "Erros comuns" no final antes de mexer em CI/CD.

## 1. Arquitetura dos repositórios

O projeto **não é um monorepo**. São dois repositórios GitHub separados, cada um com seu próprio pipeline de deploy:

| Repo | Conteúdo | Branch | Deploy |
|---|---|---|---|
| [`oia-a-conta-backend`](https://github.com/mmartirio/oia-a-conta-backend) | Microsserviços Spring Boot + `docker-compose.yml` (infra completa) | `main` | GitHub Actions → GHCR → SSH no VPS |
| [`oia-a-conta-frontend`](https://github.com/mmartirio/oia-a-conta-frontend) | SPA React/Vite | `main` | Vercel (auto-deploy no push) |

Os dois foram extraídos de um monorepo único em 2026-08-27, preservando o histórico de commits. **Esse monorepo antigo (`oia-a-conta`, sem sufixo) ainda existe no GitHub mas está desconectado de produção** — não empurre nada pra lá, não confie no CI/CD dele. Se encontrar uma pasta de projeto com esse remote, é um sandbox local abandonado, não o código real.

## 2. Serviços do backend

| Serviço | Pasta | Porta interna |
|---|---|---|
| API Gateway | `backend/api-gateway` | 8080 (exposta como `8090`) |
| Discovery (Eureka) | `backend/discovery-service` | 8761 |
| Auth | `backend/auth-service` | 8081 |
| Catalog | `backend/catalog-service` | 8082 |
| Table | `backend/table-service` | 8083 |
| Order | `backend/order-service` | 8084 |
| Notification | `backend/notification-service` | 8085 |
| WhatsApp | `backend/whatsapp-service` | 8086 |
| iFood | `backend/ifood-service` | 8089 |
| Billing | `backend/billing-service` | 8088 |

Infra: PostgreSQL (schema por serviço, `init-db.sql`), Redis, OSRM (roteamento), Ollama (IA local), Evolution API (gateway WhatsApp). Todos Java 21 / Spring Boot / Maven, buildados via Docker multi-stage (`maven:3.9-eclipse-temurin-21` → `eclipse-temurin:21-jre-alpine`).

O `frontend` é um serviço a mais no `docker-compose.yml` do backend — mas **não builda local**, ele puxa a imagem já publicada pelo CD do repo frontend (`ghcr.io/mmartirio/comanda-digital-frontend`). Ver seção 6 sobre a ambiguidade Vercel × esse container.

## 3. Rodando localmente

Pré-requisito: Docker Desktop.

```bash
git clone https://github.com/mmartirio/oia-a-conta-backend.git
cd oia-a-conta-backend
```

Crie um `.env` na raiz (nenhum `.env.example` existe hoje — use este como base, todas as chaves têm default no `docker-compose.yml` exceto onde marcado):

```bash
# Obrigatórios pra qualquer coisa funcionar direito
POSTGRES_PASSWORD=uma-senha-qualquer
JWT_SECRET=uma-string-longa-aleatoria       # HS512 — use pelo menos 64 bytes aleatórios

# Só necessários se for testar a feature correspondente
ALLOWED_ORIGINS=http://localhost,http://localhost:80,http://localhost:3000
FRONTEND_BASE_URL=http://localhost
FRONTEND_IMAGE_TAG=latest
VITE_GOOGLE_CLIENT_ID=
EVOLUTION_API_KEY=
EVOLUTION_INSTANCE_NAME=
EVOLUTION_WEBHOOK_SECRET=
EVOLUTION_WEBHOOK_URL=
GMAIL_USERNAME=
GMAIL_APP_PASSWORD=
IFOOD_API_URL=
IFOOD_CLIENT_ID=
IFOOD_CLIENT_SECRET=
MP_ACCESS_TOKEN=
MP_WEBHOOK_SECRET=
OLLAMA_MODEL=
OSRM_URL=
```

Subir tudo do zero:

```bash
./start.sh              # Linux/macOS/Git Bash — sempre para e reinicia do zero
./start.ps1              # PowerShell, equivalente
# flags: --keep-data (preserva o volume do Postgres) / --no-build / --force (rebuild sem cache)
```

Ou manualmente: `docker compose up -d --build`.

Acesso: `http://localhost` (frontend via nginx, proxy `/api` → gateway) ou `http://localhost:8090` (gateway direto).

Primeiro login: um `SUPER_ADMIN` (`superadmin@comanda.digital`) é criado automaticamente no primeiro boot com banco vazio, senha em `SUPER_ADMIN_SENHA` (default `SuperAdmin@123` se a env não for setada — troque depois do primeiro login).

### Frontend com hot reload

O `docker-compose.yml` do backend só sabe puxar a imagem já publicada do frontend. Pra desenvolver o frontend com hot reload:

```bash
git clone https://github.com/mmartirio/oia-a-conta-frontend.git ../oia-a-conta-frontend   # pasta irmã
cd oia-a-conta-backend
docker compose -f docker-compose.yml -f docker-compose.dev.yml up frontend
```

## 4. Deploy de produção — backend

Workflow: `.github/workflows/cd-deploy.yml`, dispara em push pra `main` (ou tag `v*`).

1. **Job `push-images`** — builda as 10 imagens de serviço (matrix) e publica em `ghcr.io/mmartirio/comanda-digital-<serviço>:main` / `:<sha>`. Usa `secrets.GITHUB_TOKEN` (automático) — **requer que o repositório tenha "Read and write permissions" em Settings → Actions → General → Workflow permissions**, senão falha com `denied: permission_denied: write_package`.
2. **Job `deploy`** (`needs: push-images`, só roda em `main`, ambiente `production`) — SSH no VPS via [`appleboy/ssh-action`](https://github.com/appleboy/ssh-action) (autenticação por **chave**, não senha) e roda:
   ```bash
   cd /opt/comanda-digital
   git pull origin main
   echo "JWT_SECRET=<secret>" > .env
   docker compose pull
   docker compose up -d --remove-orphans
   docker system prune -f
   ```

### Secrets necessários

Em **Settings → Environments → production** (o job usa `environment: production` — secrets de ambiente têm prioridade sobre secrets de repositório com o mesmo nome) ou em Settings → Secrets and variables → Actions:

| Secret | Valor |
|---|---|
| `DEPLOY_HOST` | IP do VPS |
| `DEPLOY_USER` | usuário SSH (ex: `root`) |
| `DEPLOY_SSH_KEY` | chave **privada** SSH (par dedicado, sem passphrase — o servidor precisa ter a pública em `~/.ssh/authorized_keys` do `DEPLOY_USER`) |
| `JWT_SECRET` | mesmo valor usado pelos serviços — **sobrescreve** `/opt/comanda-digital/.env` a cada deploy; se divergir do valor já em uso, desloga todo mundo |
| `GOOGLE_CLIENT_ID` | OAuth do login Google |

Variáveis (Settings → Secrets and variables → Actions → Variables): `VITE_API_URL`, `VITE_WS_URL` (repassadas como build-arg pro build do frontend, se ele rodar a partir daqui).

Gerar um novo par de chaves dedicado ao deploy (não reaproveitar uma chave pessoal):

```bash
ssh-keygen -t ed25519 -f oia_deploy_key -N "" -C "github-actions-deploy-oia"
# pública -> authorized_keys do servidor
# privada -> secret DEPLOY_SSH_KEY
```

## 5. Deploy de produção — frontend

**Vercel** é quem serve o frontend real (auto-deploy a cada push em `main`, projeto conectado ao repo `oia-a-conta-frontend`). `vercel.json` só faz o rewrite de SPA (`/(.*) → /index.html`).

O repo também tem seu próprio `.github/workflows/cd-deploy.yml`, que builda e publica `ghcr.io/mmartirio/comanda-digital-frontend` — **sem nenhum passo de SSH/deploy**. Essa imagem é a que o `docker-compose.yml` do backend referencia no serviço `frontend`; ela só é efetivamente re-baixada no VPS quando o **deploy do backend** roda (`docker compose pull` pega a tag mais nova). Ou seja: dar push só no repo frontend atualiza a Vercel imediatamente, mas só atualiza o container `frontend` do VPS na próxima vez que o backend fizer deploy (ou com um `docker compose pull frontend && docker compose up -d frontend` manual no servidor).

Não ficou 100% claro nesta sessão se esse container `frontend` do VPS está de fato servindo tráfego real ou é redundante à Vercel — vale confirmar antes de assumir que mexer nele importa.

## 6. Servidor de produção (VPS)

- Deploy path: `/opt/comanda-digital` (checkout git do backend + `.env` gerado pelo pipeline)
- `docker compose pull && docker compose up -d --remove-orphans` é como os serviços são atualizados
- `docker system prune -f` roda a cada deploy — não deixe nada importante só em imagens/containers soltos fora do compose

## 7. Erros comuns (já vistos)

- **"denied: permission_denied: write_package"** no job `push-images` → Settings → Actions → General → Workflow permissions → "Read and write permissions".
- **"error: missing server host"** no job `deploy` → faltam `DEPLOY_HOST`/`DEPLOY_USER`/`DEPLOY_SSH_KEY` no Environment `production` (ou nos secrets do repo).
- **Matrix falha rápido demais / vários jobs "canceled" sem erro real** → `fail-fast` (default `true`) cancelou os outros jobs assim que um falhou de verdade; procure o job com erro genuíno, não os "canceled".
- **Push pro repo errado** → confirme que o remote é `oia-a-conta-backend`/`oia-a-conta-frontend`, não `oia-a-conta` (monorepo abandonado, branch `master`, sem deploy real).
- **JWT muda e ninguém mais loga** → toda troca de `JWT_SECRET` (secret do GitHub) invalida sessões existentes no próximo deploy, porque o `.env` do servidor é reescrito do zero a cada vez.
