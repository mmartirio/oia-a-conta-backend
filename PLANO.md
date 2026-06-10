# Plano de Estrutura — Comanda Digital SaaS (Microsserviços Multi-Tenant)

## 1. Visão Geral

**Comanda Digital** é um sistema SaaS multi-tenant para gerenciamento de pedidos em restaurantes.

- Cada restaurante se cadastra na plataforma e obtém acesso isolado
- Garçons emitem pedidos via PWA instalável em tablets/celulares
- A cozinha recebe pedidos em tempo real via WebSocket
- O encerramento gera o total da mesa para cobrança
- Sem integração com gateway de pagamento (garçom usa maquininha física)

---

## 2. Multi-Tenancy

### Isolamento de dados
Estratégia: **Row-Level Tenancy** — todas as tabelas tenant-scoped possuem `restaurante_id`.

### Níveis de acesso

| Role | Descrição |
|------|-----------|
| `SUPER_ADMIN` | Dono da plataforma. Vê e gerencia todos os restaurantes |
| `ADMIN` | Dono/gerente do restaurante. Gerencia mesas, cardápio, funcionários do seu restaurante |
| `GARCON` | Abre comandas, registra pedidos, recebe notificação de prato pronto |
| `COZINHA` | Visualiza pedidos recebidos, marca como preparando/pronto |

### Fluxo de onboarding
```
1. Dono do restaurante acessa /registro
2. Preenche: nome do restaurante, nome, e-mail, senha
3. Sistema cria Restaurante + usuário ADMIN vinculado
4. Admin faz login e configura: mesas, categorias, produtos, funcionários
5. Garçons e cozinha recebem e-mail/código de acesso criado pelo Admin
```

### Login
- Tela única de login: e-mail + senha
- E-mail é globalmente único por usuário
- O JWT contém: `userId`, `restauranteId`, `role`
- Todos os serviços filtram dados pelo `restauranteId` extraído do JWT

---

## 3. Arquitetura de Microsserviços

```
┌──────────────────────────────────────────────────────────────────────────┐
│                   FRONTEND (React 18 + Vite + PWA)                        │
│                          Porta: 5173 (dev) | 80 (prod)                    │
└──────────────────────────────┬───────────────────────────────────────────┘
                               │ HTTP/REST + WebSocket (STOMP)
┌──────────────────────────────▼───────────────────────────────────────────┐
│                           API GATEWAY                                     │
│                   (Spring Cloud Gateway — Porta 8080)                     │
│  Roteamento inteligente + CORS + rate limiting                            │
└──┬──────────┬─────────────┬──────────┬──────────────┬─────────────────────┘
   │          │             │          │              │
┌──▼──┐  ┌───▼───┐  ┌──────▼──┐  ┌───▼──────┐  ┌────▼────────┐
│auth │  │catalog│  │  table  │  │  order   │  │notification │
│8081 │  │ 8082  │  │  8083   │  │  8084    │  │    8085     │
└──┬──┘  └───┬───┘  └──────┬──┘  └───┬──────┘  └────┬────────┘
   │          │             │          │              │ WebSocket
   └──────────┴─────────────┴──────────┴──────────────┘
                            │
           ┌────────────────▼──────────────────┐
           │           PostgreSQL 16             │
           │  db_auth / db_catalog / db_table    │
           │  db_order                           │
           └───────────────────────────────────┘

           ┌────────────────────────────────────┐
           │         Eureka Server :8761          │
           │    (todos os serviços registrados)   │
           └────────────────────────────────────┘
```

---

## 4. Microsserviços

### 4.1 `discovery-service` (Porta 8761)
Spring Cloud Netflix Eureka. Registro e descoberta de todos os serviços.

---

### 4.2 `api-gateway` (Porta 8080)
Spring Cloud Gateway. Ponto único de entrada para o frontend.

