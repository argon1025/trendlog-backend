package io.trendlog.api.external.kis;

import org.springframework.http.HttpStatusCode;

/**
 * 한국투자증권 OpenAPI 호출 실패
 */
public class KisApiException extends RuntimeException {

	private final HttpStatusCode statusCode;

	// KIS가 실패 응답 본문 규격을 명세에 공개하지 않아 해석하지 않고 원문을 그대로 보관
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
