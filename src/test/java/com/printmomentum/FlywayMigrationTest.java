package com.printmomentum;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class FlywayMigrationTest {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void v2CreatesShopListingAndListingImage() {
		assertThat(tableCount("shop")).isEqualTo(1);
		assertThat(tableCount("listing")).isEqualTo(1);
		assertThat(tableCount("listing_image")).isEqualTo(1);
		assertThat(tableCount("listing_snapshot")).isEqualTo(1);
		assertThat(tableCount("api_client")).isEqualTo(1);
		assertThat(tableCount("user_role")).isEqualTo(1);
		assertThat(tableCount("app_user")).isEqualTo(1);
		assertThat(tableCount("user_session")).isEqualTo(1);
		assertThat(tableCount("user_favorite")).isEqualTo(1);
		assertThat(tableCount("listing_query")).isEqualTo(1);
		assertThat(tableCount("shop_crawl_queue")).isEqualTo(1);
		assertThat(flywayVersion()).isEqualTo("12");
	}

	@Test
	void insertOneListingWithShopAndImage() {
		jdbcTemplate.update(
				"INSERT INTO shop (shop_id, name, url) VALUES (?, ?, ?)",
				1L,
				"Print Shop",
				"https://www.etsy.com/shop/printshop");

		Timestamp now = Timestamp.from(Instant.parse("2026-08-23T12:00:00Z"));
		jdbcTemplate.update(
				"""
						INSERT INTO listing (
							listing_id, shop_id, title, description, url, taxonomy_id,
							price_amount, currency, tags, num_favorers, etsy_created_at, etsy_updated_at,
							print_tee_score, is_print_tee, first_seen_at, last_seen_at
						) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
						""",
				100L,
				1L,
				"Graphic print tee",
				"A DTG printed t-shirt",
				"https://www.etsy.com/listing/100",
				123L,
				new BigDecimal("24.99"),
				"USD",
				"[\"graphic\",\"tee\"]",
				42,
				now,
				now,
				new BigDecimal("0.850"),
				1,
				now,
				now);

		jdbcTemplate.update(
				"INSERT INTO listing_image (listing_id, url, `rank`) VALUES (?, ?, ?)",
				100L,
				"https://i.etsystatic.com/example.jpg",
				1);

		Integer listings = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM listing", Integer.class);
		assertThat(listings).isEqualTo(1);

		Integer images = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM listing_image WHERE listing_id = ?", Integer.class, 100L);
		assertThat(images).isEqualTo(1);
	}

	private int tableCount(String tableName) {
		Integer count = jdbcTemplate.queryForObject(
				"""
						SELECT COUNT(*) FROM information_schema.tables
						WHERE LOWER(table_name) = LOWER(?)
						  AND LOWER(table_schema) IN (LOWER(DATABASE()), 'public')
						""",
				Integer.class,
				tableName);
		return count == null ? 0 : count;
	}

	private String flywayVersion() {
		return jdbcTemplate.queryForObject(
				"SELECT version FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 1",
				String.class);
	}
}
