# Docker Compose(MySQL) · 환경별 설정 · Swagger 도입

## Context

`trendlog-backend`는 Spring Initializr 스캐폴드 상태예요. 소스는 `ApiApplication.java`와 `ApiApplicationTests.java` 두 개뿐이고, `src/main/resources/application.yaml`에는 `spring.application.name`만 있어요.

여기서 세 가지 문제가 걸려 있어요.

- `./mvnw test`가 실패해요. `spring-boot-starter-data-jpa`가 기동할 때 데이터소스를 요구하는데 접속 설정이 비어 있어서 `ApiApplicationTests.contextLoads`가 `Failed to determine a suitable driver class`로 죽어요.
- 로컬에서 쓸 MySQL이 없어요. `mysql-connector-j`만 있고 컨테이너 정의가 없어요.
- API 명세 도구가 정해지지 않았어요. 스캐폴드에 딸려 온 Spring REST Docs 의존성이 남아 있는데, 실제로는 Swagger를 쓰기로 했어요.

이 작업이 끝나면 `docker compose up -d` 한 번과 `./mvnw spring-boot:run` 한 번으로 로컬 개발이 시작되고, `./mvnw test`가 통과하고, `/swagger-ui.html`에서 API 명세를 볼 수 있어요. 대응 이슈는 [argon1025/trendlog-backend#2](https://github.com/argon1025/my-claude-plugin-market/issues/2)예요.

## 필독 자료

`.harness/docs/` 위키는 아직 비어 있어요. 대신 이전 브랜치 피드백을 먼저 읽어주세요.

- `.harness/workspace/progress/feat-project-scaffold/feedback.md` — 테스트 실패의 원인과, README에서 MySQL 접속 안내를 일부러 뺀 이유가 적혀 있어요.

## 사전에 확정한 사실

탐색 중 Maven Central에서 직접 확인한 값이에요. 추측이 아니니 그대로 쓰면 돼요.

- 프로젝트는 **Maven**이에요. Gradle 파일은 없어요. 빌드는 `./mvnw`로 해요.
- Spring Boot **4.0.7**, Java **25** (`.sdkmanrc`에 `25.0.3-amzn` 고정).
- Boot 4 계열이라 스타터 이름이 3.x와 달라요. `spring-boot-starter-webmvc`(`-web` 아님), 테스트는 `spring-boot-starter-webmvc-test` 식으로 짝을 이뤄요.
- Swagger는 **springdoc-openapi 3.0.3**을 써요. Boot 4를 지원하는 첫 메이저 버전이에요. 2.x는 Boot 3 전용이라 여기서는 안 떠요.
- Testcontainers는 Boot 4.0.7이 **2.0.5**를 관리해요. 1.x와 좌표가 달라요.
  - `org.testcontainers:testcontainers-mysql` (1.x의 `org.testcontainers:mysql` 아님)
  - `org.testcontainers:testcontainers-junit-jupiter` (1.x의 `org.testcontainers:junit-jupiter` 아님)
  - 컨테이너 클래스는 `org.testcontainers.mysql.MySQLContainer`를 써요. `org.testcontainers.containers.MySQLContainer`도 jar에 남아 있지만 구 패키지예요.
  - 버전은 부모 POM이 관리하니 `<version>`을 적지 마세요.
- `.gitignore`에 `.vscode/`가 이미 들어 있어요. 실행 구성을 커밋하려면 예외 규칙이 필요해요.
- `.gitignore`에 `.env` 항목이 없어요. 추가해야 해요.

## 확정된 결정 사항

사용자가 직접 고른 항목이에요. 그대로 따라주세요.

1. **테스트 DB는 Testcontainers MySQL**을 써요. 운영과 같은 엔진이라 방언 차이로 인한 오탐이 없어요. H2와 로컬 컨테이너 직접 접속은 기각했어요.
2. **로컬 개발 컨테이너는 수동으로 띄워요.** `docker compose up -d`를 직접 실행하고, `application-dev.yaml`에 `localhost:3306` 접속정보를 고정해요. `spring-boot-docker-compose` 자동 기동은 기각했어요. 접속정보가 설정 파일에 보이는 쪽이 추적하기 쉽고, 앱을 껐다 켜도 컨테이너가 살아 있어요.
3. **prod 설정은 환경변수만 읽고 기본값을 두지 않아요.** `${DB_URL}` 형태로만 써요. 변수 주입을 빠뜨리면 기동 시점에 바로 실패하게 만들어서, 엉뚱한 DB에 붙은 채 정상인 척 뜨는 사고를 막아요.
4. **Swagger는 prod에서 꺼요.** 인증 없는 API 명세와 시험 호출 화면이 외부에 노출되지 않게 해요.

## 미리 정한 값

사용자에게 따로 확인하지 않고 결정한 값이에요. 바꿀 이유가 생기면 그때 물어보세요.

| 항목 | 값 |
|---|---|
| MySQL 이미지 | `mysql:8.4` (LTS) |
| 데이터베이스 이름 | `trendlog` |
| 계정 / 비밀번호 | `trendlog` / `trendlog` (로컬 전용) |
| 포트 | `3306:3306` |
| 문자셋 | `utf8mb4` / `utf8mb4_0900_ai_ci` |
| 기본 활성 프로파일 | `dev` |
| `ddl-auto` | dev는 `update`, prod는 `validate` |
| 브랜치 이름 | `feature/2-docker-swagger` |

## 작업 순서

**작업 시작 전에 `develop`에서 `feature/2-docker-swagger` 브랜치를 만들어주세요.** `develop`에서 직접 커밋하면 안 돼요.

첫 커밋은 이 플랜 스냅샷이에요. 아래 5개는 그다음에 이어져요.

---

### 커밋 1 — `chore: docker compose MySQL 구성 추가`

| 파일 | 작업 |
|---|---|
| `compose.yaml` | 신규. MySQL 8.4 서비스 하나. |
| `.env.example` | 신규. compose가 읽는 변수의 견본. |
| `.gitignore` | `.env` 무시 규칙 추가. |

`compose.yaml` 요점이에요.

- 서비스 이름 `mysql`, 이미지 `mysql:8.4`.
- 환경변수는 `.env`에서 읽어요: `MYSQL_ROOT_PASSWORD`, `MYSQL_DATABASE`, `MYSQL_USER`, `MYSQL_PASSWORD`.
- 이름 있는 볼륨(`mysql-data`)을 `/var/lib/mysql`에 붙여서 컨테이너를 지워도 데이터가 남게 해요.
- `command`로 `--character-set-server=utf8mb4 --collation-server=utf8mb4_0900_ai_ci`를 넘겨요.
- `healthcheck`에 `mysqladmin ping`을 걸어요. 간격 5초, 재시도 10회.

`.env.example`에는 위 네 변수를 로컬 기본값(`trendlog` 계열)으로 적어요. 실제 `.env`는 커밋하지 않아요.

**검증:** `docker compose up -d && docker compose ps` → `mysql` 서비스 상태가 `healthy`로 표시돼요.

---

### 커밋 2 — `feat: dev/prod 프로파일 분리`

| 파일 | 작업 |
|---|---|
| `src/main/resources/application.yaml` | 공통 설정 + 기본 프로파일 `dev` 지정. |
| `src/main/resources/application-dev.yaml` | 신규. 로컬 MySQL 접속정보 고정. |
| `src/main/resources/application-prod.yaml` | 신규. 환경변수 자리표시자만. |

`application.yaml` — 공통만 남겨요.

```yaml
spring:
  application:
    name: api
  profiles:
    default: dev
```

`application-dev.yaml` — 로컬 compose에 맞춘 고정값이에요.

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/trendlog?serverTimezone=Asia/Seoul&characterEncoding=UTF-8
    username: trendlog
    password: trendlog
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        format_sql: true
```

`application-prod.yaml` — 기본값을 절대 넣지 마세요. 값이 없으면 기동이 실패해야 해요.

```yaml
spring:
  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
```

**검증:** `docker compose up -d` 이후 `./mvnw spring-boot:run` → 로그에 `The following 1 profile is active: "dev"`가 찍히고 기동이 끝나요.

---

### 커밋 3 — `test: Testcontainers로 컨텍스트 로드 테스트 복구`

| 파일 | 작업 |
|---|---|
| `pom.xml` | Testcontainers 의존성 3개 추가 (test 스코프). |
| `src/test/java/io/trendlog/api/support/AbstractIntegrationTest.java` | 신규. 컨테이너를 띄우는 공통 부모. |
| `src/test/java/io/trendlog/api/ApiApplicationTests.java` | 위 부모를 상속하도록 수정. |

`pom.xml`에 추가할 의존성이에요. 셋 다 `<scope>test</scope>`이고 `<version>`은 적지 않아요.

- `org.springframework.boot:spring-boot-testcontainers`
- `org.testcontainers:testcontainers-mysql`
- `org.testcontainers:testcontainers-junit-jupiter`

`AbstractIntegrationTest`는 이렇게 잡아요.

- `@SpringBootTest`, `@Testcontainers`를 붙여요.
- `static` 필드로 `MySQLContainer<?>`를 하나 두고 `@Container`, `@ServiceConnection`을 붙여요. `static`이라 테스트 클래스가 늘어나도 컨테이너는 하나만 떠요.
- 이미지는 compose와 같은 `mysql:8.4`를 써요.
- import는 `org.testcontainers.mysql.MySQLContainer`예요.
- `@ServiceConnection`이 접속정보를 자동으로 주입하니 `@DynamicPropertySource`는 필요 없어요.

`ApiApplicationTests`에서는 `@SpringBootTest`를 떼고 `AbstractIntegrationTest`를 상속해요. `contextLoads()` 본문은 그대로 둬요.

**검증:** `./mvnw test` → `Tests run: 1, Failures: 0, Errors: 0`. Docker 데몬이 떠 있어야 해요.

---

### 커밋 4 — `feat: Swagger(springdoc) 도입 및 REST Docs 제거`

| 파일 | 작업 |
|---|---|
| `pom.xml` | springdoc 추가, REST Docs 관련 3개 제거. |
| `src/main/java/io/trendlog/api/config/OpenApiConfig.java` | 신규. 문서 메타정보. |
| `src/main/resources/application-dev.yaml` | springdoc 경로 설정 추가. |
| `src/main/resources/application-prod.yaml` | springdoc 비활성 설정 추가. |
| `README.md` | 실행 방법과 Swagger 주소 안내 추가. |

**제거할 것** — 세 군데 모두 지워요.

- `<dependency>` `org.springframework.boot:spring-boot-starter-restdocs`
- `<dependency>` `org.springframework.restdocs:spring-restdocs-mockmvc`
- `<plugin>` `org.asciidoctor:asciidoctor-maven-plugin` (`generate-docs` 실행 정의 통째로)

**추가할 것** — 버전을 명시해요. 부모 POM이 springdoc을 관리하지 않아요.

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>3.0.3</version>
</dependency>
```

`OpenApiConfig`는 `@Configuration` 클래스 하나에 `OpenAPI` 빈을 등록해요. 제목 `Trendlog API`, 버전 `v1`, 설명 한 줄이면 충분해요.

`application-dev.yaml`에 추가해요.

```yaml
springdoc:
  swagger-ui:
    path: /swagger-ui.html
    tags-sorter: alpha
    operations-sorter: alpha
  api-docs:
    path: /v3/api-docs
```

`application-prod.yaml`에 추가해요.

```yaml
springdoc:
  api-docs:
    enabled: false
  swagger-ui:
    enabled: false
```

README에는 로컬 실행 3단계(compose 기동 → 앱 실행 → Swagger 접속)와 prod 실행에 필요한 환경변수 3개(`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`)를 적어요.

**검증:** `./mvnw clean package`가 성공하고, dev로 기동한 뒤 `curl -s -o /dev/null -w "%{http_code}" localhost:8080/v3/api-docs`가 `200`을 반환해요. prod 프로파일로 띄우면 같은 요청이 `404`예요.

---

### 커밋 5 — `chore: VS Code 실행 구성 추가`

| 파일 | 작업 |
|---|---|
| `.gitignore` | `.vscode/` 무시 예외 추가. |
| `.vscode/launch.json` | 신규. dev / prod 실행 구성. |

`.gitignore`의 기존 `.vscode/` 줄 아래에 예외를 붙여요. 순서가 중요해요. 무시 규칙 뒤에 와야 해요.

```gitignore
.vscode/
!.vscode/launch.json
```

`.vscode/launch.json`에 구성 두 개를 둬요. 둘 다 `type: java`, `request: launch`, `mainClass: io.trendlog.api.ApiApplication`, `projectName: api`예요.

- `Trendlog API (dev)` — `"args": "--spring.profiles.active=dev"`
- `Trendlog API (prod)` — `"args": "--spring.profiles.active=prod"`, 그리고 `"env"`에 `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` 세 개를 자리표시자 문자열로 넣고 "실제 값으로 바꿔 쓰라"는 주석을 달아요.

**검증:** VS Code 실행 패널(Run and Debug)에 구성 두 개가 뜨고, `Trendlog API (dev)`로 실행하면 앱이 8080 포트에 붙어요.

---

## 전체 검증

모든 커밋이 끝난 뒤 아래 순서로 확인해요.

```bash
cp .env.example .env
docker compose up -d
docker compose ps                 # mysql 상태가 healthy
./mvnw clean test                 # Tests run: 1, Failures: 0, Errors: 0
./mvnw spring-boot:run            # 기본 프로파일 dev로 기동
curl -s -o /dev/null -w "%{http_code}\n" localhost:8080/v3/api-docs   # 200
```

브라우저에서 `http://localhost:8080/swagger-ui.html`을 열면 `Trendlog API v1` 문서가 보여요. 아직 컨트롤러가 없어서 엔드포인트 목록은 비어 있는 게 정상이에요.

## feedback.md에 남길 내용

작업 중 알게 된 사실은 `.harness/workspace/progress/feature-2-docker-swagger/feedback.md`에 그때그때 append하고, 해당 커밋에 포함시켜요. 최소한 아래 세 가지는 남겨주세요.

- Spring Boot 4.0.7이 관리하는 Testcontainers는 2.0.5이며, 1.x와 좌표가 다르다는 사실 (`testcontainers-mysql`, `testcontainers-junit-jupiter`).
- springdoc-openapi는 Boot 4에서 3.x를 써야 하며 2.x는 동작하지 않는다는 사실.
- prod 프로파일에 기본값을 두지 않기로 한 이유 (환경변수 누락 시 조용히 로컬 DB에 붙는 사고 방지).

## Re-plan 2026-07-19

워크스루 리뷰에서 나온 지적 1건을 반영해요. **로컬 MySQL의 호스트 포트를 3306에서 3310으로 바꿔요.**

사용자의 다른 사이드 프로젝트가 호스트 3306을 이미 점유하고 있어요. 원래 계획대로 `3306:3306`으로 두면 `docker compose up -d`가 포트 충돌로 실패해요. 컨테이너 안 MySQL은 기본 3306 그대로 두고, 호스트 쪽만 3310으로 매핑해요. 컨테이너 내부 포트까지 바꾸면 MySQL 설정 파일을 따로 건드려야 하는데 얻는 게 없어요.

아래 세 곳을 정정해요. 나머지 내용은 위 원본 그대로 유효해요.

### 1. 「미리 정한 값」 표 — 포트

| 항목 | 값 |
|---|---|
| 포트 | ~~`3306:3306`~~ → **`3310:3306`** (호스트 3310, 컨테이너 3306) |

커밋 1의 `compose.yaml`에 이 매핑을 적용해요.

### 2. 커밋 2 — `application-dev.yaml` 접속 URL

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3310/trendlog?serverTimezone=Asia/Seoul&characterEncoding=UTF-8
    username: trendlog
    password: trendlog
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        format_sql: true
```

### 3. 「전체 검증」 — 접속 확인

`docker compose ps` 결과에서 포트 표기가 `0.0.0.0:3310->3306/tcp`로 나오는지 확인해요. 호스트에서 직접 붙어볼 때도 포트를 명시해요.

```bash
mysql -h 127.0.0.1 -P 3310 -u trendlog -p trendlog
```

### 변경 없는 것

- **커밋 3 (Testcontainers)** — 컨테이너가 실행할 때마다 임의 포트를 잡고 `@ServiceConnection`이 자동으로 꽂아줘요. 호스트 3310과 무관해요.
- **커밋 4, 5** — 앱 포트(8080)와 프로파일 설정은 그대로예요.

### feedback.md 추가 항목

위 3개에 더해 아래 사실도 남겨요.

- 로컬 MySQL 호스트 포트가 3310인 이유 (3306은 사용자의 다른 프로젝트가 점유).
