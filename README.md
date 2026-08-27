# API de Pessoas + Previsão de Nacionalidade

Prova técnica. API REST em Java para registrar pessoas, consultá-las, excluí-las e
prever a nacionalidade a partir do nome, consumindo a API pública
[nationalize.io](https://api.nationalize.io). Inclui autenticação por JWT e uma
interface web para consumir tudo pelo navegador.

---

## Como rodar

Precisa apenas de **JDK 21**. O banco é em memória e a interface é servida pela
própria aplicação — não existe segundo processo para subir.

```bash
./mvnw spring-boot:run          # Linux / macOS
.\mvnw.cmd spring-boot:run      # Windows (o .\ é obrigatório no PowerShell)
```

Sem o wrapper, com Maven instalado:

```bash
mvn spring-boot:run
```

Depois de subir:

| O quê | Onde |
|---|---|
| Interface web | http://localhost:8080 |
| **Explicação do projeto** | http://localhost:8080/explicacao |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Console do banco (H2) | http://localhost:8080/h2-console |

Se for para ler só uma coisa, leia a **explicação**: ela mostra as telas, o que
foi pedido em cada ponto da prova e o porquê de cada decisão, em linguagem que
não exige ser da área.

Console do H2 — JDBC URL `jdbc:h2:mem:pessoas`, usuário `sa`, senha em branco.

A base já sobe com 4 pessoas cadastradas, para a tela não abrir vazia.

### Credenciais

| Usuário | Senha | Pode |
|---|---|---|
| `admin` | `admin123` | listar, consultar, registrar, prever e **excluir** |
| `user` | `user123` | listar, consultar, registrar e prever |

Este arquivo é o **único** lugar onde as credenciais aparecem. Não estão impressas
na tela de login, nem no Swagger, nem na página de explicação — deixar usuário e
senha na interface é atalho que economiza um minuto e vira hábito.

Em uso real elas vêm de variável de ambiente (`APP_ADMIN_SENHA`, `APP_USER_SENHA`);
os valores acima são apenas o padrão para o projeto subir com um comando.

---

## Deploy

O repositório tem um `render.yaml`. No [Render](https://render.com), em
**New → Blueprint**, apontando para este repositório, o serviço sobe sozinho a
partir do `Dockerfile` — sem preencher formulário.

O segredo do JWT é gerado pelo próprio Render (`generateValue`) e nunca fica
versionado. As senhas dos perfis são definidas no painel, em `APP_ADMIN_SENHA` e
`APP_USER_SENHA`.

Dois avisos honestos sobre o plano gratuito:

- **Hiberna após 15 minutos sem acesso.** A primeira visita depois disso leva uns
  50 segundos para responder. Não é lentidão do sistema, é a máquina acordando.
- **O banco é em memória.** Cada reinício volta para as 4 pessoas da carga
  inicial. É o esperado nesta prova, e trocar por um Postgres gerenciado mexe em
  quatro linhas do `application.yml`.

---

## Stack

| Camada | Escolha | Por quê |
|---|---|---|
| Linguagem | Java 21 | records e text blocks deixam DTO e configuração enxutos |
| Framework | Spring Boot 3.3.5 | web, validação, segurança e persistência no mesmo lugar |
| Validação | Jakarta Bean Validation | validação declarativa, visível na assinatura do método |
| Persistência | Spring Data JPA + H2 em memória | avaliar o projeto não exige instalar banco |
| Autenticação | Spring Security + JWT (jjwt) | stateless, sem sessão no servidor |
| HTTP externo | `RestClient` (Spring 6.1) | nativo, sem dependência a mais |
| Documentação | springdoc-openapi | Swagger UI para testar sem Postman |
| Testes | JUnit 5, MockMvc, Mockito | 48 testes cobrindo rota, validação, papel e erro |

---

## Endpoints

O parâmetro identificador escolhido foi o **documento (CPF)**.

> **Por que CPF e não um id sequencial?** Porque é chave natural e única da
> pessoa e porque permite uma validação de tipo de dado de verdade — formato
> **e** dígito verificador — em vez de apenas checar se um número é número.
> Aceita com ou sem máscara: `529.982.247-25` e `52998224725` são a mesma pessoa.

| Método | Rota do enunciado | Equivalente REST | Autenticação | Validação de tipo de dado |
|---|---|---|---|---|
| `POST` | `/auth/login` | — | pública | usuário e senha obrigatórios |
| `POST` | `/registrarName` | `/pessoas` | token | CPF com DV, nome/sobrenome só com letras, e-mail em formato válido, nada nulo ou em branco |
| `GET` | `/list` | `/pessoas` | token | filtro opcional `nome` com mínimo de 2 caracteres |
| `GET` | `/list/{documento}` | `/pessoas/{documento}` | token | CPF do path validado antes de tocar no banco |
| `DELETE` | `/list/{documento}` | `/pessoas/{documento}` | token + **perfil ADMIN** | mesma validação de CPF do path |
| `GET` | `/findNacionalityByPerson/{documento}` | `/pessoas/{documento}/nacionalidade` | token | CPF do path + nome validado antes da chamada externa |

> **Por que dois endereços por operação?** As rotas do enunciado são requisito
> escrito e continuam valendo exatamente como pedidas — a interface web usa elas.
> Ao lado, exponho o desenho REST convencional, porque `registrarName` cadastra
> uma pessoa inteira e `list` no singular identifica um recurso: nenhum dos dois
> nomes descreve o que a rota faz.
>
> Não há duplicação de lógica — é o mesmo método atendendo por dois caminhos. A
> exigência de perfil ADMIN no `DELETE` cobre os dois, senão a rota REST viraria
> porta dos fundos. Existe teste automatizado provando isso.

### Códigos de resposta

| Código | Quando |
|---|---|
| `200` / `201` / `204` | sucesso |
| `400` | dado ou parâmetro que não passou na validação |
| `401` | token ausente, inválido ou expirado |
| `403` | token válido, mas o perfil não permite a operação |
| `404` | CPF válido, porém não registrado |
| `409` | tentativa de registrar um documento que já existe |
| `502` | a API externa de nacionalidade caiu ou estourou o timeout |

---

## Autenticação

O enunciado permitia proteger apenas a API mais crítica. A escolha aqui foi
proteger **todas** e colocar um nível a mais na crítica:

- toda rota de negócio exige `Authorization: Bearer <token>`;
- **`DELETE /list/{documento}` exige, além do token, o perfil `ADMIN`.** É a
  única operação destrutiva e irreversível do sistema. Nela, autenticar não
  basta: precisa autorizar.

Ficam abertas apenas o login, a interface web, o Swagger e o console do H2.

O segredo do JWT e as credenciais vêm de variável de ambiente
(`APP_JWT_SECRET`, `APP_ADMIN_SENHA`, `APP_USER_SENHA`). Os valores do
`application.yml` são apenas o fallback para o projeto subir com um comando na
avaliação.

### Exemplo com curl

```bash
# 1. Obter o token
TOKEN=$(curl -s -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"usuario":"admin","senha":"admin123"}' | jq -r .token)

# 2. Listar
curl -s http://localhost:8080/list -H "Authorization: Bearer $TOKEN"

# 3. Registrar
curl -s -X POST http://localhost:8080/registrarName \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"documento":"168.995.350-09","nome":"Akira","sobrenome":"Yamamoto","email":"akira@exemplo.com.br"}'

# 4. Prever a nacionalidade
curl -s http://localhost:8080/findNacionalityByPerson/529.982.247-25 \
  -H "Authorization: Bearer $TOKEN"

# 5. Excluir (só o admin consegue)
curl -s -X DELETE http://localhost:8080/list/168.995.350-09 \
  -H "Authorization: Bearer $TOKEN"
```

---

## O detalhe do código ISO

O enunciado avisa que a API externa devolve a nacionalidade em **código ISO** e
pede o **nome** da possível nacionalidade. Então a conversão é feita antes de
responder, usando a base de locales da própria JDK — sem dependência extra e sem
tabela mantida à mão:

```java
Locale.of("", "US").getDisplayCountry(Locale.of("pt", "BR"));  // "Estados Unidos"
```

A resposta devolve os dois: o nome traduzido e o código de origem, porque
esconder o dado bruto de quem consome a API não ajuda ninguém.

```json
{
  "documento": "529.982.247-25",
  "nomeCompleto": "Nathaniel Barbosa",
  "nomeConsultado": "Nathaniel",
  "nacionalidade": "Estados Unidos",
  "codigoIso": "US",
  "probabilidade": 0.42,
  "alternativas": [
    { "nacionalidade": "Brasil", "codigoIso": "BR", "probabilidade": 0.18 }
  ]
}
```

Dois casos de borda tratados:

- **nome sem previsão** — a API devolve `country: []`. Isso não é erro, é ausência
  de dado: responde `200` com `nacionalidade: null`.
- **API externa fora do ar ou lenta** — timeout de 3s para conectar e 5s para ler.
  Estourou, responde `502` identificando o fornecedor, nunca um `500` genérico.
  Uma lentidão da nationalize.io não pode derrubar esta API junto.

---

## Interface web

`http://localhost:8080` — HTML, CSS e JS puros, servidos pelo próprio Spring
Boot. Sem framework e sem etapa de build: um comando sobe a API e a tela juntas,
na mesma origem, sem CORS no meio.

São duas telas. A primeira é o **login** — nada do sistema aparece antes de
existir um token válido. Depois de autenticar, entra a aplicação.

Quem decide qual tela aparece é a existência do token, nunca um clique de
navegação: sair, ou o servidor responder `401` no meio do uso, devolve para o
login na hora.

A tela consome **todos** os endpoints, não apenas um:

- registra pessoa, com máscara de CPF e o erro de validação aparecendo no campo
  que quebrou — o mapa `erros` do backend é espelhado na tela;
- lista e filtra por nome;
- prevê a nacionalidade, com a barra de probabilidade e as alternativas;
- exclui — e o botão aparece desabilitado quando o perfil logado não é `ADMIN`.

---

## Testes

```bash
mvn test
```

| Arquivo | O que cobre |
|---|---|
| `CpfValidatorTest` | dígito verificador, máscara, CPFs de dígito repetido, nulo |
| `NacionalidadeServiceTest` | conversão ISO → nome, código inexistente, código ausente |
| `PessoaApiIntegrationTest` | as 5 rotas de ponta a ponta, nos dois endereços: 401 sem token, 403 por perfil, 400 de validação, 404, 409, 502 e a conversão do ISO |

A API externa é substituída por um dublê nos testes. Teste que depende de
internet não é teste, é loteria.

---

## Estrutura

```
src/main/java/br/com/luansantos/pessoas/
├── config/        segurança, cliente HTTP externo, OpenAPI
├── domain/        entidade JPA e repositório
├── dto/           contrato de entrada e saída (records)
├── exception/     exceções de negócio
├── integration/   cliente da nationalize.io
├── security/      emissão e leitura do JWT
├── service/       regras de negócio
├── validation/    a anotação @Cpf e sua validação
└── web/           controllers e o tratamento global de erro
```

A entidade JPA nunca sai do service: o que trafega na API são os records de
`dto/`. Trocar H2 por Postgres mexe em 4 linhas do `application.yml` e em nada
mais.

Todo erro sai por um único lugar — `GlobalExceptionHandler` — no formato
RFC 7807 (`ProblemDetail`), com um campo `erros` detalhando campo a campo o que
quebrou. `400 Bad Request` sem dizer qual campo falhou não ajuda quem consome.

---

## Duas observações sobre o enunciado

1. **"Para os dois pontos a seguir"**, mas só um endpoint é listado depois. Segui
   o que está escrito e implementei apenas o `/findNacionalityByPerson/{param}`.
2. **Os nomes das rotas** (`/registrarName` para registrar uma pessoa inteira,
   mistura de português e inglês) foram mantidos exatamente como no enunciado.
   Em projeto próprio eu proporia `POST /pessoas`, mas requisito escrito não se
   reescreve por gosto.

---

## Se fosse para produção

O que ficou de fora de propósito, por ser prova e não sistema em produção:

- **Banco real** — H2 em memória perde tudo no restart. Em produção: Postgres com
  Flyway versionando o schema, em vez de `ddl-auto`.
- **Usuários no banco** — hoje são dois usuários em memória. Em produção: tabela
  de usuários, refresh token e revogação.
- **CPF em URL** — CPF é dado pessoal e path de requisição costuma cair em log de
  servidor. Sob LGPD, o certo seria expor um id opaco para fora e manter o CPF
  como chave interna. Mantive o CPF na URL porque o enunciado pede um parâmetro
  identificador da pessoa e ele é o que permite a validação mais rica.
- **Cache** — a previsão de nacionalidade por nome muda pouco. Um cache de curta
  duração cortaria a maior parte das chamadas externas.
