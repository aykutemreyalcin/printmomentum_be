package com.printmomentum.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.printmomentum.domain.QueryStats;
import com.printmomentum.domain.QueryStatsCalculator;
import com.printmomentum.domain.QueryStatsRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
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
@WithMockUser(roles = "user")
class QueryStatsControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private QueryStatsRepository queryStatsRepository;

	@Test
	void returnsLatestDayOnly() throws Exception {
		QueryStats older = new QueryStats("graphic tee", LocalDate.of(2026, 8, 20));
		older.apply(new QueryStatsCalculator.Row(4, 1000, new BigDecimal("20.00"), 10, 100));
		queryStatsRepository.save(older);
		QueryStats latest = new QueryStats("teacher shirt", LocalDate.of(2026, 8, 26));
		latest.apply(new QueryStatsCalculator.Row(8, 4200, new BigDecimal("26.50"), 33, 800));
		queryStatsRepository.save(latest);

		mockMvc.perform(get("/api/v1/query-stats"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].query").value("teacher shirt"))
				.andExpect(jsonPath("$[0].etsyCount").value(4200))
				.andExpect(jsonPath("$[0].medianPrice").value(26.50))
				.andExpect(jsonPath("$[0].listingCount").value(8));
	}
}
