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
| API 문서 | Spring REST Docs + Asciidoctor |
| 보일러플레이트 축소 | Lombok |

## 사전 준비

Java 버전은 `.sdkmanrc`에 고정해 뒀어요. SDKMAN을 쓰면 프로젝트 루트에서 한 줄로 맞출 수 있어요.

```bash
sdk env
```

`sdk env install`은 해당 버전이 로컬에 없을 때 먼저 받아와요.

## 실행

```bash
./mvnw spring-boot:run
```

기본 포트는 8080이에요. `spring-boot-devtools`가 들어 있어서 클래스가 바뀌면 자동으로 다시 시작해요.

## 테스트

```bash
./mvnw test
```

## API 문서 생성

테스트가 REST Docs 스니펫을 만들고, `prepare-package` 단계에서 Asciidoctor가 그 스니펫을 HTML로 바꿔요.

```bash
./mvnw package
```

결과물은 `target/generated-docs/`에 생겨요.

## 디렉터리 구조

```
src/main/java/io/trendlog/api/   애플리케이션 코드
src/main/resources/              설정 파일 (application.yaml)
src/test/java/io/trendlog/api/   테스트 코드
.harness/                        개발 하네스 (docs = 프로젝트 위키, workspace = 브랜치별 작업 기록)
.sdkmanrc                        SDKMAN이 읽는 Java 버전 고정 파일
```

## 참고 문서

- [Spring Boot Maven Plugin](https://docs.spring.io/spring-boot/4.0.7/maven-plugin)
- [Spring Web MVC](https://docs.spring.io/spring-boot/4.0.7/reference/web/servlet.html)
- [Spring Data JPA](https://docs.spring.io/spring-boot/4.0.7/reference/data/sql.html#data.sql.jpa-and-spring-data)
- [Spring REST Docs](https://docs.spring.io/spring-restdocs/docs/current/reference/htmlsingle/)
- [Validation](https://docs.spring.io/spring-boot/4.0.7/reference/io/validation.html)
- [OCI 이미지 생성](https://docs.spring.io/spring-boot/4.0.7/maven-plugin/build-image.html)
