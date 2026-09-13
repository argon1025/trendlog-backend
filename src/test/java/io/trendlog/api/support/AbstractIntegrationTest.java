package io.trendlog.api.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

/**
 * 통합 테스트 구성
 */
@SpringBootTest
@Testcontainers
public abstract class AbstractIntegrationTest {

    // 테스트 클래스가 늘어나도 MySQL 컨테이너는 공유
    @Container
    @ServiceConnection
    static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.4");
}