**Rotas:**
```
/api/auth/**        → auth-service:8081
/api/restaurantes/** → auth-service:8081
/api/usuarios/**    → auth-service:8081
/api/categorias/**  → catalog-service:8082
/api/produtos/**    → catalog-service:8082
/api/mesas/**       → table-service:8083
/api/comandas/**    → order-service:8084
/api/pedidos/**     → order-service:8084
/ws/**              → notification-service:8085
```

---

### 4.3 `auth-service` (Porta 8081)
Autenticação, usuários e gerenciamento de restaurantes (tenants).

**Endpoints:**

| Endpoint | Método | Papel |
|----------|--------|-------|
| `/api/auth/login` | POST | Público |
| `/api/auth/registro` | POST | Público (cria restaurante + admin) |
| `/api/auth/me` | GET | Autenticado |
| `/api/restaurantes` | GET | SUPER_ADMIN |
| `/api/restaurantes/{id}` | GET/PUT/DELETE | SUPER_ADMIN |
| `/api/usuarios` | GET | ADMIN (do mesmo restaurante) |
| `/api/usuarios` | POST | ADMIN |
| `/api/usuarios/{id}` | PUT/DELETE | ADMIN |

**Banco: `db_auth`**
```sql
restaurantes (id, nome, slug, plano, ativo, created_at)
usuarios     (id, restaurante_id, nome, email, senha, role, ativo)
```

---

### 4.4 `catalog-service` (Porta 8082)
Cardápio — categorias e produtos por restaurante.

**Endpoints:**

| Endpoint | Método | Papel |
|----------|--------|-------|
| `/api/categorias` | GET | Autenticado |
| `/api/categorias` | POST/PUT/DELETE | ADMIN |
| `/api/produtos` | GET | Autenticado |
| `/api/produtos` | POST/PUT/DELETE | ADMIN |

**Banco: `db_catalog`**
```sql
categorias (id, restaurante_id, nome, ativo)
produtos   (id, restaurante_id, categoria_id, nome, descricao, preco, ativo)
```

---

### 4.5 `table-service` (Porta 8083)
Mesas por restaurante.

**Endpoints:**

| Endpoint | Método | Papel |
|----------|--------|-------|
| `/api/mesas` | GET | Autenticado |
| `/api/mesas` | POST/PUT/DELETE | ADMIN |
| `/api/mesas/{id}/status` | PUT | GARCON/ADMIN |

**Banco: `db_table`**
```sql
mesas (id, restaurante_id, numero, capacidade, status)
```

**Status:** `DISPONIVEL` | `OCUPADA` | `AGUARDANDO_PAGAMENTO`

---

### 4.6 `order-service` (Porta 8084)
Comandas e pedidos. Usa Feign para consultar table-service e catalog-service.

**Endpoints:**

| Endpoint | Método | Papel |
|----------|--------|-------|
| `/api/mesas/{id}/comanda` | POST | GARCON/ADMIN |
| `/api/mesas/{id}/comanda` | GET | GARCON/ADMIN |
| `/api/comandas/{id}` | GET | GARCON/ADMIN |
| `/api/comandas/{id}/pedidos` | POST | GARCON/ADMIN |
| `/api/comandas/{id}/fechar` | PUT | GARCON/ADMIN |
| `/api/pedidos/ativos` | GET | COZINHA/ADMIN |
| `/api/pedidos/{id}/preparando` | PUT | COZINHA/ADMIN |
| `/api/pedidos/{id}/pronto` | PUT | COZINHA/ADMIN |
| `/api/pedidos/{id}/entregue` | PUT | GARCON/ADMIN |

**Banco: `db_order`**
```sql
comandas    (id, restaurante_id, mesa_id, garcon_id, garcon_nome, status, metodo_pagamento, created_at, closed_at)
pedidos     (id, comanda_id, restaurante_id, status, observacao, created_at, ready_at)
itens_pedido(id, pedido_id, produto_id, produto_nome, quantidade, observacao, preco_unitario)
```

