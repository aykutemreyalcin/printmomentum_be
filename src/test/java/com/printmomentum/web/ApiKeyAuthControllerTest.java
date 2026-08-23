package com.printmomentum.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.printmomentum.domain.ApiClient;
import com.printmomentum.domain.ApiClientRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ApiKeyAuthControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ApiClientRepository apiClientRepository;

	@Test
	void healthIsPublic() throws Exception {
		mockMvc.perform(get("/api/v1/health"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("ok"));
	}

	@Test
	void listingsWithoutKeyIsUnauthorized() throws Exception {
		mockMvc.perform(get("/api/v1/listings"))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
	}

	@Test
	void listingsWithValidConfiguredKeyIsOk() throws Exception {
		mockMvc.perform(get("/api/v1/listings").header(ApiKeyInterceptor.HEADER, "test-printmomentum-key"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items").isArray());
	}

	@Test
	void listingsWithValidTableKeyIsOk() throws Exception {
		apiClientRepository.save(new ApiClient("integration-test", "table-key-be015"));

		mockMvc.perform(get("/api/v1/listings").header(ApiKeyInterceptor.HEADER, "table-key-be015"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items").isArray());
	}

	@Test
	void listingsWithInvalidKeyIsUnauthorized() throws Exception {
		mockMvc.perform(get("/api/v1/listings").header(ApiKeyInterceptor.HEADER, "nope"))
				.andExpect(status().isUnauthorized());
	}
}
