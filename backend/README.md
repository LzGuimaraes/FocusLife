# FocusLifeHub

Backend em Java com Spring Boot para gerenciar estudos, tarefas, metas, finanças, contas e matérias, com autenticação JWT e PostgreSQL.

## Tecnologias
- Spring Boot
- Spring Security (JWT com java-jwt)
- Spring Data JPA
- PostgreSQL
- Lombok
- Docker

## Autenticação
- Stateless com JWT.
- Rotas públicas: POST /auth/login, POST /auth/register.
- Demais rotas exigem autenticação.
- O token é enviado em cookie httpOnly ("jwt"). O filtro também aceita Authorization: Bearer <token>.
- CORS permitido para: https://focus-life-hub-front.vercel.app.

Variáveis de ambiente necessárias (application.properties):
- SPRING_DATASOURCE_URL
- SPRING_DATASOURCE_USERNAME
- SPRING_DATASOURCE_PASSWORD
- JWT_SECRET

## Endpoints

### Auth (/auth)
- POST /auth/login
  - Body: {"email":"string","password":"string"}
  - Resposta: {"message":"Login successful"} + cookie "jwt" setado.
- POST /auth/register
  - Body: {"name":"string","email":"string","password":"string"}
  - Resposta: 201 com dados do usuário criado.
- POST /auth/logout
  - Limpa o cookie "jwt".

### Usuário (/users)
- GET /users/me
  - Retorna dados do usuário autenticado.
- PUT /users/me
  - Body: conforme UserUpdateDTO (ex.: nome/email/senha se aplicável).

### Finanças (/financas)
- GET /financas/all
- GET /financas/all/{id}
- POST /financas/create
- PUT /financas/alter/{id}
- DELETE /financas/delete/{id}

### Contas (/contas)
- GET /contas/all
- GET /contas/all/{id}
- GET /contas/by-financa/{financasId}
- POST /contas/create
- PUT /contas/alter/{id}
- DELETE /contas/delete/{id}

### Matérias (/materias)
- GET /materias/all
- GET /materias/all/{id}
- POST /materias/create
- PUT /materias/alter/{id}
- DELETE /materias/delete/{id}

### Estudos (/estudos)
- GET /estudos/all
- GET /estudos/all/{id}
- POST /estudos/create
- PUT /estudos/alter/{id}
- DELETE /estudos/delete/{id}

### Metas (/metas)
- GET /metas/all
- GET /metas/all/{id}
- POST /metas/create
- PUT /metas/alter/{id}
- DELETE /metas/delete/{id}

### Tarefas (/tarefas)
- GET /tarefas/all
- GET /tarefas/all/{id}
- POST /tarefas/create
- PUT /tarefas/alter/{id}
- DELETE /tarefas/delete/{id}

## Modelos e Tabelas (JPA)

### UserModel (tb_user)
- id (Long, PK)
- nome (String)
- email (String)
- password (String)
- metas (OneToMany -> MetasModel)

### FinancasModel (tb_financas)
- id (Long, PK)
- nome (String)
- moeda (String)
- user (ManyToOne -> UserModel, FK user_id)

### ContasModel (tb_contas)
- id (Long, PK)
- nome (String)
- tipo (String)
- saldo (Float)
- financas (ManyToOne -> FinancasModel, FK financas_id)

### MateriaModel (tb_materia)
- Campos conforme entidade (id, nome, etc.)

### EstudosModel (tb-estudos)
- Campos conforme entidade

### MetasModel (tb_metas)
- Campos conforme entidade (inclui relação com User)

### TarefasModel (tb_tarefas)
- Campos conforme entidade

Obs.: As entidades Materia, Estudos, Metas e Tarefas possuem mapeamentos complementares (enums e relações) conforme pacote.

## Executar via Docker

Pré-requisitos:
- Docker instalado
- Banco PostgreSQL acessível

1) Build da imagem:
- docker build -t focus-life-hub .

2) Executar o container (exemplo):
- docker run -p 8080:8080 \
  -e SPRING_DATASOURCE_URL="jdbc:postgresql://<HOST>:<PORT>/<DB>" \
  -e SPRING_DATASOURCE_USERNAME="<USER>" \
  -e SPRING_DATASOURCE_PASSWORD="<PASS>" \
  -e JWT_SECRET="<segredo>" \
  --name focus-life-hub focus-life-hub

A API ficará disponível em http://localhost:8080.

## Executar Localmente (Spring)

Pré-requisitos:
- JDK 17+
- PostgreSQL

1) Clone o repositório:
- git clone <URL_DO_REPO>
- cd FocusLifeHub

2) Defina variáveis de ambiente (Windows PowerShell exemplo):
- $env:SPRING_DATASOURCE_URL="jdbc:postgresql://<HOST>:<PORT>/<DB>"
- $env:SPRING_DATASOURCE_USERNAME="<USER>"
- $env:SPRING_DATASOURCE_PASSWORD="<PASS>"
- $env:JWT_SECRET="<segredo>"

3) Execute a aplicação:
- .\mvnw spring-boot:run

A API ficará disponível em http://localhost:8080.

## Exemplos de uso

- Login:
  - curl -X POST http://localhost:8080/auth/login -H "Content-Type: application/json" -d "{\"email\":\"user@mail.com\",\"password\":\"123456\"}"
- Listar finanças (após autenticado, com cookie JWT):
  - curl -X GET http://localhost:8080/financas/all --cookie "jwt=<TOKEN>"

## Observações
- Em produção, o cookie JWT está com httpOnly e secure habilitados; SameSite=None.
- As rotas protegidas exigem token válido.
- O Hibernate está com ddl-auto=update e dialect do PostgreSQL.
