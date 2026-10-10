package com.qaryati.qaryati;

import com.qaryati.qaryati.integration.TestcontainersConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfig.class)
class QaryatiApplicationTests {

	@Test
	void contextLoads() {
	}
}