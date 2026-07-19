package io.trendlog.api.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

/**
 * 통합 테스트 공통 부모. 컨테이너를 static으로 두어 테스트 클래스가 늘어나도 MySQL은 하나만 뜬다.
 */
@SpringBootTest
@Testcontainers
public abstract class AbstractIntegrationTest {

	@Container
	@ServiceConnection
	static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.4");

}