**Status Comanda:** `ABERTA` | `FECHADA`
**Status Pedido:** `ENVIADO` | `PREPARANDO` | `PRONTO` | `ENTREGUE`
**Método Pagamento:** `DINHEIRO` | `PIX` | `CARTAO_CREDITO` | `CARTAO_DEBITO`

---

### 4.7 `notification-service` (Porta 8085)
WebSocket STOMP. Recebe eventos REST do order-service e faz broadcast.

**Endpoint REST (interno):**
```
POST /internal/notificar  → recebe evento e publica via WebSocket
```

**Tópicos WebSocket:**
```
/topic/cozinha/{restauranteId}        → novo pedido para a cozinha
/topic/garcon/{restauranteId}/{userId} → pedido pronto para o garçom específico
```

---

## 5. Modelos de Dados Completos

### db_auth

```sql
CREATE TABLE restaurantes (
    id          BIGSERIAL PRIMARY KEY,
    nome        VARCHAR(200) NOT NULL,
    slug        VARCHAR(100) UNIQUE NOT NULL,
    plano       VARCHAR(20) DEFAULT 'BASICO',  -- BASICO | PRO | ENTERPRISE
    ativo       BOOLEAN DEFAULT TRUE,
    created_at  TIMESTAMP DEFAULT NOW()
);

CREATE TABLE usuarios (
    id              BIGSERIAL PRIMARY KEY,
    restaurante_id  BIGINT REFERENCES restaurantes(id),
    nome            VARCHAR(100) NOT NULL,
    email           VARCHAR(100) UNIQUE NOT NULL,
    senha           VARCHAR(255) NOT NULL,  -- BCrypt
    role            VARCHAR(20) NOT NULL,   -- SUPER_ADMIN | ADMIN | GARCON | COZINHA
    ativo           BOOLEAN DEFAULT TRUE,
    created_at      TIMESTAMP DEFAULT NOW()
);
```

### db_catalog

```sql
CREATE TABLE categorias (
    id              BIGSERIAL PRIMARY KEY,
    restaurante_id  BIGINT NOT NULL,
    nome            VARCHAR(100) NOT NULL,
    ativo           BOOLEAN DEFAULT TRUE
);

CREATE TABLE produtos (
    id              BIGSERIAL PRIMARY KEY,
    restaurante_id  BIGINT NOT NULL,
    categoria_id    BIGINT REFERENCES categorias(id),
    nome            VARCHAR(200) NOT NULL,
    descricao       TEXT,
    preco           DECIMAL(10,2) NOT NULL,
    ativo           BOOLEAN DEFAULT TRUE
);
```

### db_table

```sql
CREATE TABLE mesas (
    id              BIGSERIAL PRIMARY KEY,
    restaurante_id  BIGINT NOT NULL,
    numero          INTEGER NOT NULL,
    capacidade      INTEGER DEFAULT 4,
    status          VARCHAR(30) DEFAULT 'DISPONIVEL',
    UNIQUE(restaurante_id, numero)
);
```

### db_order

```sql
CREATE TABLE comandas (
    id                  BIGSERIAL PRIMARY KEY,
    restaurante_id      BIGINT NOT NULL,
    mesa_id             BIGINT NOT NULL,
    mesa_numero         INTEGER NOT NULL,
    garcon_id           BIGINT NOT NULL,
    garcon_nome         VARCHAR(100) NOT NULL,
    status              VARCHAR(20) DEFAULT 'ABERTA',
    metodo_pagamento    VARCHAR(30),
    created_at          TIMESTAMP DEFAULT NOW(),
    closed_at           TIMESTAMP
);

CREATE TABLE pedidos (
    id              BIGSERIAL PRIMARY KEY,
    comanda_id      BIGINT REFERENCES comandas(id),
    restaurante_id  BIGINT NOT NULL,
    status          VARCHAR(20) DEFAULT 'ENVIADO',
    observacao      TEXT,
    created_at      TIMESTAMP DEFAULT NOW(),
    ready_at        TIMESTAMP
);

CREATE TABLE itens_pedido (
    id              BIGSERIAL PRIMARY KEY,
    pedido_id       BIGINT REFERENCES pedidos(id),
    produto_id      BIGINT NOT NULL,
    produto_nome    VARCHAR(200) NOT NULL,
    quantidade      INTEGER NOT NULL,
    observacao      TEXT,
    preco_unitario  DECIMAL(10,2) NOT NULL
);
```

