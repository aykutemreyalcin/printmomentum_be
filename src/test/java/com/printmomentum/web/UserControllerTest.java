package com.printmomentum.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
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
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UserControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void changePasswordWithoutAuthIsUnauthorized() throws Exception {
		mockMvc.perform(patch("/api/v1/user")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"currentPassword":"User123!","newPassword":"NewPass1","confirmationPassword":"NewPass1"}
								"""))
				.andExpect(status().isUnauthorized());
	}

	@Test
	@Order(99)
	void userCanChangeOwnPasswordThenLoginWithNewOne() throws Exception {
		String token = login("user@printmomentum.local", "User123!");

		mockMvc.perform(patch("/api/v1/user")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"currentPassword":"User123!","newPassword":"NewPass1","confirmationPassword":"NewPass1"}
								"""))
				.andExpect(status().isAccepted());

		mockMvc.perform(post("/api/v2/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"user@printmomentum.local","password":"User123!"}
								"""))
				.andExpect(status().isUnauthorized());

		mockMvc.perform(post("/api/v2/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"user@printmomentum.local","password":"NewPass1"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.token").isNotEmpty());
	}

	@Test
	void changePasswordRejectsWrongCurrentPassword() throws Exception {
		String token = login("user@printmomentum.local", "User123!");

		mockMvc.perform(patch("/api/v1/user")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"currentPassword":"wrong","newPassword":"NewPass1","confirmationPassword":"NewPass1"}
								"""))
				.andExpect(status().isNotAcceptable())
				.andExpect(jsonPath("$.detail").value("Wrong password"));
	}

	@Test
	void registerWithoutAuthIsUnauthorized() throws Exception {
		mockMvc.perform(post("/api/v1/user/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"new@printmomentum.local","password":"Secret1","name":"New User","role":"user"}
								"""))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void userRoleCannotRegister() throws Exception {
		String token = login("user@printmomentum.local", "User123!");

		mockMvc.perform(post("/api/v1/user/register")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"new@printmomentum.local","password":"Secret1","name":"New User","role":"user"}
								"""))
				.andExpect(status().isForbidden());
	}

	@Test
	void adminCanRegisterUserWhoThenLogsIn() throws Exception {
		String adminToken = login("admin@printmomentum.local", "Admin123!");

		mockMvc.perform(post("/api/v1/user/register")
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"fresh@printmomentum.local","password":"Secret1","name":"Fresh","role":"user"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isNumber());

		String createdToken = login("fresh@printmomentum.local", "Secret1");
		mockMvc.perform(get("/api/v1/user").header("Authorization", "Bearer " + createdToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value("fresh@printmomentum.local"))
				.andExpect(jsonPath("$.role").value("user"))
				.andExpect(jsonPath("$.name").value("Fresh"));
	}

	@Test
	void adminRegisterRejectsDuplicateEmail() throws Exception {
		String adminToken = login("admin@printmomentum.local", "Admin123!");

		mockMvc.perform(post("/api/v1/user/register")
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"user@printmomentum.local","password":"Secret1","name":"Dup","role":"user"}
								"""))
				.andExpect(status().isNotAcceptable())
				.andExpect(jsonPath("$.detail").value("Email already in use"));
	}

	@Test
	void userRoleCannotListMembers() throws Exception {
		String token = login("user@printmomentum.local", "User123!");

		mockMvc.perform(get("/api/v1/user/members").header("Authorization", "Bearer " + token))
				.andExpect(status().isForbidden());
	}

	@Test
	void adminCanListMembers() throws Exception {
		String adminToken = login("admin@printmomentum.local", "Admin123!");

		mockMvc.perform(get("/api/v1/user/members").header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].email").exists())
				.andExpect(jsonPath("$[0].active").value(true));
	}

	@Test
	void adminCanDeactivateUserWhoThenCannotLogin() throws Exception {
		String adminToken = login("admin@printmomentum.local", "Admin123!");

		MvcResult created = mockMvc.perform(post("/api/v1/user/register")
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"temp@printmomentum.local","password":"Secret1","name":"Temp","role":"user"}
								"""))
				.andExpect(status().isOk())
				.andReturn();
		int userId = Integer.parseInt(created.getResponse().getContentAsString());

		mockMvc.perform(patch("/api/v1/user/" + userId + "?status=false")
						.header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isAccepted());

		mockMvc.perform(post("/api/v2/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"temp@printmomentum.local","password":"Secret1"}
								"""))
				.andExpect(status().isNotAcceptable())
				.andExpect(jsonPath("$.detail").value("This account is deactivated"));
	}

	@Test
	void adminCannotDeactivateSelf() throws Exception {
		String adminToken = login("admin@printmomentum.local", "Admin123!");
		MvcResult me = mockMvc.perform(get("/api/v1/user").header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isOk())
				.andReturn();
		int adminId = com.jayway.jsonpath.JsonPath.read(me.getResponse().getContentAsString(), "$.id");

		mockMvc.perform(patch("/api/v1/user/" + adminId + "?status=false")
						.header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isNotAcceptable())
				.andExpect(jsonPath("$.detail").value("Cannot deactivate your own account"));
	}

	@Test
	void userCanUpdateDisplayName() throws Exception {
		String token = login("user@printmomentum.local", "User123!");

		mockMvc.perform(put("/api/v1/user")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":"Pat","displayName":"Pat Print"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Pat"))
				.andExpect(jsonPath("$.displayName").value("Pat Print"));
	}

	@Test
	@Order(1)
	void userCanChangeEmailWithCurrentPasswordThenLoginWithNewEmail() throws Exception {
		String nextEmail = "user-" + System.nanoTime() + "@printmomentum.local";
		String token = login("user@printmomentum.local", "User123!");

		mockMvc.perform(put("/api/v1/user")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":"User","displayName":"User","email":"%s","currentPassword":"User123!"}
								""".formatted(nextEmail)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value(nextEmail));

		mockMvc.perform(post("/api/v2/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"user@printmomentum.local","password":"User123!"}
								"""))
				.andExpect(status().isUnauthorized());

		mockMvc.perform(post("/api/v2/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"%s","password":"User123!"}
								""".formatted(nextEmail)))
				.andExpect(status().isOk());
	}

	@Test
	void adminCanReadMemberSessionsAndLastLogin() throws Exception {
		String adminToken = login("admin@printmomentum.local", "Admin123!");
		String members = mockMvc.perform(get("/api/v1/user/members").header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.email=='admin@printmomentum.local')].lastLoginAt").isNotEmpty())
				.andReturn()
				.getResponse()
				.getContentAsString();
		int adminId = com.jayway.jsonpath.JsonPath.read(
				mockMvc.perform(get("/api/v1/user").header("Authorization", "Bearer " + adminToken))
						.andReturn()
						.getResponse()
						.getContentAsString(),
				"$.id");

		mockMvc.perform(get("/api/v1/user/members/" + adminId + "/sessions")
						.header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].active").value(true))
				.andExpect(jsonPath("$[0].lastUsedAt").isNotEmpty());
		assert members.contains("admin@printmomentum.local");
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
