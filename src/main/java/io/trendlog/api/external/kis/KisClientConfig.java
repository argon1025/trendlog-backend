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
@ImportHttpServices(group = "kis-auth", types = KisAuthApi.class, clientType = ClientType.REST_CLIENT)
public class KisClientConfig {

	private static final String KIS_GROUP_NAME_PREFIX = "kis-";

	/**
	 * 공통 에러 처리 핸들러 등록
	 */
	@Bean
	RestClientHttpServiceGroupConfigurer kisApiErrorHandlerConfigurer() {
		KisApiErrorHandler errorHandler = new KisApiErrorHandler();
		return groups -> groups.filter(group -> group.name().startsWith(KIS_GROUP_NAME_PREFIX))
				.forEachClient((group, builder) -> builder.defaultStatusHandler(errorHandler));
	}

}
