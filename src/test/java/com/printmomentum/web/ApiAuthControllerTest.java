package com.printmomentum.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ApiAuthControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void healthIsPublic() throws Exception {
		mockMvc.perform(get("/api/v1/health"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("ok"));
	}

	@Test
	void listingsWithoutAuthIsUnauthorized() throws Exception {
		mockMvc.perform(get("/api/v1/listings")).andExpect(status().isUnauthorized());
	}

	@Test
	@WithMockUser(roles = "user")
	void listingsWithUserRoleIsOk() throws Exception {
		mockMvc.perform(get("/api/v1/listings"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items").isArray());
	}

	@Test
	@WithMockUser(roles = "admin")
	void listingsWithAdminRoleIsOk() throws Exception {
		mockMvc.perform(get("/api/v1/listings"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items").isArray());
	}
}
