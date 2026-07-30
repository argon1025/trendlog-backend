package io.trendlog.api.external.kis;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.service.registry.HttpServiceGroup.ClientType;
import org.springframework.web.service.registry.ImportHttpServices;

/**
 * 한국투자증권 OpenAPI 클라이언트 등록
 */
@Configuration
@EnableConfigurationProperties(KisProperties.class)
// group 문자열은 application.yaml의 spring.http.serviceclient 키와 정확히 일치해야 base-url이 적용됨
@ImportHttpServices(group = "kis-auth", types = KisAuthApi.class, clientType = ClientType.REST_CLIENT)
public class KisClientConfig {
}
