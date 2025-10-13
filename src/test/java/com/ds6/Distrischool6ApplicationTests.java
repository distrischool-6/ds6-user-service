package com.ds6;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.flyway.enabled=false",
    "jwt.secret=test-secret-for-ci-cd",
    "jwt.expiration=3600000"
})
class Distrischool6ApplicationTests {

	@Test
	void contextLoads() {
		// Este teste verifica se o contexto Spring carrega corretamente
		// Usa H2 em memória definido diretamente nas propriedades do teste
		// Isso garante que funcione em qualquer ambiente CI/CD
	}

}
