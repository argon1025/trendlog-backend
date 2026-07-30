package io.trendlog.api.external.kis.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import io.trendlog.api.external.kis.KisApiErrorHandler;
import io.trendlog.api.external.kis.KisApiException;
import io.trendlog.api.external.kis.KisProperties;
import io.trendlog.api.external.kis.auth.dto.KisTokenResponse;

/**
 * KIS 접근토큰 발급 클라이언트 단위 테스트
 */
class KisAuthClientTest {

	private static final String BASE_URL = "https://kis.test";

	private static final String APP_KEY = "test-app-key";

	private static final String APP_SECRET = "test-app-secret";

	private MockRestServiceServer server;

	private KisAuthClient kisAuthClient;

	@BeforeEach
	void setUp() {
		// 컨텍스트를 안 띄우면 KisClientConfig의 그룹 설정이 적용되지 않아 실패 변환 핸들러를 직접 붙임
		RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL)
				.defaultStatusHandler(new KisApiErrorHandler());
		this.server = MockRestServiceServer.bindTo(builder).build();
		// 컨텍스트를 띄우지 않고 선언형 프록시만 직접 만들어 붙임
		KisAuthApi kisAuthApi = HttpServiceProxyFactory
				.builderFor(RestClientAdapter.create(builder.build()))
				.build()
				.createClient(KisAuthApi.class);
		this.kisAuthClient = new KisAuthClient(kisAuthApi, new KisProperties(APP_KEY, APP_SECRET));
	}

	@Test
	@DisplayName("접근토큰 발급 요청 본문에 고정 grant_type과 설정된 자격증명을 담아 보내고 응답 네 필드를 매핑하는지 검증")
	void issueAccessToken() {
		String responseBody = """
				{
					"access_token": "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxMiJ9",
					"access_token_token_expired": "2023-12-22 08:16:59",
					"token_type": "Bearer",
					"expires_in": 86400
				}
				""";
		server.expect(requestTo(BASE_URL + "/oauth2/tokenP"))
				.andExpect(method(HttpMethod.POST))
				.andExpect(jsonPath("$.grant_type").value("client_credentials"))
				.andExpect(jsonPath("$.appkey").value(APP_KEY))
				.andExpect(jsonPath("$.appsecret").value(APP_SECRET))
				.andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

		KisTokenResponse response = kisAuthClient.issueAccessToken();

		server.verify();
		assertThat(response.accessToken()).isEqualTo("eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxMiJ9");
		assertThat(response.tokenType()).isEqualTo("Bearer");
		assertThat(response.expiresInSeconds()).isEqualTo(86400L);
		assertThat(response.expiredAt()).isEqualTo(LocalDateTime.of(2023, 12, 22, 8, 16, 59));
	}

	@Test
	@DisplayName("발급 호출이 실패하면 상태 코드와 응답 본문 원문을 담은 KisApiException으로 바뀌는지 검증")
	void issueAccessTokenFailure() {
		String errorBody = "{\"error_description\":\"invalid appkey\"}";
		server.expect(requestTo(BASE_URL + "/oauth2/tokenP"))
				.andRespond(withStatus(HttpStatus.FORBIDDEN)
						.contentType(MediaType.APPLICATION_JSON)
						.body(errorBody));

		assertThatThrownBy(() -> kisAuthClient.issueAccessToken())
				.isInstanceOf(KisApiException.class)
				.satisfies(thrown -> {
					KisApiException exception = (KisApiException) thrown;
					assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
					assertThat(exception.getResponseBody()).isEqualTo(errorBody);
				});
	}

}
