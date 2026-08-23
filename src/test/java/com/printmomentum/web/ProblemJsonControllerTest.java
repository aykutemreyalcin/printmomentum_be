package com.printmomentum.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.printmomentum.ingest.EtsyUnavailableException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest
@AutoConfigureMockMvc
@Import(ProblemJsonControllerTest.EtsyDownProbe.class)
class ProblemJsonControllerTest {

	private static final String API_KEY = "test-printmomentum-key";

	@Autowired
	private MockMvc mockMvc;

	@Test
	void invalidPageSizeIsProblemJson400() throws Exception {
		mockMvc.perform(get("/api/v1/listings")
						.param("size", "1000")
						.header(ApiKeyInterceptor.HEADER, API_KEY))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.title").exists())
				.andExpect(jsonPath("$.trace").doesNotExist())
				.andExpect(jsonPath("$.stackTrace").doesNotExist());
	}

	@Test
	void missingListingIsProblemJson404() throws Exception {
		mockMvc.perform(get("/api/v1/listings/999999999").header(ApiKeyInterceptor.HEADER, API_KEY))
				.andExpect(status().isNotFound())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.title").exists())
				.andExpect(jsonPath("$.trace").doesNotExist());
	}

	@Test
	void etsyUnavailableIsProblemJson503() throws Exception {
		mockMvc.perform(get("/api/v1/__probe/etsy-down").header(ApiKeyInterceptor.HEADER, API_KEY))
				.andExpect(status().isServiceUnavailable())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(503))
				.andExpect(jsonPath("$.title").exists())
				.andExpect(jsonPath("$.trace").doesNotExist());
	}

	@TestConfiguration
	static class EtsyDownProbe {

		@RestController
		@RequestMapping("/api/v1")
		static class ProbeController {

			@GetMapping("/__probe/etsy-down")
			void etsyDown() {
				throw new EtsyUnavailableException("Etsy unavailable: HTTP 503");
			}
		}
	}
}
