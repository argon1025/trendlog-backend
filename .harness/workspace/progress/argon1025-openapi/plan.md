# 한국투자증권 OpenAPI 외부 연동 기반 구축 · 접근토큰 발급 API 연동

## Context

`trendlog-backend`는 운영자가 등록한 종목을 세 가지 전략 프로필 관점으로 해석해 주는 서비스예요. 시장 데이터는 전부 한국투자증권 OpenAPI(이하 KIS)에서 받아오기로 했고, 앞으로 다음 흐름을 만들어야 해요.

```
[KIS 어댑터] (향후 US 어댑터)
     ↓
[MarketSync] ─ 정규화 적재 → [시장 데이터 테이블]
     └─ marketsync.* 발행
          ↓                                ↓ (quote·status만 직결)
   [IndicatorEngine]                       │
     └─ indicator.* 발행                    │
          ↓                                ↓
   [TrendStrategy] [MeanRevStrategy] [BreakoutStrategy]
     └─ strategy.{trend|meanrev|breakout}.* 발행
          ↓
   [OverviewComposer] ─ (instrument, snapshot_ver) 단위 팬인
     └─ overview.* 발행
     ↓
[조회 API] — 전 테이블 read-only 조합 → 프론트
```

지금 저장소에는 외부 HTTP 호출 코드가 한 줄도 없어요. 소스는 `ApiApplication`과 `OpenApiConfig` 두 개뿐이고, `RestClient` 자동 구성도 켜져 있지 않아요.

이번 브랜치는 그 첫 칸만 채워요. KIS를 부르는 HTTP 계층을 어떤 모양으로 둘지 정하고, 그 위에서 접근토큰 발급 한 건을 실제로 호출할 수 있게 만들어요. 시세 조회, 토큰 재사용, MarketSync 적재는 모두 다음 브랜치 몫이에요.

## 필독 자료

`.harness/docs/` 위키는 아직 비어 있어요(`wiki_index.py` 실행 결과가 헤더 한 줄뿐이에요). 대신 이전 브랜치 피드백을 먼저 읽어주세요.

- `.harness/workspace/progress/feature-2-docker-swagger/feedback.md` — Spring Boot 4 계열의 스타터 이름 규칙, Testcontainers 2.x 좌표, `prod` 프로파일이 환경변수만 읽는 이유, `.gitignore`에서 부정 규칙(`!`)을 쓸 때의 함정, 주석·테스트 작성 관례가 적혀 있어요.
- `.harness/workspace/progress/chore-vscode-run-profile-setup/feedback.md` — `application-prod.yaml`이 저장소에서 추적되지 않는다는 사실이 적혀 있어요. 이번 작업에서 이 파일을 고치려 들면 안 돼요.

## 사전에 확정한 사실

아래는 탐색 중 `한투OPENAPI.xlsx`, 로컬 Maven 저장소의 실제 jar, Spring Boot 4.0 공식 문서에서 직접 확인한 값이에요. 추측이 아니니 그대로 쓰면 돼요.

### KIS 접근토큰발급 API 명세

`한투OPENAPI.xlsx`의 `접근토큰발급(P)` 시트에서 그대로 옮긴 값이에요.

| 항목 | 값 |
|---|---|
| API ID | 인증-001 |
| HTTP Method | `POST` |
| URL | `/oauth2/tokenP` |
| 실전 Domain | `https://openapi.koreainvestment.com:9443` |
| 모의 Domain | `https://openapivts.koreainvestment.com:29443` |
| 요청 헤더 | 별도 필수 헤더 없어요. `tr_id`도 없어요. |

요청 본문은 세 개예요. 전부 필수예요.

| Element | Type | 설명 |
|---|---|---|
| `grant_type` | string | 고정값 `client_credentials` |
| `appkey` | string | KIS 홈페이지에서 발급받은 appkey |
| `appsecret` | string | KIS 홈페이지에서 발급받은 appsecret |

응답 본문은 네 개예요. 전부 필수예요.

| Element | Type | 설명 |
|---|---|---|
| `access_token` | string | 접근토큰 |
| `token_type` | string | 고정값 `Bearer` |
| `expires_in` | number | 유효기간(초). 예시는 `86400`이에요. |
| `access_token_token_expired` | string | 유효기간 일시. `"2023-12-22 08:16:59"` 형식이에요. |

실제 응답 예시는 이래요.

