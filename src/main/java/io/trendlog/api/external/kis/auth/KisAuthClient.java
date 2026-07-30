package io.trendlog.api.external.kis.auth;

import org.springframework.stereotype.Component;

import io.trendlog.api.external.kis.KisProperties;
import io.trendlog.api.external.kis.auth.dto.KisTokenRequest;
import io.trendlog.api.external.kis.auth.dto.KisTokenResponse;
import lombok.RequiredArgsConstructor;

/**
 * 한국투자증권 접근토큰 발급 호출
 */
// KisProperties를 주입받는 클래스를 io.trendlog.api.external.kis 안에만 두려고 남겨 둔 얇은 컴포넌트.
// 시세 API는 appkey·appsecret을 그룹 기본 헤더로 채우지만 접근토큰발급만 JSON 본문으로 받아서 조립 지점이 따로 필요함
@Component
@RequiredArgsConstructor
public class KisAuthClient {

	private final KisAuthApi kisAuthApi;

	private final KisProperties kisProperties;

	public KisTokenResponse issueAccessToken() {
		return kisAuthApi.issueAccessToken(
				KisTokenRequest.of(kisProperties.appKey(), kisProperties.appSecret()));
	}

}
