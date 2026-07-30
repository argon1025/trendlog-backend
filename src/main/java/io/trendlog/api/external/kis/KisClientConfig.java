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
 *
 * <p>KIS API를 하나 더 붙일 때 만드는 파일은 아래와 같습니다.
 *
 * <ul>
 * <li>대분류 폴더: {@code 한투OPENAPI.xlsx}의 해당 시트 {@code 메뉴 위치} 값을 그대로 폴더 이름으로 씁니다.
 * 그 폴더가 아직 없을 때만 새로 만듭니다.</li>
 * <li>{@code Kis...Api} 인터페이스: 대분류마다 하나만 둡니다. API가 늘면 기존 인터페이스에 메서드를 추가하고,
 * API마다 인터페이스를 새로 만들지 않습니다.</li>
 * <li>요청·응답 {@code record}: API마다 필요한 만큼 대분류 폴더의 {@code dto} 아래에 둡니다. 응답에
 * {@code output1}·{@code output2}가 있으면 중첩 {@code record}로 만듭니다.</li>
 * <li>{@code Kis...Client} 래퍼: 원칙적으로 만들지 않습니다. 시세 API는 {@code appkey}·{@code appsecret}·
 * {@code custtype}을 그룹 기본 헤더가, {@code authorization}을 인터셉터가, {@code tr_id}를
 * {@code @GetExchange(headers = "tr_id=...")}가 채우므로 감쌀 것이 없습니다. 접근토큰발급만 자격증명을 JSON
 * 본문으로 받아서 {@code KisAuthClient}가 남아 있습니다.</li>
 * <li>그룹: 인증 헤더 정책이 다를 때만 추가합니다. 지금은 {@code kis-auth} 하나이고 시세용 {@code kis-quote}를
 * 하나 더 만들 예정입니다. 실패 변환은 {@code kis-} 접두사를 가진 모든 그룹에 이미 걸려 있으므로 새 API에
 * {@code try/catch}를 다시 쓰지 않습니다.</li>
 * </ul>
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
