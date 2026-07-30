package io.trendlog.api.external.kis;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

/**
 * 한국투자증권 OpenAPI 자격증명
 */
@Validated
@ConfigurationProperties(prefix = "kis")
public record KisProperties(
		@NotBlank String appKey,
		@NotBlank String appSecret) {
}
