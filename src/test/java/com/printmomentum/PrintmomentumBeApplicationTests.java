package com.printmomentum;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class PrintmomentumBeApplicationTests {

	@Autowired
	private DataSource dataSource;

	@Test
	void contextLoads() {
	}

	@Test
	void testsUseH2() throws Exception {
		try (Connection connection = dataSource.getConnection()) {
			assertThat(connection.getMetaData().getURL()).contains("jdbc:h2:");
		}
	}
}