```json
{
	"access_token": "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxMiJ9...",
	"access_token_token_expired": "2023-12-22 08:16:59",
	"token_type": "Bearer",
	"expires_in": 86400
}
```

토큰 수명 규칙도 시트에 적혀 있어요. 접근토큰 유효기간은 24시간이고 1일 1회 발급이 원칙이에요. 갱신발급주기는 6시간이라서, 6시간 안에 다시 호출하면 새 토큰이 아니라 직전 토큰이 그대로 돌아와요.

**실패 응답 규격은 시트에 없어요.** `접근토큰발급(P)` 시트에는 성공 응답 레이아웃만 있고 오류 본문 규격이 실려 있지 않아요. 그래서 이번 작업에서는 실패 본문을 파싱하는 DTO를 만들지 않아요(뒤의 "설계 결정" 3번 참고).

### Spring Boot 4.0.7의 HTTP 클라이언트 지원

`~/.m2` 안의 실제 jar를 열어 확인했어요.

- `spring-boot-starter-webmvc`는 `RestClient` 자동 구성을 **가져오지 않아요.** 이 스타터가 끌고 오는 건 `spring-boot-starter`, `spring-boot-starter-jackson`, `spring-boot-starter-tomcat`, `spring-boot-http-converter`, `spring-boot-webmvc` 다섯 개예요.
- `RestClient.Builder` 자동 구성과 선언형 클라이언트 지원은 별도 모듈 **`org.springframework.boot:spring-boot-restclient`**에 들어 있어요. 이 모듈이 `spring-boot-http-client`를 함께 끌고 와요. Boot 부모 POM이 버전을 관리하므로 `<version>`을 적지 마세요.
- 이 모듈이 등록하는 자동 구성은 `RestClientAutoConfiguration`, `RestClientObservationAutoConfiguration`, `RestTemplateAutoConfiguration`, `RestTemplateObservationAutoConfiguration`, `HttpServiceClientAutoConfiguration` 다섯 개예요. 마지막 것이 선언형 클라이언트에 설정값을 꽂아주는 역할이에요.
- 설정 프로퍼티 이름은 **복수형**이에요. 전역 설정은 `spring.http.clients.connect-timeout`, `spring.http.clients.read-timeout`, `spring.http.clients.redirects`, `spring.http.clients.imperative.factory`예요. 예전 문서에 나오는 단수형 `spring.http.client.*`는 사용 중단(deprecated) 별칭이에요.
- 선언형 클라이언트의 그룹별 설정은 `spring.http.serviceclient.<그룹명>` 아래에 둬요. `HttpClientProperties`가 받는 값은 `base-url`, `default-header`, `apiversion`, 그리고 상속받은 `connect-timeout`·`read-timeout`·`redirects`·`ssl.bundle`이에요.
- 그룹명은 `Map.get(String)`으로 정확히 일치 비교해요. yaml에 적은 `kis-auth:`와 `@ImportHttpServices(group = "kis-auth")`의 문자열이 글자 단위로 같아야 해요.
- 요청 팩토리 선택 순서는 Apache HttpClient → Jetty → Reactor Netty → JDK `java.net.http.HttpClient` → `HttpURLConnection`이에요. 이 프로젝트에는 앞의 셋이 없으므로 **JDK `HttpClient`가 선택돼요.** 별도 의존성을 추가하지 않아요.

### 선언형 클라이언트 관련 클래스 위치

- `@HttpExchange`, `@PostExchange`는 `org.springframework.web.service.annotation` 패키지예요.
- `@ImportHttpServices`는 `org.springframework.web.service.registry` 패키지예요. 속성은 `value`, `types`, `group`, `basePackages`, `basePackageClasses`, `clientType` 여섯 개예요.
- `clientType`에 넣을 열거값은 `org.springframework.web.service.registry.HttpServiceGroup.ClientType`의 `REST_CLIENT`, `WEB_CLIENT`, `UNSPECIFIED`예요.
- 수동으로 프록시를 만들 때는 `org.springframework.web.service.invoker.HttpServiceProxyFactory`와 `org.springframework.web.client.support.RestClientAdapter`를 써요.
- `spring-test` 7.0.8의 `MockRestServiceServer`에는 `bindTo(RestClient.Builder)` 오버로드가 있어요. 이미 `spring-boot-starter-webmvc-test`로 들어와 있으니 테스트용 의존성을 새로 추가할 필요가 없어요.
- `@ConfigurationProperties`와 `@EnableConfigurationProperties`는 Boot 3과 같은 `org.springframework.boot.context.properties` 패키지에 그대로 있어요.

