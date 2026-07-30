package io.trendlog.api.external.kis;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.ResponseErrorHandler;

/**
 * 한국투자증권 OpenAPI 응답 실패를 KisApiException으로 변환
 */
public class KisApiErrorHandler implements ResponseErrorHandler {

	@Override
	public boolean hasError(ClientHttpResponse response) throws IOException {
		return response.getStatusCode().isError();
	}

	@Override
	public void handleError(URI url, HttpMethod method, ClientHttpResponse response) throws IOException {
		// 응답 본문을 그대로 실어 보내는 자리라 감쌀 원인 예외가 없어 cause에 null을 넘김
		throw new KisApiException(response.getStatusCode(),
				StreamUtils.copyToString(response.getBody(), StandardCharsets.UTF_8), null);
	}

}
