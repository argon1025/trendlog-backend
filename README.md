# trendlog-backend

trendlog 서비스의 백엔드 API 서버예요.

현재는 초기 골격 단계예요. Spring Boot 애플리케이션 진입점(`ApiApplication`)만 있고 도메인 코드는 아직 없어요.

## 기술 스택

| 항목 | 버전 / 사용 기술 |
|---|---|
| 언어 | Java 25 (`25.0.3-amzn`) |
| 프레임워크 | Spring Boot 4.0.7 |
| 빌드 도구 | Maven (프로젝트에 포함된 `mvnw` 래퍼 사용) |
| 데이터베이스 | MySQL (`mysql-connector-j`) |
| 영속성 | Spring Data JPA |
| 웹 | Spring WebMVC |
| 검증 | Jakarta Bean Validation |
| API 문서 | Swagger UI (springdoc-openapi 3.0.3) |
| 외부 연동 | 한국투자증권 OpenAPI (Spring `RestClient` 선언형 클라이언트) |
| 보일러플레이트 축소 | Lombok |
| 코드 포매터 | spotless 3.9.0 + palantir-java-format 2.96.0 (`PALANTIR` 스타일) |

## 사전 준비

Java 버전은 `.sdkmanrc`에 고정해 뒀어요. SDKMAN을 쓰면 프로젝트 루트에서 한 줄로 맞출 수 있어요.

```bash
sdk env
```

`sdk env install`은 해당 버전이 로컬에 없을 때 먼저 받아와요.

## 로컬 실행

### 1. dev 프로파일 설정 파일 만들기

프로파일별 설정 파일에는 자격증명이 들어 있어서 저장소에 올리지 않아요. 그래서 clone한 직후에는 `application-dev.yaml`이 없고, 견본을 복사해서 직접 만들어야 해요.

```bash
cp src/main/resources/application.example.yml \
   src/main/resources/application-dev.yaml
```

복사한 파일의 `kis.app-key`와 `kis.app-secret`을 한국투자증권 홈페이지에서 발급받은 실제 값으로 바꿔 주세요. 값이 비어 있으면 `KisProperties` 검증이 걸려 기동 시점에 바로 실패해요. 데이터베이스 접속 정보는 아래 컨테이너 설정과 맞춰 둔 값이라 그대로 쓰면 돼요.

### 2. MySQL 컨테이너 기동

```bash
docker compose up -d
```

호스트 포트는 **3310**이에요. 컨테이너 안 MySQL은 기본 3306을 그대로 써요. 접속 계정은 `compose.yaml`에 적혀 있어요(로컬 전용 `trendlog` / `trendlog`). 데이터는 프로젝트 안 `./data/`에 쌓이고, git에는 올라가지 않아요.

### 3. 애플리케이션 실행

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

기본 포트는 8080이에요. 기본 프로파일이 없어서 `dev`든 `prod`든 프로파일을 반드시 명시해야 해요. 명시하지 않으면 데이터소스 설정이 없어 기동에 실패해요. `spring-boot-devtools`가 들어 있어서 클래스가 바뀌면 자동으로 다시 시작해요.

### 4. Swagger 접속

`http://localhost:8080/swagger-ui.html`을 열면 API 명세가 보여요. 원본 OpenAPI 문서는 `http://localhost:8080/v3/api-docs`예요.

## 코드 스타일

Java 코드 서식은 spotless가 자동으로 맞춰요. 규칙은 palantir-java-format의 `PALANTIR` 스타일이고, 들여쓰기는 4칸 공백, 한 줄은 120컬럼까지예요. import 순서 정리와 쓰지 않는 import 제거도 함께 처리해요.

`spotless:apply`가 Maven `process-sources` 단계에 묶여 있어서, 아래 명령 중 무엇을 돌려도 소스가 정리돼요.

```bash
./mvnw compile
./mvnw test
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

서식이 맞는지만 확인하고 싶으면 다음 명령을 쓰세요. 어긋난 파일이 있으면 목록을 보여 주고 실패해요.

```bash
./mvnw spotless:check
```

편집기 쪽 들여쓰기는 `.editorconfig`가 맞춰 줘요.

## 운영 실행

`prod` 프로파일은 데이터베이스 접속 정보를 기본값 없이 환경변수로 읽어요. 아래 세 개를 반드시 주입해 주세요. 하나라도 빠지면 기동이 실패해요.

| 환경변수 | 설명 |
|---|---|
| `DB_URL` | JDBC 접속 URL |
| `DB_USERNAME` | DB 계정 |
| `DB_PASSWORD` | DB 비밀번호 |

KIS 자격증명은 dev와 마찬가지로 `application-prod.yaml` 안에 직접 적어요. `application-prod.yaml`도 저장소에 없어서 운영 환경에서 직접 만들어야 해요. 견본은 `application.example.yml`을 그대로 쓰고, 데이터베이스 항목은 위 환경변수를 참조하는 형태로 바꿔 주세요.

```yaml
kis:
  app-key: 발급받은-appkey
  app-secret: 발급받은-appsecret
```

Swagger는 `prod`에서 꺼져 있어요. 인증 없는 API 명세가 외부에 노출되지 않게 하려고요.

## 테스트

```bash
./mvnw test
```

테스트는 Testcontainers로 MySQL 8.4 컨테이너를 직접 띄워요. Docker 데몬이 실행 중이어야 해요.

## 디렉터리 구조

```
src/main/java/io/trendlog/api/   애플리케이션 코드
src/main/resources/              공통 설정 (application.yaml)과 설정 견본 (application.example.yml)
                                 프로파일별 application-{dev,prod}.yaml은 자격증명을 담아 git에서 제외
compose.yaml                     로컬 개발용 MySQL 컨테이너 정의
src/test/java/io/trendlog/api/   테스트 코드
.harness/                        개발 하네스 (docs = 프로젝트 위키, workspace = 브랜치별 작업 기록)
.sdkmanrc                        SDKMAN이 읽는 Java 버전 고정 파일
```

## 참고 문서

- [Spring Boot Maven Plugin](https://docs.spring.io/spring-boot/4.0.7/maven-plugin)
- [Spring Web MVC](https://docs.spring.io/spring-boot/4.0.7/reference/web/servlet.html)
- [Spring Data JPA](https://docs.spring.io/spring-boot/4.0.7/reference/data/sql.html#data.sql.jpa-and-spring-data)
- [springdoc-openapi](https://springdoc.org/)
- [Validation](https://docs.spring.io/spring-boot/4.0.7/reference/io/validation.html)
- [OCI 이미지 생성](https://docs.spring.io/spring-boot/4.0.7/maven-plugin/build-image.html)
