package io.trendlog.api.external.kis.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 접근토큰발급 요청 본문
 */
public record KisTokenRequest(
		@JsonProperty("grant_type") String grantType,
		@JsonProperty("appkey") String appKey,
		@JsonProperty("appsecret") String appSecret) {

	private static final String GRANT_TYPE_CLIENT_CREDENTIALS = "client_credentials";

	public static KisTokenRequest of(String appKey, String appSecret) {
		return new KisTokenRequest(GRANT_TYPE_CLIENT_CREDENTIALS, appKey, appSecret);
	}

}
