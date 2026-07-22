package io.trendlog.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class OpenApiConfig {

	@Bean
	public OpenAPI trendlogOpenAPI() {
		return new OpenAPI()
				.info(new Info()
						.title("Trendlog API")
						.version("v1")
						.description("Trendlog 백엔드 API 명세"));
	}

}
