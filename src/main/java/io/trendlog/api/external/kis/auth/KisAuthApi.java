package io.trendlog.api.external.kis.auth;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.PostExchange;

import io.trendlog.api.external.kis.auth.dto.KisTokenRequest;
import io.trendlog.api.external.kis.auth.dto.KisTokenResponse;

/**
 * 한국투자증권 OpenAPI 인증 호출 명세
 */
public interface KisAuthApi {

	@PostExchange("/oauth2/tokenP")
	KisTokenResponse issueAccessToken(@RequestBody KisTokenRequest request);

}
