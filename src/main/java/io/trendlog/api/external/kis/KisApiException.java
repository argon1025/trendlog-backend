package io.trendlog.api.external.kis;

import org.springframework.http.HttpStatusCode;

/**
 * 한국투자증권 OpenAPI 호출 실패
 */
public class KisApiException extends RuntimeException {

	private final HttpStatusCode statusCode;

	// 원문 그대로
	private final String responseBody;

	public KisApiException(HttpStatusCode statusCode, String responseBody, Throwable cause) {
		super("KIS API 호출이 실패했습니다. status=" + statusCode + ", body=" + responseBody, cause);
		this.statusCode = statusCode;
		this.responseBody = responseBody;
	}

	public HttpStatusCode getStatusCode() {
		return statusCode;
	}

	public String getResponseBody() {
		return responseBody;
	}

}
