# SupportFlow

SupportFlow é uma API REST para gestão interna de chamados/helpdesk.

O projeto foi desenvolvido para praticar backend Java com regras de negócio, autenticação, autorização, persistência, migrations, testes automatizados e validação de fluxo integrado.

> Nome da aplicação no Spring: `support-flow`.

## Principais Funcionalidades

### Auth

- Login com e-mail e senha.
- Geração de token JWT.
- Endpoint para consultar os dados do usuário autenticado.

### Users

- Criação de usuários por administrador.
- Listagem paginada de usuários.
- Busca de usuário por id.
- Bloqueio e desbloqueio de usuários.
- Soft delete de usuários.
- Normalização de e-mail para evitar duplicidade por diferença de caixa.

### Categories

- Criação de categorias.
- Listagem paginada de categorias ativas.
- Busca de categoria por id.
- Atualização de nome e descrição.
- Ativação e desativação de categorias.
- Validação de nome duplicado ignorando maiúsculas/minúsculas.

### Tickets

- Criação de chamados.
- Listagem paginada com filtros por status, prioridade, categoria, responsável e criador.
- Regras de visualização por perfil.
- Atribuição de ticket a técnico por administrador.
- Claim de ticket por técnico.
- Resolução, fechamento e cancelamento conforme regras de negócio.

### Ticket History

- Registro de eventos do ticket:
  - `CREATED`
  - `ASSIGNED`
  - `CLAIMED`
  - `RESOLVED`
  - `CLOSED`
  - `CANCELLED`
- Consulta paginada do histórico de um ticket.

### Ticket Comments

- Comentários em tickets abertos, em andamento ou resolvidos.
- Regras de acesso por perfil:
  - `ADMIN` comenta em tickets permitidos.
  - `TECHNICIAN` comenta em tickets atribuídos a ele.
  - `EMPLOYEE` comenta nos próprios tickets.
- Tickets fechados ou cancelados não recebem novos comentários.

### Ticket Rating

- Avaliação de tickets fechados.
- Nota de 1 a 5.
- Uma avaliação por ticket.
- `TECHNICIAN` não pode criar avaliação.

### Dashboard

- Resumo com total de tickets.
- Tickets abertos, em andamento, resolvidos, fechados e cancelados.
- Tickets críticos.
- Tickets não atribuídos.

## Stack Utilizada

- Java 25
- Spring Boot 4.1.0
- Spring Web MVC
- Spring Security
- JWT com OAuth2 Resource Server
- Spring Data JPA / Hibernate
- MySQL
- Flyway
- Bean Validation
- Lombok
- MapStruct
- Springdoc OpenAPI / Swagger UI
- Docker / Docker Compose
- JUnit 5
- Mockito
- MockMvc
- Testcontainers com MySQL

## Arquitetura / Organização de Pacotes

O projeto segue uma separação por domínios/módulos. Cada módulo concentra controller, DTOs, entidade, mapper, repository e service quando aplicável.

