package com.printmomentum.ingest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@TestPropertySource(properties = {
		"printmomentum.ingest.pages-per-query=1",
		"printmomentum.ingest.created-page=false"
})
class ListingIngestQuotaTest {

	@Autowired
	private ListingIngestJob ingestJob;

	@Autowired
	private EtsyQuotaTracker quotaTracker;

	@Autowired
	private MeterRegistry meterRegistry;

	@MockitoBean
	private EtsyClient etsyClient;

	@AfterEach
	void resetQuota() {
		quotaTracker.clear();
	}

	@Test
	void remainingFiveSkipsJob() {
		quotaTracker.recordRemainingToday(5);

		IngestResult result = ingestJob.run();

		assertThat(result.stored()).isZero();
		assertThat(result.skipped()).isZero();
		assertThat(result.crawlRunId()).isNull();
		verify(etsyClient, never()).searchActive(anyString(), nullable(Long.class), anyInt(), anyInt());
		assertThat(meterRegistry.find("etsy.remaining.today").gauge()).isNotNull();
		assertThat(meterRegistry.find("etsy.remaining.today").gauge().value()).isEqualTo(5.0);
	}

	@Test
	void remainingFiveThousandRunsJob() {
		quotaTracker.recordRemainingToday(5000);
		when(etsyClient.searchActive(anyString(), nullable(Long.class), anyInt(), anyInt()))
				.thenReturn(new EtsySearchPage(0, List.of()));

		IngestResult result = ingestJob.run();

		assertThat(result.crawlRunId()).isNotNull();
		verify(etsyClient, atLeastOnce()).searchActive(anyString(), nullable(Long.class), anyInt(), anyInt());
		assertThat(meterRegistry.find("etsy.remaining.today").gauge().value()).isEqualTo(5000.0);
	}
}
