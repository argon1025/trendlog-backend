package io.trendlog.api.external.kis;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.support.RestClientHttpServiceGroupConfigurer;
import org.springframework.web.service.registry.HttpServiceGroup.ClientType;
import org.springframework.web.service.registry.ImportHttpServices;

import io.trendlog.api.external.kis.auth.KisAuthApi;

/**
 * 한국투자증권 OpenAPI 클라이언트 등록
 */
@Configuration
@EnableConfigurationProperties(KisProperties.class)
// group 문자열은 application.yaml의 spring.http.serviceclient 키와 정확히 일치해야 base-url이 적용됨
@ImportHttpServices(group = "kis-auth", types = KisAuthApi.class, clientType = ClientType.REST_CLIENT)
public class KisClientConfig {

	private static final String KIS_GROUP_NAME_PREFIX = "kis-";

	// 그룹 단위로 걸어 두면 인터페이스가 몇 개로 늘어나도 KIS 호출 실패가 전부 KisApiException으로 균일하게 올라옴
	@Bean
	RestClientHttpServiceGroupConfigurer kisApiErrorHandlerConfigurer() {
		KisApiErrorHandler errorHandler = new KisApiErrorHandler();
		return groups -> groups.filter(group -> group.name().startsWith(KIS_GROUP_NAME_PREFIX))
				.forEachClient((group, builder) -> builder.defaultStatusHandler(errorHandler));
	}

}