### Jackson 버전

Spring Boot 4.0.7은 **Jackson 3.1.4**를 쓰고, 좌표는 `tools.jackson.core:jackson-databind`예요. 다만 애노테이션은 여전히 `com.fasterxml.jackson.core:jackson-annotations:2.19.2`에서 오므로, `@JsonProperty`와 `@JsonFormat`의 import 경로는 `com.fasterxml.jackson.annotation.*` 그대로예요. Jackson 3은 `java.time` 지원을 내장했으니 `jackson-datatype-jsr310`을 따로 넣지 않아요.

### 저장소 현재 상태

- 빌드는 Maven이에요. Gradle 파일은 없어요. `./mvnw`를 써요.
- Java 25, Spring Boot 4.0.7이에요.
- `application.yaml`에는 `spring.application.name: trendlog-backend` 한 줄만 있어요. 기본 프로파일은 지정하지 않았으니 실행할 때 `dev`나 `prod`를 반드시 명시해야 해요.
- `src/main/resources/application-prod.yaml`은 `.gitignore`에 등록돼 있어 저장소에 없어요. 이번 작업에서 건드리지 마세요.
- `.gitignore`에 `.env` 관련 규칙이 아직 없어요.
- `.vscode/launch.json`은 추적돼요(`.vscode/*` 무시 + `!.vscode/launch.json` 예외). 실행 구성 이름은 `Trendlog API`이고 프로파일을 `pickString`으로 골라요.
- `AbstractIntegrationTest`가 `@SpringBootTest` + Testcontainers MySQL 8.4로 전체 컨텍스트를 띄우고, `ApiApplicationTests.contextLoads`가 이를 상속해요.
- Java 소스는 들여쓰기에 **탭**을 써요. 기존 파일과 맞춰주세요.

## 확정된 결정 사항

사용자가 직접 고른 항목이에요. 그대로 따라주세요.

1. **이번 브랜치는 접근토큰발급 한 건만 연동해요.** 접근토큰폐기(`/oauth2/revokeP`)와 웹소켓 접속키 발급(`/oauth2/Approval`)은 실제로 호출할 곳이 생기는 시점에 붙여요.
2. **호출 코드는 선언형 `@HttpExchange` 방식으로 써요.** 인터페이스에 메서드만 선언하고 구현은 Spring이 만들어요. 앞으로 붙일 시세 API가 10여 개라, 호출 지점마다 조립 코드를 반복하지 않는 쪽을 택했어요.
3. **`external` 패키지는 순수 API 연동만 담당해요.** 요청 본문을 만들어 보내고 응답을 DTO로 옮기는 일까지만 해요. 캐시, 만료 판정, 재발급 판단은 넣지 않아요.
4. **토큰 재사용 레이어는 다음 브랜치에서 만들어요.** 발급받은 토큰을 어디에 보관하고 언제 다시 발급할지는 별도 레이어가 혼자 결정해요. 그 레이어의 패키지 이름과 파일 구조도 다음 브랜치에서 확정해요. 이번 브랜치에서 미리 만들지 마세요.
5. **패키지는 `io.trendlog.api.external.kis`에 둬요.** 외부 연동만 모으는 최상위 패키지예요. 향후 US 어댑터는 같은 층에 붙어요.
6. **연동 환경은 실전으로 고정해요.** `https://openapi.koreainvestment.com:9443` 하나만 설정에 적어요. 모의 도메인을 고르는 분기 코드를 넣지 마세요. 시세 조회 API 상당수가 모의투자 미지원이라 실전만 쓸 수 있어요.
7. **`.env` 계열 파일은 전부 git에서 제외하고, 저장소에는 `example.env`만 올려요.** 실제 appkey와 appsecret은 커밋되지 않아요.
8. **검증은 단위 테스트로 해요.** 확인 전용 HTTP 엔드포인트는 만들지 않아요.

## 설계 결정

사용자에게 따로 확인하지 않고 제가 정한 값이에요. 이유를 함께 적었어요.

