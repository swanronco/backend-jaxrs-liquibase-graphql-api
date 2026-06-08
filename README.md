# Backend JAX-RS + Liquibase + GraphQL (Java 17)

API d'authentification GraphQL packagée en **WAR** pour **Tomcat 10.1** (Jakarta EE 10). Elle expose l'inscription, la connexion (email ou username) et la déconnexion via JWT, avec persistance **PostgreSQL** (JPA/Hibernate), migrations de schéma **Liquibase** et hachage de mot de passe **BCrypt**.

C'est le backend du frontend [`frontend-nextjs-graphql`](../frontend-nextjs-graphql) : ce dernier consomme l'endpoint `/api/graphql` exposé ici.

## 🧱 Stack

- **Java 17**, build **Gradle** (plugin `war`)
- **JAX-RS** via **Jersey 3.1** (servlet container), tournant sur **Tomcat 10.1 (JDK 17)**
- **GraphQL** via **graphql-java 21**
- **JPA / Hibernate 6** (unité de persistance `RESOURCE_LOCAL`)
- **PostgreSQL** (driver 42.7.3)
- **Liquibase 4.24** (migrations de schéma)
- **jBCrypt** (hachage des mots de passe) + **jjwt 0.11.5** (JWT, HS512)
- Tests : **JUnit 5** + **Testcontainers** (PostgreSQL)
- Conteneurisation **Docker** (Tomcat), orchestration **Kubernetes** via **Kustomize**, CI/CD **GitHub Actions** → image sur **GHCR**

## 🌐 Endpoint

Un unique point d'entrée GraphQL, en `POST`, attendant un corps JSON `{ "query": "...", "variables": {...} }`.

Le servlet Jersey est mappé sur `/api/*` (`web.xml`) et la ressource GraphQL est `@Path("/graphql")`, soit le chemin **`/api/graphql`** dans le contexte de l'application.

Comme le WAR est déployé en **`ROOT.war`** (voir `Dockerfile`), le contexte applicatif est la racine `/`, donc l'URL complète est :

```
http://<host>:8080/api/graphql
```

> ℹ️ Le `CorsFilter` (déclaré dans `web.xml`) autorise toutes les origines, les méthodes usuelles et l'en-tête `Authorization`, et répond directement aux requêtes préflight `OPTIONS`.

## 🔧 Schéma GraphQL

Défini dans `src/main/resources/graphql/user.graphqls` :

```graphql
type Mutation {
  createUser(input: UserInput!): CreateUserResponse!
  login(identifier: String!, password: String!): AuthPayload!
  logout(token: String!): LogoutResponse!
}

type Query {
  user(id: ID!): User
  users: [User!]!
}

input UserInput {
  firstName: String!
  lastName: String!
  email: String!
  username: String!
  password: String!
}

type AuthPayload { token: String!  user: User! }
type CreateUserResponse { firstName: String!  lastName: String!  email: String!  username: String! }
type LogoutResponse { success: Boolean!  message: String! }

type User {
  id: ID!
  firstName: String!
  lastName: String!
  email: String!
  username: String!
  passwordHash: String!
}
```

Comportement des résolveurs :

