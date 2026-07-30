package io.trendlog.api.external.kis.auth;

import io.trendlog.api.external.kis.auth.dto.KisTokenRequest;
import io.trendlog.api.external.kis.auth.dto.KisTokenResponse;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.PostExchange;

/**
 * 한국투자증권 Auth API
 * 실제 규격과 동일, KisAuthClient에서 래핑하여 사용
 */
public interface KisAuthApi {

    /** Access Token 발급 */
    @PostExchange("/oauth2/tokenP")
    KisTokenResponse issueAccessToken(@RequestBody KisTokenRequest request);
}