1. **선언형 클라이언트의 그룹명은 `kis-auth`로 해요.** 토큰 발급은 인증 헤더가 필요 없지만, 앞으로 붙일 시세 API는 `authorization`·`appkey`·`appsecret`·`tr_id` 헤더를 매번 실어야 해요. 시세용 그룹에 토큰을 자동으로 끼워 넣는 가로채기(interceptor)를 나중에 달 텐데, 그게 토큰 발급 호출에도 걸리면 토큰을 받으려고 토큰을 요구하는 순환이 생겨요. 그래서 처음부터 인증용 그룹을 따로 떼어 놨어요.

2. **`KisAuthClient`가 자격증명을 채워 넣어요.** 선언형 인터페이스 `KisAuthApi`는 프로토콜만 서술하고, 그 위에 얇은 `KisAuthClient`를 둬요. 이 컴포넌트가 `KisProperties`에서 appkey와 appsecret을 읽어 요청 본문을 만들어요. 이렇게 하면 다음 브랜치의 토큰 재사용 레이어가 자격증명을 알 필요 없이 `issueAccessToken()` 한 번만 부르면 돼요. 상태는 갖지 않으니 3번 결정과 어긋나지 않아요.

3. **실패 응답 본문을 파싱하는 DTO를 만들지 않아요.** KIS 명세 시트에 오류 본문 규격이 없어서, 필드 이름을 상상해 만들면 실제 응답과 어긋날 위험이 있어요. 대신 `KisApiException`이 HTTP 상태 코드와 응답 본문 원문을 그대로 실어 던져요. 실제 오류 응답을 한 번 관찰한 뒤에 전용 DTO를 붙이는 게 맞아요.

4. **재시도와 호출량 제한은 넣지 않아요.** Spring Framework 7에는 `org.springframework.resilience.annotation.@Retryable`과 `@ConcurrencyLimit`이 들어 있어서 나중에 쓸 수 있어요. 다만 이번 브랜치에는 토큰 발급을 부르는 운영 경로가 아직 없고, KIS의 초당 호출 제한은 시세 조회를 붙이는 MarketSync 브랜치에서 다뤄야 할 문제예요.

5. **`AbstractIntegrationTest`에 더미 자격증명을 주입해요.** `application.yaml`이 `${KIS_APP_KEY}`를 기본값 없이 읽으므로, 환경변수가 없는 CI에서는 `KisProperties` 바인딩이 실패해 `ApiApplicationTests.contextLoads`가 깨져요. 그래서 통합 테스트 부모 클래스에서 가짜 값을 넘겨요. 운영 기동은 여전히 값이 없으면 즉시 실패해요.

6. **응답의 `access_token_token_expired`는 `LocalDateTime`으로 받아요.** `"2023-12-22 08:16:59"` 형식이라 `@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")`로 매핑돼요. 시간대는 붙이지 않고, 어느 지역 시간으로 볼지는 다음 브랜치의 토큰 레이어가 결정해요.

## 작업 순서

브랜치는 `argon1025/openapi`예요. 이미 이 브랜치에 있으니 새로 만들지 마세요.

첫 커밋은 이 플랜 스냅샷이에요(`.harness/workspace/progress/argon1025-openapi/plan.md`). 아래 네 개는 그다음에 이어져요.

---

### 커밋 1 — `chore: KIS 연동용 RestClient 모듈과 로컬 환경변수 파일 규약 추가`

| 파일 | 작업 |
|---|---|
| `pom.xml` | `spring-boot-restclient` 의존성 추가 |
| `.gitignore` | `.env` 계열 무시 규칙 추가, `example.env`는 예외 |
| `example.env` | 신규. 필요한 환경변수 견본 |
| `.vscode/launch.json` | `envFile` 항목 추가 |

`pom.xml`에는 아래를 넣어요. 위치는 `spring-boot-starter-webmvc` 바로 다음이 자연스러워요. `<version>`을 적지 마세요.

```xml
<dependency>
	<groupId>org.springframework.boot</groupId>
	<artifactId>spring-boot-restclient</artifactId>
</dependency>
```

`.gitignore`에는 아래 블록을 파일 끝에 붙여요. 부정 규칙(`!`)이 동작하려면 상위 디렉터리를 통째로 제외하지 않아야 해요.

```gitignore
### Local env files (untracked, example.env only) ###
*.env
.env
.env.*
!example.env
```

`example.env`는 두 줄이에요.

```
KIS_APP_KEY=your-app-key
KIS_APP_SECRET=your-app-secret
```

`.vscode/launch.json`의 `configurations[0]`에 `"envFile": "${workspaceFolder}/.env"`를 추가해요. 그러면 VS Code로 실행할 때 `.env`의 값이 환경변수로 들어가요. 나머지 항목은 건드리지 마세요.

