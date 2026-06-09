package com.ibfd.schematron;

import com.ibfd.schematron.runner.AppRunner;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class SchematronApplicationTests {

	// Prevent AppRunner from executing file I/O during context-load tests
	@MockitoBean
	AppRunner appRunner;

	@Test
	void contextLoads() {
		// Verifies that the Spring context (Saxon bean, services) wires correctly
	}

}