- **`createUser`** — hache le `password` reçu avec **BCrypt** puis persiste l'utilisateur. Les violations d'unicité sont traduites en messages clairs (« Cet email est déjà utilisé. », « Ce nom d'utilisateur est déjà pris. »).
- **`login`** — recherche l'utilisateur par **username OU email**, vérifie le mot de passe avec `BCrypt.checkpw`, et renvoie un **JWT** (HS512) accompagné de l'utilisateur. Identifiants invalides → erreur `Invalid credentials`.
- **`logout`** — place le token dans une **liste noire en mémoire** (jusqu'à son expiration), purgée quotidiennement par une tâche planifiée.
- **`user` / `users`** — lecture par id ou liste complète.

## 🔐 Authentification (JWT)

- Les tokens sont signés en **HS512** (`io.jsonwebtoken`).
- La clé et la durée de vie sont lues depuis l'environnement, avec repli sur `application-local.properties` :
  - `JWT_SECRET` (sinon `jwt.secret`)
  - `JWT_EXPIRATION` en millisecondes (sinon `jwt.expiration`, défaut **86 400 000** = 24 h)
- Le token se transmet via l'en-tête `Authorization: Bearer <JWT>`. `JwtAuthenticationFilter` le résout, vérifie la liste noire et construit un `GraphQLContext` authentifié/anonyme.
- Mots de passe stockés **uniquement hachés** (BCrypt) dans la colonne `password_hash`.

## 🗄️ Données & migrations

Entité JPA `User` → table **`users`**. Le schéma est géré par **Liquibase** (`src/main/resources/db/changelog/db.changelog-master.xml`) :

- changeset `1` : création de `users` (`id`, `first_name`, `last_name`, `email` unique, `username` unique, `password_hash`)
- changeset `2` : contraintes d'unicité `uq_users_email` et `uq_users_username`

Connexion par défaut (dev) : `jdbc:postgresql://localhost:5432/testdb`, user `postgres` / `password`.

> ⚠️ La persistence unit a aussi `hibernate.hbm2ddl.auto=update`. Hibernate (auto-DDL) **et** Liquibase touchent donc tous deux au schéma : en production, privilégier Liquibase comme source de vérité et désactiver l'auto-DDL.

## 🚀 Build & exécution

Prérequis : JDK 17 et une base PostgreSQL accessible (les tests utilisent Docker via Testcontainers).

```bash
# Construire le WAR -> build/libs/jaxrs-liquibase-graphql-api.war
./gradlew war

# Lancer les tests (JUnit 5 + Testcontainers, nécessite Docker)
./gradlew test

# Appliquer les migrations Liquibase (plugin gradle, runList "main")
./gradlew update
```

### Déploiement local (Podman)

`deploy-local.sh` construit le WAR, l'image, puis lance un conteneur Tomcat sur le port **8080** (réseau/conteneur `local`, variables `DB_*` et `JWT_SECRET` injectées) :

```bash
./deploy-local.sh
# -> http://localhost:8080/api/graphql
```

## 🐳 Docker

```dockerfile
FROM tomcat:10.1-jdk17
COPY build/libs/jaxrs-liquibase-graphql-api.war /usr/local/tomcat/webapps/ROOT.war
```

Le WAR est servi à la **racine** (`ROOT.war`), d'où l'URL `…:8080/api/graphql` (sans préfixe de contexte). Construire l'image après `./gradlew war` :

```bash
./gradlew war
docker build -t ghcr.io/swanronco/backend-jaxrs-liquibase-graphql-api:latest .
docker run -p 8080:8080 \
  -e DB_URL=jdbc:postgresql://<host>:5432/testdb \
  -e DB_USER=postgres -e DB_PASSWORD=password \
  -e JWT_SECRET=<clé-de-64-octets-min> \
  ghcr.io/swanronco/backend-jaxrs-liquibase-graphql-api:latest
```

## ☸️ Kubernetes (Kustomize)

Manifests dans `k8s/`, namespace **`backend`** :

```
k8s/base/        deployment (port 8080), service (:8080), hpa (3→6 pods @80% CPU),
                 serviceaccount, namespace, resourcequota, limitrange
k8s/overlays/dev 1 replica, ressources réduites, Secret app-secret (valeurs de dev)
k8s/overlays/prod 3 replicas, stratégie maxUnavailable, Secret app-secret (prod)
```

Le déploiement lit `DB_URL`, `DB_USER`, `DB_PASSWORD` et `JWT_SECRET` depuis le `Secret` **`app-secret`**.

```bash
kustomize build k8s/overlays/dev     # validation locale
kubectl apply -k k8s/overlays/dev
kubectl apply -k k8s/overlays/prod
```

Image déployée : `ghcr.io/swanronco/backend-jaxrs-liquibase-graphql-api` (tag `latest`, fixé dans chaque overlay).

## 🔁 CI/CD (GitHub Actions)

Workflow `.github/workflows/deploy.yml`, sur push et PR vers `main` :

- **validate-k8s** — `kustomize build` des overlays dev et prod.
- **test** — `./gradlew test` (JDK 17 Temurin, cache Gradle).
- **build-and-push** — sur `main` : `./gradlew war` puis build/push de l'image sur **GHCR** (tags `latest` + SHA).
- **deploy** — étape `kubectl rollout restart` présente mais **commentée** (à activer avec un secret `KUBECONFIG`).

## 📁 Structure

```
src/main/java/com/example/
  api/JerseyServlet.java                    # ResourceConfig Jersey (+ filtre JWT)
  graphql/
    GraphQLEndpoint.java                    # ressource JAX-RS POST /api/graphql
    GraphQLProvider.java                    # chargement du schéma + wiring
    DateTimeScalar.java                     # scalaire personnalisé
    datafetcher/UserQueryDataFetcher.java   # user / users
    datafetcher/UserMutationDataFetcher.java# createUser (hash BCrypt)
    resolver/UserMutationResolver.java      # login / logout (+ blacklist tokens)
  service/UserService.java, AuthService.java
  user/UserRepository.java                  # accès JPA (find / save)
  model/User.java                           # entité @Table(users)
  filter/CorsFilter.java                    # CORS (web.xml)
  filter/JwtAuthenticationFilter.java       # contexte auth depuis Bearer
  context/GraphQLContext.java
  util/JwtUtil.java, JwtResolver.java, JpaUtil.java
  dto/CreateUserResponse.java, LogoutResponse.java
src/main/resources/
  graphql/user.graphqls                     # schéma GraphQL
  db/changelog/db.changelog-master.xml      # migrations Liquibase
  META-INF/persistence.xml                  # unité de persistance JPA
src/main/webapp/WEB-INF/web.xml             # mapping servlet /api/* + CorsFilter
src/test/java/...UserRepositoryTest.java    # test Testcontainers
Dockerfile, deploy-local.sh, build.gradle, liquibase.properties
k8s/                                        # manifests Kubernetes
.github/workflows/deploy.yml                # CI/CD
```

## 🔒 Notes sécurité

- **Secrets versionnés** : `k8s/overlays/*/secret.yaml` et `deploy-local.sh` contiennent des valeurs `JWT_SECRET` / mots de passe **en clair**. Ce sont des valeurs de **dev** ; pour la prod, externaliser via un vrai gestionnaire de secrets (Sealed Secrets, External Secrets, SOPS…) et ne jamais committer les secrets réels.
- **CORS** : `Access-Control-Allow-Origin: *` combiné à `Allow-Credentials: true` est permissif ; restreindre l'origine en production.
- **JWT** : la clé HS512 doit faire au moins 64 octets. La déconnexion repose sur une **liste noire en mémoire** — elle n'est ni partagée entre instances ni persistée (un token reste valide sur les autres pods jusqu'à expiration). Pour un cluster multi-replicas, envisager un store partagé (Redis) ou des tokens courts.
- **Schéma** : voir la note sur le doublon Hibernate auto-DDL / Liquibase ci-dessus.
```