**검증:** `./mvnw -q compile`이 성공해요. `git check-ignore -v .env`가 `*.env` 규칙에 걸렸다고 알려주고, `git check-ignore -v example.env`는 아무것도 출력하지 않아요(무시 대상이 아니라는 뜻이에요).

---

### 커밋 2 — `feat: KIS 접근토큰 발급 API 연동`

| 파일 | 작업 |
|---|---|
| `src/main/java/io/trendlog/api/external/kis/KisProperties.java` | 신규. appkey·appsecret 설정 바인딩 |
| `src/main/java/io/trendlog/api/external/kis/KisAuthApi.java` | 신규. 선언형 HTTP 인터페이스 |
| `src/main/java/io/trendlog/api/external/kis/KisAuthClient.java` | 신규. 자격증명 주입과 예외 변환 |
| `src/main/java/io/trendlog/api/external/kis/KisApiException.java` | 신규. 호출 실패 예외 |
| `src/main/java/io/trendlog/api/external/kis/KisClientConfig.java` | 신규. 클라이언트 등록 설정 |
| `src/main/java/io/trendlog/api/external/kis/dto/KisTokenRequest.java` | 신규. 요청 본문 |
| `src/main/java/io/trendlog/api/external/kis/dto/KisTokenResponse.java` | 신규. 응답 본문 |
| `src/main/resources/application.yaml` | KIS 접속 설정과 자격증명 자리표시자 추가 |
| `src/test/java/io/trendlog/api/support/AbstractIntegrationTest.java` | 더미 자격증명 주입 |

`KisProperties`는 레코드로 만들고 값이 비면 기동이 실패하게 해요.

```java
@Validated
@ConfigurationProperties(prefix = "kis")
public record KisProperties(
		@NotBlank String appKey,
		@NotBlank String appSecret) {
}
```

`KisAuthApi`는 메서드 한 개만 선언해요. 반환형은 응답 DTO예요.

```java
public interface KisAuthApi {

	@PostExchange("/oauth2/tokenP")
	KisTokenResponse issueAccessToken(@RequestBody KisTokenRequest request);

}
```

`KisTokenRequest`는 고정값 `client_credentials`를 안에서 채워요. JSON 필드 이름이 KIS 명세와 정확히 같아야 하므로 `@JsonProperty`를 붙여요.

```java
public record KisTokenRequest(
		@JsonProperty("grant_type") String grantType,
		@JsonProperty("appkey") String appKey,
		@JsonProperty("appsecret") String appSecret) {

	private static final String GRANT_TYPE_CLIENT_CREDENTIALS = "client_credentials";

	public static KisTokenRequest of(String appKey, String appSecret) {
		return new KisTokenRequest(GRANT_TYPE_CLIENT_CREDENTIALS, appKey, appSecret);
	}

}
```

`KisTokenResponse`는 응답 네 필드를 그대로 받아요.

```java
public record KisTokenResponse(
		@JsonProperty("access_token") String accessToken,
		@JsonProperty("token_type") String tokenType,
		@JsonProperty("expires_in") long expiresInSeconds,
		@JsonProperty("access_token_token_expired")
		@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime expiredAt) {
}
```

`KisAuthClient`는 `@Component`로 두고 자격증명을 채워 호출해요. 실패는 `KisApiException`으로 바꿔요.

```java
@Component
public class KisAuthClient {

	private final KisAuthApi kisAuthApi;
	private final KisProperties kisProperties;

	public KisAuthClient(KisAuthApi kisAuthApi, KisProperties kisProperties) {
		this.kisAuthApi = kisAuthApi;
		this.kisProperties = kisProperties;
	}

	public KisTokenResponse issueAccessToken() {
		try {
			return kisAuthApi.issueAccessToken(
					KisTokenRequest.of(kisProperties.appKey(), kisProperties.appSecret()));
		}
		catch (RestClientResponseException exception) {
			throw new KisApiException(exception.getStatusCode(),
					exception.getResponseBodyAsString(), exception);
		}
	}

}
```

`KisApiException`은 `RuntimeException`을 상속하고 HTTP 상태 코드와 응답 본문 원문을 필드로 들고 있어요. KIS가 실패 응답 규격을 문서로 공개하지 않아 본문을 해석하지 않고 그대로 실어 보낸다는 점을, 본문을 담는 필드 바로 위에 한 줄 주석으로 남겨주세요.

