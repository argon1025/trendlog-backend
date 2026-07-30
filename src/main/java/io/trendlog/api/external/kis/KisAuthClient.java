package io.trendlog.api.external.kis;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;

import io.trendlog.api.external.kis.dto.KisTokenRequest;
import io.trendlog.api.external.kis.dto.KisTokenResponse;

/**
 * 한국투자증권 접근토큰 발급 호출
 */
@Component
public class KisAuthClient {

	private final KisAuthApi kisAuthApi;

	private final KisProperties kisProperties;

	public KisAuthClient(KisAuthApi kisAuthApi, KisProperties kisProperties) {
		this.kisAuthApi = kisAuthApi;
		this.kisProperties = kisProperties;
	}

	// 호출하는 쪽이 자격증명을 모르고도 발급받을 수 있도록 여기서 채워 넣음
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