---

## 6. Fluxo Completo de Negócio

```
ONBOARDING:
1. Dono acessa /registro → cria conta do restaurante
2. Admin configura: categorias, produtos, mesas, funcionários

ATENDIMENTO:
3. Garçom faz login (email + senha)
4. Garçom vê painel de mesas → seleciona mesa DISPONIVEL
5. Garçom abre comanda → mesa passa a OCUPADA
6. Garçom adiciona itens ao pedido e clica "Enviar para Cozinha"
   → order-service cria Pedido (ENVIADO)
   → order-service POST /internal/notificar → notification-service
   → notification-service publica em /topic/cozinha/{restauranteId}
   → Cozinha recebe alerta visual em tempo real

COZINHA:
7. Cozinheiro clica "Preparando" → Pedido = PREPARANDO
8. Cozinheiro clica "Pronto"
   → Pedido = PRONTO
   → notification-service publica em /topic/garcon/{restauranteId}/{garconId}
   → Garçom recebe ALERTA VISUAL + SONORO no dispositivo

ENTREGA:
9. Garçom clica "Receber" → Pedido = ENTREGUE
10. Processo se repete para N pedidos da mesma mesa

FECHAMENTO:
11. Garçom clica "Fechar Comanda"
    → Sistema exibe total (soma de todos os itens dos pedidos)
    → Garçom seleciona método de pagamento
    → Comanda = FECHADA, Mesa = DISPONIVEL
```

---

## 7. Estrutura de Pastas