```text
src/main/java/davi/spf/supportflow
|-- auth
|-- category
|-- comment
|-- common
|-- dashboard
|-- history
|-- rating
|-- ticket
`-- user
```

- `auth`: login, JWT e usuário autenticado.
- `user`: usuários, roles, status e administração.
- `category`: categorias de chamados.
- `ticket`: chamados, status, prioridades, filtros e regras principais.
- `history`: histórico de eventos dos tickets.
- `comment`: comentários dos tickets.
- `rating`: avaliação do atendimento.
- `dashboard`: indicadores resumidos.
- `common`: configurações, segurança e tratamento global de erros.

## Regras de Acesso

O sistema trabalha com três roles:

| Role | Permissões principais |
| --- | --- |
| `ADMIN` | Administra usuários e categorias, atribui tickets a técnicos, resolve tickets, fecha tickets, cancela tickets e acessa o dashboard. |
| `TECHNICIAN` | Lista tickets, assume tickets sem responsável, comenta tickets atribuídos a ele, resolve tickets atribuídos e acessa o dashboard. |
| `EMPLOYEE` | Abre tickets, acompanha os próprios tickets, comenta nos próprios tickets, fecha tickets resolvidos e avalia tickets fechados. |

Algumas regras também são validadas na camada de service, como acesso ao histórico, comentários, rating, fechamento e cancelamento.

## Fluxo Principal do Sistema

1. O administrador cria usuários e categorias.
2. Um usuário abre um ticket informando título, descrição, prioridade e categoria.
3. O administrador atribui o ticket a um técnico ou o técnico assume um ticket sem responsável.
4. O técnico comenta e resolve o ticket.
5. O employee que abriu o ticket, ou um admin, fecha o ticket resolvido.
6. O employee avalia o atendimento do ticket fechado.
7. Histórico e dashboard refletem as mudanças do fluxo.

## Como Rodar o Projeto

### Pré-requisitos

- Java 25
- Maven ou Maven Wrapper do projeto
- Docker e Docker Compose

### Configurar ambiente

O projeto possui um arquivo `.env.example`. Para usar as configurações locais:

```powershell
copy .env.example .env
```

No Linux/macOS:

```bash
cp .env.example .env
```

O arquivo `.env.example` define variáveis como:

- `DB_HOST`
- `DB_PORT`
- `DB_NAME`
- `DB_USERNAME`
- `DB_PASSWORD`
- `SERVER_PORT`
- `ADMIN_EMAIL`
- `ADMIN_PASSWORD`

A aplicação cria um usuário `ADMIN` inicial se o e-mail configurado em `ADMIN_EMAIL` ainda não existir.

### Subir o banco com Docker Compose

```powershell
docker compose up -d mysql
```

O `docker-compose.yml` sobe um MySQL com:

- imagem `mysql:8.4` por padrão;
- banco `support_flow` por padrão;
- serviço chamado `mysql`;
- container `support-flow-mysql`;
- volume `support-flow-mysql-data`.

### Rodar a aplicação

No Windows PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

No Linux/macOS:

```bash
./mvnw spring-boot:run
```

Por padrão, a API sobe em:

```text
http://localhost:8080
```

## Swagger / OpenAPI

A documentação interativa da API fica disponível em:

```text
http://localhost:8080/swagger-ui/index.html
```

Para testar endpoints protegidos pelo Swagger UI, faça login em `/auth/login`, copie o valor retornado em `accessToken` e use o botão **Authorize** informando apenas o JWT. O Swagger UI enviará o header:

```http
Authorization: Bearer <token>
```

## Banco de Dados e Migrations

O schema do banco é versionado com Flyway.

As migrations ficam em:

```text
src/main/resources/db/migration
```

Migrations atuais:

- `V1__init_database.sql`
- `V2__create_users_table.sql`
- `V3__create_categories_table.sql`
- `V4__add_unique_constraint_to_categories_name.sql`
- `V5__create_tickets_table.sql`
- `V6__create_ticket_history_table.sql`
- `V7__create_ticket_comments_table.sql`
- `V8__create_ticket_ratings_table.sql`

A configuração JPA usa `ddl-auto: validate`, então o Hibernate valida o schema criado pelas migrations.

## Autenticação

O login retorna um token JWT no campo `accessToken`.

Exemplo de login:

```http
POST /auth/login
Content-Type: application/json

