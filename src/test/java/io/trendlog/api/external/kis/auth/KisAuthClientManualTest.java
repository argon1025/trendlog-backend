package io.trendlog.api.external.kis.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import io.trendlog.api.external.kis.KisApiErrorHandler;
import io.trendlog.api.external.kis.KisProperties;
import io.trendlog.api.external.kis.auth.dto.KisTokenResponse;

/**
 * KIS 접근토큰 발급 실호출 확인
 */
// 실제 응답 모양(필드 구성·만료 일시 형식)을 언제든 다시 눈으로 확인하려고 남겨 둔 테스트
@Disabled("실제 KIS 호출. 실키가 있을 때만 수동 실행")
class KisAuthClientManualTest {

	private static final String REAL_BASE_URL = "https://openapi.koreainvestment.com:9443";

	@Test
	@DisplayName("실전 도메인으로 접근토큰이 실제로 발급되는지 검증")
	void issueAccessToken() {
		KisProperties kisProperties = new KisProperties(
				System.getenv("KIS_APP_KEY"),
				System.getenv("KIS_APP_SECRET"));
		// 컨텍스트를 안 띄우면 KisClientConfig의 그룹 설정이 적용되지 않아 실패 변환 핸들러를 직접 붙임
		RestClient restClient = RestClient.builder()
				.baseUrl(REAL_BASE_URL)
				.defaultStatusHandler(new KisApiErrorHandler())
				.build();
		KisAuthApi kisAuthApi = HttpServiceProxyFactory
				.builderFor(RestClientAdapter.create(restClient))
				.build()
				.createClient(KisAuthApi.class);

		KisTokenResponse response = new KisAuthClient(kisAuthApi, kisProperties).issueAccessToken();

		assertThat(response.accessToken()).isNotBlank();
	}

}