```
comanda-digital/
│
├── discovery-service/
│   ├── src/main/java/com/comandadigital/discovery/
│   │   └── DiscoveryServiceApplication.java
│   ├── src/main/resources/application.yml
│   ├── pom.xml
│   └── README.md
│
├── api-gateway/
│   ├── src/main/java/com/comandadigital/gateway/
│   │   └── ApiGatewayApplication.java
│   ├── src/main/resources/application.yml
│   ├── pom.xml
│   └── README.md
│
├── auth-service/
│   ├── src/main/java/com/comandadigital/auth/
│   │   ├── config/
│   │   │   └── SecurityConfig.java
│   │   ├── controller/
│   │   │   ├── AuthController.java
│   │   │   └── UsuarioController.java
│   │   ├── dto/
│   │   │   ├── request/  (LoginRequest, RegistroRequest, UsuarioRequest)
│   │   │   └── response/ (AuthResponse, UsuarioResponse, RestauranteResponse)
│   │   ├── entity/
│   │   │   ├── Restaurante.java
│   │   │   └── Usuario.java
│   │   ├── enums/
│   │   │   └── Role.java
│   │   ├── exception/
│   │   │   └── GlobalExceptionHandler.java
│   │   ├── repository/
│   │   │   ├── RestauranteRepository.java
│   │   │   └── UsuarioRepository.java
│   │   ├── security/
│   │   │   ├── JwtUtil.java
│   │   │   ├── JwtAuthFilter.java
│   │   │   └── UserDetailsServiceImpl.java
│   │   ├── service/
│   │   │   ├── AuthService.java
│   │   │   └── UsuarioService.java
│   │   └── AuthServiceApplication.java
│   ├── src/main/resources/application.yml
│   ├── pom.xml
│   └── README.md
│
├── catalog-service/
│   ├── src/main/java/com/comandadigital/catalog/
│   │   ├── config/SecurityConfig.java
│   │   ├── controller/
│   │   │   ├── CategoriaController.java
│   │   │   └── ProdutoController.java
│   │   ├── dto/request/ + response/
│   │   ├── entity/ (Categoria, Produto)
│   │   ├── repository/
│   │   ├── security/ (JwtUtil, JwtAuthFilter)
│   │   ├── service/
│   │   └── CatalogServiceApplication.java
│   ├── src/main/resources/application.yml
│   ├── pom.xml
│   └── README.md
│
├── table-service/
│   ├── src/main/java/com/comandadigital/table/
│   │   ├── config/SecurityConfig.java
│   │   ├── controller/MesaController.java
│   │   ├── dto/request/ + response/
│   │   ├── entity/Mesa.java
│   │   ├── enums/StatusMesa.java
│   │   ├── repository/MesaRepository.java
│   │   ├── security/ (JwtUtil, JwtAuthFilter)
│   │   ├── service/MesaService.java
│   │   └── TableServiceApplication.java
│   ├── src/main/resources/application.yml
│   ├── pom.xml
│   └── README.md
│
├── order-service/
│   ├── src/main/java/com/comandadigital/order/
│   │   ├── client/
│   │   │   ├── TableClient.java    (Feign → table-service)
│   │   │   └── NotificationClient.java (Feign → notification-service)
│   │   ├── config/SecurityConfig.java
│   │   ├── controller/
│   │   │   ├── ComandaController.java
│   │   │   └── PedidoController.java
│   │   ├── dto/request/ + response/
│   │   ├── entity/ (Comanda, Pedido, ItemPedido)
│   │   ├── enums/ (StatusComanda, StatusPedido, MetodoPagamento)
│   │   ├── repository/
│   │   ├── security/ (JwtUtil, JwtAuthFilter)
│   │   ├── service/
│   │   │   ├── ComandaService.java
│   │   │   └── PedidoService.java
│   │   └── OrderServiceApplication.java
│   ├── src/main/resources/application.yml
│   ├── pom.xml
│   └── README.md
│
├── notification-service/
│   ├── src/main/java/com/comandadigital/notification/
│   │   ├── config/
│   │   │   ├── SecurityConfig.java
│   │   │   └── WebSocketConfig.java
│   │   ├── controller/NotificationController.java
│   │   ├── dto/NotificacaoMessage.java
│   │   ├── service/NotificationService.java
│   │   └── NotificationServiceApplication.java
│   ├── src/main/resources/application.yml
│   ├── pom.xml
│   └── README.md
│
├── frontend/
│   ├── public/
│   │   ├── manifest.json
│   │   └── icons/ (192x192, 512x512)
│   ├── src/
│   │   ├── api/
│   │   │   ├── axios.js
│   │   │   ├── authApi.js
│   │   │   ├── mesaApi.js
│   │   │   ├── comandaApi.js
│   │   │   ├── pedidoApi.js
│   │   │   ├── produtoApi.js
│   │   │   ├── categoriaApi.js
│   │   │   └── usuarioApi.js
│   │   ├── context/
│   │   │   ├── AuthContext.jsx
│   │   │   ├── WebSocketContext.jsx
│   │   │   └── NotificationContext.jsx
│   │   ├── hooks/
│   │   │   ├── useWebSocket.js
│   │   │   └── useNotification.js
│   │   ├── pages/
│   │   │   ├── Login.jsx
│   │   │   ├── Registro.jsx          ← cadastro de novo restaurante
│   │   │   ├── admin/
│   │   │   │   ├── AdminLayout.jsx
│   │   │   │   ├── Dashboard.jsx
│   │   │   │   ├── Mesas.jsx
│   │   │   │   ├── Produtos.jsx
│   │   │   │   ├── Categorias.jsx
│   │   │   │   └── Usuarios.jsx
│   │   │   ├── garcon/
│   │   │   │   ├── GarconLayout.jsx
│   │   │   │   ├── SelecaoMesa.jsx
│   │   │   │   ├── Comanda.jsx
│   │   │   │   └── NovoPedido.jsx
│   │   │   └── cozinha/
│   │   │       ├── CozinhaLayout.jsx
│   │   │       └── Pedidos.jsx
│   │   ├── components/
│   │   │   ├── PrivateRoute.jsx
│   │   │   ├── NotificationAlert.jsx
│   │   │   └── Layout/
│   │   ├── App.jsx
│   │   └── main.jsx
│   ├── index.html
│   ├── vite.config.js
│   └── package.json
│
├── docker-compose.yml
├── .gitignore
├── PLANO.md         ← este arquivo
└── README.md
```

