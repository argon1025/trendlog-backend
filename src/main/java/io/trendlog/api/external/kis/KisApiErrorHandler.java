package io.trendlog.api.external.kis;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.ResponseErrorHandler;

/**
 * 한국투자증권 OpenAPI 공통 에러 처리 핸들러
 */
public class KisApiErrorHandler implements ResponseErrorHandler {

    @Override
    public boolean hasError(ClientHttpResponse response) throws IOException {
        return response.getStatusCode().isError();
    }

    @Override
    public void handleError(URI url, HttpMethod method, ClientHttpResponse response) throws IOException {
        throw new KisApiException(
                response.getStatusCode(), StreamUtils.copyToString(response.getBody(), StandardCharsets.UTF_8), null);
    }
}
