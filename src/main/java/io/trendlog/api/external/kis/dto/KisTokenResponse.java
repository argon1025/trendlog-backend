package io.trendlog.api.external.kis.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 접근토큰발급 응답 본문
 */
public record KisTokenResponse(
		@JsonProperty("access_token") String accessToken,
		@JsonProperty("token_type") String tokenType,
		@JsonProperty("expires_in") long expiresInSeconds,
		// KIS는 만료 일시를 시간대 없는 "yyyy-MM-dd HH:mm:ss" 문자열로 내려줌
		@JsonProperty("access_token_token_expired")
		@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime expiredAt) {
}
