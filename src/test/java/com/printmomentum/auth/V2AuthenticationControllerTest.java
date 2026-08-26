package com.printmomentum.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class V2AuthenticationControllerTest {

	private static final String COOKIE = "printmomentum_refresh_token";

	@Autowired
	private MockMvc mockMvc;

	@Test
	void loginWithWrongPasswordIsUnauthorized() throws Exception {
		mockMvc.perform(post("/api/v2/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"user@printmomentum.local","password":"wrong"}
								"""))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void userLoginReturnsAccessTokenAndRefreshCookie() throws Exception {
		mockMvc.perform(post("/api/v2/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"user@printmomentum.local","password":"User123!","deviceId":"test-device"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.token").isNotEmpty())
				.andExpect(jsonPath("$.authVersion").value("v2"))
				.andExpect(jsonPath("$.expiresInSeconds").value(7200))
				.andExpect(cookie().exists(COOKIE))
				.andExpect(cookie().httpOnly(COOKIE, true));
	}

	@Test
	void adminAndUserSeeTheSameListingFeed() throws Exception {
		String adminToken = login("admin@printmomentum.local", "Admin123!");
		String userToken = login("user@printmomentum.local", "User123!");

		MvcResult adminFeed = mockMvc.perform(get("/api/v1/listings").header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isOk())
				.andReturn();
		MvcResult userFeed = mockMvc.perform(get("/api/v1/listings").header("Authorization", "Bearer " + userToken))
				.andExpect(status().isOk())
				.andReturn();

		org.assertj.core.api.Assertions.assertThat(adminFeed.getResponse().getContentAsString())
				.isEqualTo(userFeed.getResponse().getContentAsString());
	}

	@Test
	void refreshRotatesCookieAndCurrentUserWorks() throws Exception {
		MvcResult login = mockMvc.perform(post("/api/v2/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"user@printmomentum.local","password":"User123!"}
								"""))
				.andExpect(status().isOk())
				.andReturn();
		Cookie refresh = login.getResponse().getCookie(COOKIE);
		String access = com.jayway.jsonpath.JsonPath.read(login.getResponse().getContentAsString(), "$.token");

		mockMvc.perform(get("/api/v1/user").header("Authorization", "Bearer " + access))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value("user@printmomentum.local"))
				.andExpect(jsonPath("$.role").value("user"));

		MvcResult refreshed = mockMvc.perform(post("/api/v2/auth/refresh")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}")
						.cookie(refresh))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.token").isNotEmpty())
				.andExpect(cookie().exists(COOKIE))
				.andReturn();

		mockMvc.perform(post("/api/v2/auth/refresh")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}")
						.cookie(refresh))
				.andExpect(status().isUnauthorized());

		Cookie rotated = refreshed.getResponse().getCookie(COOKIE);
		mockMvc.perform(post("/api/v2/auth/logout").cookie(rotated)).andExpect(status().isNoContent());

		mockMvc.perform(post("/api/v2/auth/refresh")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}")
						.cookie(rotated))
				.andExpect(status().isUnauthorized());
	}

	private String login(String email, String password) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v2/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
				.andExpect(status().isOk())
				.andReturn();
		return com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), "$.token");
	}
}
