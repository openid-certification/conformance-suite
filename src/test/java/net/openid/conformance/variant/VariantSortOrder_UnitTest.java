package net.openid.conformance.variant;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Tests that getVariantSummary() returns variants ordered by @VariantParameter sortOrder,
 * with ties (including the default 1000) broken alphabetically by variant name.
 */
class VariantSortOrder_UnitTest {

	// vp_profile (60) and credential_format (70) are ranked; the rest default to 1000 and sort alphabetically
	private static final List<String> EXPECTED_ORDER =
			List.of("vp_profile", "credential_format", "client_id_prefix", "request_method", "response_mode");

	private static VariantService variantService;

	@BeforeAll
	static void setUp() {
		variantService = new VariantService(holder -> true);
	}

	private static List<?> variantNames(Object summary) {
		return List.copyOf(((Map<?, ?>) summary).keySet());
	}

	@Test
	void planSummaryOrderedBySortOrder() {
		VariantService.TestPlanHolder plan = variantService.getTestPlan("oid4vp-1final-wallet-test-plan");
		assertNotNull(plan);

		assertEquals(EXPECTED_ORDER, variantNames(plan.getVariantSummary()));
	}

	@Test
	void moduleSummaryOrderedBySortOrder() {
		VariantService.TestModuleHolder module = variantService.getTestModule("oid4vp-1final-wallet-happy-flow");
		assertNotNull(module);

		assertEquals(EXPECTED_ORDER, variantNames(module.getVariantSummary()));
	}

}