`KisClientConfig`는 설정 클래스 하나로 끝나요.

```java
@Configuration
@EnableConfigurationProperties(KisProperties.class)
@ImportHttpServices(group = "kis-auth", types = KisAuthApi.class, clientType = ClientType.REST_CLIENT)
public class KisClientConfig {
}
```

`application.yaml`에는 아래를 추가해요. 기존 `spring.application.name`은 그대로 두고 같은 `spring` 아래에 `http`를 붙여요. 자격증명에 기본값을 넣지 마세요. 값이 없으면 기동 시점에 바로 실패해야 해요.

```yaml
spring:
  application:
    name: trendlog-backend
  http:
    serviceclient:
      kis-auth:
        base-url: https://openapi.koreainvestment.com:9443
        connect-timeout: 3s
        read-timeout: 10s

kis:
  app-key: ${KIS_APP_KEY}
  app-secret: ${KIS_APP_SECRET}
```

`AbstractIntegrationTest`는 `@SpringBootTest`에 더미 값을 넘기도록 바꿔요. 실제 KIS 자격증명 없이도 컨텍스트가 떠야 한다는 이유를 한 줄 주석으로 남겨주세요.

```java
@SpringBootTest(properties = {
		"KIS_APP_KEY=test-app-key",
		"KIS_APP_SECRET=test-app-secret"
})
```

**검증:** `./mvnw test`가 통과해요. `Tests run: 1, Failures: 0, Errors: 0`이 나오고, `ApiApplicationTests.contextLoads`가 통과한다는 것은 `KisAuthApi` 프록시 빈과 `KisProperties` 바인딩이 정상이라는 뜻이에요. Docker 데몬이 떠 있어야 해요.

---

### 커밋 3 — `test: KIS 접근토큰 발급 클라이언트 단위 테스트 추가`

| 파일 | 작업 |
|---|---|
| `src/test/java/io/trendlog/api/external/kis/KisAuthClientTest.java` | 신규. 요청·응답 매핑과 실패 변환 검증 |
| `src/test/java/io/trendlog/api/external/kis/KisAuthClientManualTest.java` | 신규. 실제 KIS를 부르는 수동 실행 테스트 |

`KisAuthClientTest`는 Spring 컨텍스트를 띄우지 않아요. `MockRestServiceServer`를 `RestClient.Builder`에 붙이고, 그 빌더로 만든 `RestClient`에서 선언형 프록시를 직접 만들어요.

```java
RestClient.Builder builder = RestClient.builder().baseUrl("https://kis.test");
MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
KisAuthApi kisAuthApi = HttpServiceProxyFactory
		.builderFor(RestClientAdapter.create(builder.build()))
		.build()
		.createClient(KisAuthApi.class);
KisAuthClient client = new KisAuthClient(kisAuthApi,
		new KisProperties("test-app-key", "test-app-secret"));
```

검증할 항목은 두 가지예요. 각 테스트 메서드에는 한국어 `@DisplayName`을 달아주세요(기존 관례예요).

1. **정상 발급** — `POST /oauth2/tokenP`로 나가고, 요청 본문의 `grant_type`이 `client_credentials`이며 `appkey`와 `appsecret`이 설정값과 같아요. 응답으로 위의 실제 예시 JSON을 돌려주면 `accessToken`, `tokenType`(`Bearer`), `expiresInSeconds`(`86400`), `expiredAt`(`2023-12-22T08:16:59`) 네 개가 채워져요.
2. **실패 변환** — 서버가 `403 Forbidden`과 임의의 본문을 돌려주면 `KisApiException`이 던져지고, 예외가 들고 있는 상태 코드가 403이며 응답 본문 원문이 그대로 담겨 있어요.

`KisAuthClientManualTest`는 실제 KIS를 부르는 테스트예요. `@Disabled("실제 KIS 호출. 실키가 있을 때만 수동 실행")`을 붙여서 CI에서는 절대 돌지 않게 해요. 환경변수 `KIS_APP_KEY`와 `KIS_APP_SECRET`을 읽어 실전 도메인으로 발급을 한 번 호출하고, 받은 `accessToken`이 비어 있지 않은지만 확인해요. 이 파일이 있는 이유(실호출 응답 모양을 언제든 다시 확인하려고 남겨 둔다는 것)를 클래스 위에 한 줄 주석으로 적어주세요.

