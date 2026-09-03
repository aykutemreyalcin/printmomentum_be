package com.printmomentum.niche;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class NicheSlugTest {

	@Test
	void differentLabelsCanShareSlug() {
		assertThat(NicheSlug.fromLabel("boo p ghost dog"))
				.isEqualTo(NicheSlug.fromLabel("Boo-P Ghost Dog"));
	}
}
