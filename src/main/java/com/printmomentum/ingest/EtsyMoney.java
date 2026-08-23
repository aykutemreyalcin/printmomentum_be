package com.printmomentum.ingest;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record EtsyMoney(long amount, int divisor, String currencyCode) {

	public BigDecimal toDecimal() {
		if (divisor <= 0) {
			return BigDecimal.valueOf(amount);
		}
		return BigDecimal.valueOf(amount).divide(BigDecimal.valueOf(divisor), 2, RoundingMode.HALF_UP);
	}
}