{
  "email": "admin@supportflow.com",
  "password": "admin123"
}
```

Para acessar endpoints protegidos, envie o token no header:

```http
Authorization: Bearer <token>
```

## Principais Endpoints

As listagens usam paginação do Spring Data quando recebem parâmetros como `page`, `size` e `sort`.

### Auth

| Método | Endpoint | Descrição | Acesso |
| --- | --- | --- | --- |
| `POST` | `/auth/login` | Autentica usuário e retorna JWT. | Público |
| `GET` | `/auth/me` | Retorna dados do usuário autenticado. | Autenticado |

### Users

| Método | Endpoint | Descrição | Acesso |
| --- | --- | --- | --- |
| `POST` | `/users/signup` | Cria usuário. | `ADMIN` |
| `GET` | `/users` | Lista usuários não deletados. | `ADMIN` |
| `GET` | `/users/{id}` | Busca usuário por id. | `ADMIN` |
| `POST` | `/users/{id}/block` | Bloqueia usuário. | `ADMIN` |
| `POST` | `/users/{id}/unblock` | Desbloqueia usuário. | `ADMIN` |
| `POST` | `/users/{id}/delete` | Realiza soft delete do usuário. | `ADMIN` |

### Categories

| Método | Endpoint | Descrição | Acesso |
| --- | --- | --- | --- |
| `GET` | `/categories` | Lista categorias ativas. | Autenticado |
| `GET` | `/categories/{id}` | Busca categoria por id. | `ADMIN` |
| `POST` | `/categories` | Cria categoria. | `ADMIN` |
| `PUT` | `/categories/{id}` | Atualiza nome e/ou descrição. | `ADMIN` |
| `PATCH` | `/categories/{id}/activate` | Ativa categoria. | `ADMIN` |
| `PATCH` | `/categories/{id}/deactivate` | Desativa categoria. | `ADMIN` |

### Tickets

| Método | Endpoint | Descrição | Acesso |
| --- | --- | --- | --- |
| `POST` | `/tickets` | Cria ticket. | Autenticado |
| `GET` | `/tickets` | Lista tickets com filtros opcionais: `status`, `priority`, `categoryId`, `assignedToId`, `createdById`. | Autenticado; `EMPLOYEE` visualiza apenas os próprios tickets |
| `GET` | `/tickets/{id}` | Busca ticket por id. | `ADMIN`/`TECHNICIAN`; `EMPLOYEE` somente se for o criador |
| `PATCH` | `/tickets/{id}/assign` | Atribui ticket a um técnico. | `ADMIN` |
| `PATCH` | `/tickets/{id}/claim` | Técnico assume ticket sem responsável. | `TECHNICIAN` |
| `PATCH` | `/tickets/{id}/resolve` | Resolve ticket em andamento. | `ADMIN` ou `TECHNICIAN` atribuído |
| `PATCH` | `/tickets/{id}/close` | Fecha ticket resolvido. | `ADMIN` ou `EMPLOYEE` criador |
| `PATCH` | `/tickets/{id}/cancel` | Cancela ticket permitido pela regra de negócio. | `ADMIN` ou `EMPLOYEE` criador |

### Ticket History

| Método | Endpoint | Descrição | Acesso |
| --- | --- | --- | --- |
| `GET` | `/tickets/{ticketId}/history` | Lista histórico do ticket. | `ADMIN`/`TECHNICIAN`; `EMPLOYEE` somente se for o criador |

### Ticket Comments

| Método | Endpoint | Descrição | Acesso |
| --- | --- | --- | --- |
| `POST` | `/tickets/{id}/comments` | Cria comentário no ticket. | `ADMIN`; `TECHNICIAN` atribuído; `EMPLOYEE` criador |
| `GET` | `/tickets/{id}/comments` | Lista comentários do ticket. | `ADMIN`; `TECHNICIAN` atribuído; `EMPLOYEE` criador |

### Ticket Rating

| Método | Endpoint | Descrição | Acesso |
| --- | --- | --- | --- |
| `POST` | `/tickets/{ticketId}/rating` | Cria avaliação para ticket fechado. | `EMPLOYEE` criador |
| `GET` | `/tickets/{ticketId}/rating` | Consulta avaliação do ticket. | `ADMIN`/`TECHNICIAN`; `EMPLOYEE` criador |

### Dashboard

| Método | Endpoint | Descrição | Acesso |
| --- | --- | --- | --- |
| `GET` | `/dashboard/summary` | Retorna resumo de tickets. | `ADMIN` ou `TECHNICIAN` |

## Testes

O projeto possui testes unitários, testes de controller/security e teste de integração com banco real via Testcontainers.

### Testes unitários de service

- `TicketServiceTest`
- `TicketRatingServiceTest`
- `TicketCommentServiceTest`
- `AuthServiceTest`
- `UserServiceTest`
- `CategoryServiceTest`

### Testes de controller/security com MockMvc

- `TicketControllerTest`
- `AuthControllerTest`
- `UserControllerTest`
- `CategoryControllerTest`
- `DashboardControllerTest`

### Teste de integração

- `TicketFlowIntegrationTest`

O teste de integração usa:

- `@SpringBootTest`
- `MockMvc`
- JWT real
- Flyway real
- Testcontainers com `mysql:8.4`

Fluxo coberto no teste de integração:

1. Login.
2. Criação de ticket.
3. Claim por técnico.
4. Comentário.
5. Resolução.
6. Fechamento.
7. Avaliação.
8. Consulta de histórico.
9. Consulta de dashboard.

Para rodar os testes no Windows PowerShell:

```powershell
.\mvnw.cmd test
```

No Linux/macOS:

```bash
./mvnw test
```

> Para os testes com Testcontainers, o Docker precisa estar em execução.

## Decisões Técnicas

- JWT stateless para evitar sessão no servidor.
- `UserStatus.DELETED` para soft delete de usuários.
- `Category` como entidade para permitir administração dinâmica das categorias.
- Histórico de tickets separado das transições principais, mantendo registro dos eventos relevantes.
- Comentários separados do histórico para diferenciar conversa operacional de auditoria de eventos.
- Flyway como fonte de verdade para evolução do schema.
- Testcontainers para validar integração com MySQL real durante os testes.

## Próximas Melhorias

- Frontend.
- Refresh token.
- Paginação e filtros avançados no dashboard.
- Upload de anexos nos tickets.
- Notificações.
- CI/CD com GitHub Actions.
- Deploy em nuvem.

## Autor

- Nome: Davi Amaral
- Perfil: Desenvolvedor Java Backend em formação
- GitHub: https://github.com/daviamaral7
- LinkedIn: https://www.linkedin.com/in/davi-amaral-2b8312141/