**검증:** `./mvnw test`가 통과하고, 새 테스트 2건이 실행되며 수동 테스트 1건은 건너뛴 것으로 표시돼요(`Tests run: 3, Failures: 0, Errors: 0, Skipped: 1`).

---

### 커밋 4 — `docs: KIS 연동 환경변수 설정 안내 추가`

| 파일 | 작업 |
|---|---|
| `README.md` | 외부 연동 설정 안내 추가 |

README에 담을 내용은 세 가지예요.

- 기술 스택 표에 `외부 연동` 행을 넣어요. 값은 "한국투자증권 OpenAPI (Spring `RestClient` 선언형 클라이언트)"예요.
- "로컬 실행" 절에 KIS 자격증명 준비 단계를 넣어요. `example.env`를 `.env`로 복사해 실제 값을 채우라고 안내해요. VS Code 실행 구성은 `.env`를 자동으로 읽지만, `./mvnw spring-boot:run`으로 띄울 때는 셸에서 먼저 값을 올려야 한다고 적어요.

  ```bash
  cp example.env .env
  # .env에 실제 appkey / appsecret을 채운 뒤
  set -a; source .env; set +a
  ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
  ```

- "운영 실행" 절의 환경변수 표에 `KIS_APP_KEY`와 `KIS_APP_SECRET` 두 행을 추가해요. 기존 `DB_URL`·`DB_USERNAME`·`DB_PASSWORD`와 마찬가지로 빠지면 기동이 실패한다고 적어요.

"디렉터리 구조" 절의 목록에도 `example.env`를 한 줄 넣어주세요.

**검증:** `README.md`를 눌러 읽었을 때 `.env` 준비 → 셸에 올리기 → 앱 기동 순서가 끊기지 않아요.

---

## 마무리 확인

네 커밋이 끝나면 아래를 순서대로 확인해요.

1. `./mvnw test` — 전체 통과. `Tests run: 3, Failures: 0, Errors: 0, Skipped: 1`이에요. Docker 데몬이 떠 있어야 해요.
2. `git status` — `.env`가 목록에 나타나지 않아요. `example.env`만 추적돼요.
3. 실제 키로 한 번 확인하기 — `.env`에 실제 appkey와 appsecret을 채운 뒤 `KisAuthClientManualTest`의 `@Disabled`를 잠시 지우고 그 테스트만 돌려요. 응답의 `access_token`이 채워지고 `expiresInSeconds`가 `86400`으로 오면 연동이 끝난 거예요. 확인이 끝나면 `@Disabled`를 반드시 되돌려 놓아요.

## feedback.md에 남길 내용

작업 중 아래 사실들을 `.harness/workspace/progress/argon1025-openapi/feedback.md`에 append하고, 해당 커밋에 함께 담아주세요.

- `spring-boot-starter-webmvc`는 `RestClient` 자동 구성을 가져오지 않으므로 `spring-boot-restclient` 모듈을 따로 넣어야 한다는 사실
- Boot 4의 HTTP 클라이언트 설정 프로퍼티가 복수형 `spring.http.clients.*`이고 단수형은 사용 중단 별칭이라는 사실
- 선언형 클라이언트 그룹명이 `Map.get`으로 정확히 일치 비교되므로 yaml 키와 애노테이션 문자열이 같아야 한다는 사실
- Jackson 3.1.4를 쓰지만 애노테이션 import는 `com.fasterxml.jackson.annotation.*` 그대로라는 사실
- KIS가 접근토큰발급 실패 응답 규격을 명세에 공개하지 않아, 전용 오류 DTO 대신 응답 본문 원문을 예외에 실어 보내기로 했다는 결정과 이유
- 인증용 그룹(`kis-auth`)을 시세용 그룹과 처음부터 분리한 이유(시세 그룹에 달 토큰 주입 가로채기가 토큰 발급 호출에도 걸리면 순환이 생김)
- 토큰 보관·재발급 레이어는 다음 브랜치에서 만들고, 그 패키지 이름과 파일 구조도 그때 확정한다는 사용자와의 합의
- 연동 환경을 실전으로 고정한 이유(시세 조회 API 상당수가 모의투자 미지원)
- `application.yaml`이 `${KIS_APP_KEY}`를 기본값 없이 읽기 때문에, 통합 테스트 부모 클래스가 더미 자격증명을 넘겨야 컨텍스트가 뜬다는 사실
