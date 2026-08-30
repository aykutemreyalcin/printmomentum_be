package com.printmomentum.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "printmomentum.niche")
public record NicheProperties(
		@DefaultValue("0.25") double maxIdfRatio,
		@DefaultValue("3") int minListingsForWindow,
		@DefaultValue("2") int lowDataListingCount,
		@DefaultValue("0.65") double cloneSimilarityThreshold,
		@DefaultValue("5") int etsyValidateMinListings,
		@DefaultValue("20") int etsyValidateMaxPerDay,
		@DefaultValue("2") int openNewEntrantsMin,
		@DefaultValue("0.40") double openBreakInRateMin,
		@DefaultValue("0.35") double openCloneDensityMax,
		@DefaultValue("0.35") double closingCloneDensityMin,
		@DefaultValue("0.40") double closingBreakInRateMax,
		@DefaultValue("30") int closedNewEntrantsDays,
		@DefaultValue("21") double closedIncumbentAgeDays,
		@DefaultValue("0.15") double closedBreakInRateMax) {
}