---

## 8. Portas e URLs

| Serviço | Porta | Descrição |
|---------|-------|-----------|
| Eureka Dashboard | 8761 | http://localhost:8761 |
| API Gateway | 8080 | http://localhost:8080/api |
| auth-service | 8081 | Autenticação + usuários |
| catalog-service | 8082 | Cardápio |
| table-service | 8083 | Mesas |
| order-service | 8084 | Comandas e pedidos |
| notification-service | 8085 | WebSocket STOMP |
| Frontend (dev) | 5173 | http://localhost:5173 |
| PostgreSQL | 5432 | localhost:5432 |

---

## 9. Segurança e JWT

```
JWT Payload:
{
  "sub": "user@email.com",
  "userId": 1,
  "restauranteId": 1,
  "role": "GARCON",
  "exp": ...
}
```

- **Emissão:** auth-service
- **Validação:** cada microsserviço valida localmente com o secret compartilhado (`JWT_SECRET` via env var)
- **SUPER_ADMIN:** `restauranteId = null` no JWT (acessa tudo)
- **WebSocket:** token enviado como query param na conexão (`/ws?token=...`)

---

## 10. PWA — Notificações em Tempo Real

1. Garçom instala o app na tela inicial do dispositivo
2. Ao fazer login, conecta ao WebSocket do notification-service
3. Inscreve-se no tópico `/topic/garcon/{restauranteId}/{userId}`
4. Quando pedido fica pronto: **alerta visual** (modal/toast) + **som** (Web Audio API)
5. Garçom toca "Receber" para confirmar

---

## 11. Ordem de Inicialização (Docker)

```
1. PostgreSQL           → banco de dados
2. discovery-service    → Eureka (outros dependem)
3. auth-service         → (independente)
4. catalog-service      → (independente)
5. table-service        → (independente)
6. notification-service → (independente)
7. order-service        → (depende de table + notification via Feign)
8. api-gateway          → (depende de Eureka + todos registrados)
9. frontend             → static build ou dev server
```

---

## 12. Plano de Sprints

### Sprint 1 — Infraestrutura Base
- [ ] discovery-service (Eureka)
- [ ] api-gateway (Gateway + roteamento)
- [ ] docker-compose (PostgreSQL + serviços)

### Sprint 2 — Auth e Multi-Tenancy
- [ ] auth-service (registro de restaurante, login, JWT, usuários)
- [ ] Frontend: tela de login e registro

### Sprint 3 — Cardápio e Mesas
- [ ] catalog-service (categorias, produtos)
- [ ] table-service (mesas CRUD + status)
- [ ] Frontend admin: gerenciar produtos, categorias, mesas

### Sprint 4 — Comandas e Pedidos
- [ ] order-service (comandas + pedidos + cálculo de total)
- [ ] Frontend garçom: seleção de mesa, comanda, envio de pedido

### Sprint 5 — Cozinha e Notificações
- [ ] notification-service (WebSocket STOMP)
- [ ] Frontend cozinha: visualização e status dos pedidos
- [ ] Frontend garçom: notificação sonora/visual quando pedido pronto

### Sprint 6 — PWA e Polimento
- [ ] vite-plugin-pwa (manifest, service worker, installable)
- [ ] Ajustes de UX/UI responsivo
- [ ] Testes end-to-end do fluxo completo
