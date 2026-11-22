# Auth & User Service

Microsserviço de autenticação, autorização (RBAC) e gestão de perfis de usuários construído com **Java 24**, **Spring Boot 4** e **Virtual Threads**, projetado para arquiteturas de microsserviços com **API Gateway**.

---
## Pré-requisitos

* **JDK 24** (caso rode fora do Docker)
* **PostgreSQL** (caso rode fora do Docker)
* **Docker** e **Docker Compose**
* **OpenSSL** (para gerar o par de chaves RSA)
---

## Arquitetura e Tecnologias

* **Linguagem & Runtime:** Java 24 (com Virtual Threads habilitadas via `spring.threads.virtual.enabled`)
* **Framework:** Spring Boot 4.1.x (Web, Data JPA, Security, OAuth2 Resource Server, Validation, Actuator)
* **Banco de Dados & Migrations:** PostgreSQL 16 + Flyway + HikariCP
* **Autenticação & Criptografia:**
    * JWT assimétrico assinado com **RSA-256**
    * Endpoint público **JWKS** (`GET /.well-known/jwks.json`) para validação distribuída no API Gateway
    * **Refresh Token Rotation** com armazenamento em hash **SHA-256** e detecção automática de reuso
    * Hashing de senhas com **BCrypt (fator 12)**
* **Segurança & Compliance:**
    * Proteção contra força bruta (bloqueio automático de 15 minutos após 5 tentativas falhas)
    * Auditoria de eventos críticos (`security_audit_logs`) com rastreamento de IP (`X-Forwarded-For`)
    * Respostas de erro padronizadas seguindo a **RFC 9457 (`ProblemDetail`)**
* **Containerização:** Docker Multi-Stage otimizado com **Spring Boot Layered JAR** e **CDS (Class Data Sharing)**

### Estrutura em Camadas

```text
src/main/java/auth_user_service/
├── config/          # Segurança (SecurityFilterChain), JWT/JWKSet, OpenAPI e Seed de Admin
├── controller/      # Endpoints REST (Auth, User, Admin e JWKS)
├── dto/             # Contratos imutáveis (Java Records) com Bean Validation
├── exception/       # Hierarquia de exceções de domínio e GlobalExceptionHandler (RFC 9457)
├── model/           # Entidades JPA ricas com encapsulamento e invariantes de domínio
├── repository/      # Interfaces Spring Data JPA com consultas otimizadas (JOIN FETCH)
└── service/         # Regras de negócio (Interfaces + Implementações) e Jobs agendados
```

---

## Passo a Passo para Subir a Aplicação

### 1. Gerar o Par de Chaves RSA (Obrigatório)

Por segurança, os arquivos `.pem` não são versionados no Git. Antes de subir a aplicação pela primeira vez, gere as chaves pública e privada:

**Para Desenvolvimento Local (`src/main/resources/certs/`):**
```bash
mkdir -p src/main/resources/certs
openssl genpkey -algorithm RSA -out src/main/resources/certs/private.pem -pkeyopt rsa_keygen_bits:2048
openssl rsa -pubout -in src/main/resources/certs/private.pem -out src/main/resources/certs/public.pem
```

**Para Produção na Nuvem (`certs-prod/`):**
```bash
mkdir -p certs-prod
openssl genpkey -algorithm RSA -out certs-prod/private.pem -pkeyopt rsa_keygen_bits:2048
openssl rsa -pubout -in certs-prod/private.pem -out certs-prod/public.pem
chmod 600 certs-prod/private.pem
```
---

### 2. Executar em Desenvolvimento Local (`dev`)

O perfil `dev` habilita o **Swagger UI**, carrega os usuários iniciais de teste e cria o administrador padrão (`admin@seuprojeto.com` / `Admin@123456`).

#### Opção A: Tudo via Docker Compose (Recomendado)


```bash
docker compose up -d --build
```
**ou**
```bash
docker compose -f docker-compose.yml -f docker-compose.dev.yml up -d --build
```

#### Opção B: Apenas o PostgreSQL no Docker + Aplicação na IDE

```bash
docker compose up -d postgres
```
**ou**. 
```bash
docker compose -f docker-compose.yml -f docker-compose.dev.yml up -d postgres
```
2. Inicie a classe `AuthUserServiceApplication` pela sua IDE (porta **8081**).

* **Swagger UI:** `http://localhost:8081/swagger-ui.html`
* **Health Check:** `http://localhost:8081/actuator/health`
* **Chave Pública JWKS:** `http://localhost:8081/.well-known/jwks.json`

---

### 3. Executar em Produção / Cloud (`prod`)

#### Ainda estou ajustando

---

## Executando os Testes Automatizados

Certifique-se de que o container `postgres` esteja rodando e execute a suíte completa (Testes Unitários de Domínio/Serviço + Testes de Integração E2E):

```bash
./mvnw test
```

---

## Documentação dos Endpoints

### **Swagger**

```
http://localhost:8081/swagger-ui/index.html
```
**ou**.
```bash
http://localhost:8081/swagger-ui.html
```

### Públicos & Autenticação (`/api/v1/auth` e JWKS)

| Método | Rota | Descrição | Status de Sucesso |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/auth/register` | Cadastra novo usuário (`USER` ou `EDITOR`) | `201 Created` |
| `POST` | `/api/v1/auth/login` | Autentica e retorna par `accessToken` + `refreshToken` | `200 OK` |
| `POST` | `/api/v1/auth/refresh` | Rotaciona o `refreshToken` e emite novo par de tokens | `200 OK` |
| `POST` | `/api/v1/auth/logout` | Revoga o `refreshToken` informado | `204 No Content` |
| `GET` | `/.well-known/jwks.json` | Expõe a chave pública RSA para o API Gateway | `200 OK` |
| `GET` | `/actuator/health` | Verifica saúde da aplicação (`liveness` e `readiness`) | `200 OK` |

### Usuário Autenticado (`/api/v1/users`) — *Requer `Authorization: Bearer <token>`*

| Método | Rota | Descrição | Status de Sucesso |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/users/me` | Retorna os dados da conta e perfil do usuário logado | `200 OK` |
| `PATCH` | `/api/v1/users/me/profile` | Atualiza parcialmente nome, bio ou URL do avatar | `200 OK` |
| `PATCH` | `/api/v1/users/me/password` | Altera a senha e revoga sessões ativas | `204 No Content` |

### Administração (`/api/v1/admin`) — *Requer `ROLE_ADMIN`*

| Método | Rota | Descrição | Status de Sucesso |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/admin/users` | Lista todos os usuários com paginação (`?page=0&size=20`) | `200 OK` |
| `PATCH` | `/api/v1/admin/users/{id}/role` | Altera a permissão de um usuário (`USER`, `EDITOR`, `ADMIN`) | `200 OK` |
| `PATCH` | `/api/v1/admin/users/{id}/status` | Ativa (`true`) ou desativa (`false`) uma conta | `200 OK` |

---

