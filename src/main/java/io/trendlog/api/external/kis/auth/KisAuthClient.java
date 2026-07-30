package io.trendlog.api.external.kis.auth;

import org.springframework.stereotype.Component;

import io.trendlog.api.external.kis.KisProperties;
import io.trendlog.api.external.kis.auth.dto.KisTokenRequest;
import io.trendlog.api.external.kis.auth.dto.KisTokenResponse;
import lombok.RequiredArgsConstructor;

/**
 * 한국투자증권 Auth API Client 래퍼
 */
@Component
@RequiredArgsConstructor
public class KisAuthClient {

	private final KisAuthApi kisAuthApi;

	private final KisProperties kisProperties;

	/**
	 * Access Token 발급
	 */
	public KisTokenResponse issueAccessToken() {
		return kisAuthApi.issueAccessToken(
				KisTokenRequest.of(kisProperties.appKey(), kisProperties.appSecret()));
	}

}
