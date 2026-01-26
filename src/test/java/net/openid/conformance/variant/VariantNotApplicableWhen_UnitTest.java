package net.openid.conformance.variant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for @VariantNotApplicableWhen annotation support.
 * Tests use existing VCI wallet variants as the test case.
 */
class VariantNotApplicableWhen_UnitTest {

	private VariantService variantService;

	@BeforeEach
	void setUp() {
		variantService = new VariantService(holder -> true);
	}

	@Test
	void testConditionalExclusionDataStructure() {
		// Test that ConditionalExclusion correctly computes excluded values
		VariantService.ParameterHolder<VCICredentialOfferParameterVariant> subOptionParam =
			new VariantService.ParameterHolder<>(VCICredentialOfferParameterVariant.class);

		VariantService.ParameterHolder<VCIWalletAuthorizationCodeFlowVariant> flowParam =
			new VariantService.ParameterHolder<>(VCIWalletAuthorizationCodeFlowVariant.class);

		// Create a conditional exclusion: exclude all sub_option values when flow = wallet_initiated
		Map<String, Set<VCICredentialOfferParameterVariant>> excludedByCondition = Map.of(
			"wallet_initiated", Set.of(
				VCICredentialOfferParameterVariant.BY_VALUE,
				VCICredentialOfferParameterVariant.BY_REFERENCE
			)
		);

		VariantService.ConditionalExclusion<VCICredentialOfferParameterVariant> exclusion =
			new VariantService.ConditionalExclusion<>(flowParam, excludedByCondition);

		// When flow = wallet_initiated, both values should be excluded
		Map<VariantService.ParameterHolder<? extends Enum<?>>, Enum<?>> variantWalletInitiated = Map.of(
			flowParam, VCIWalletAuthorizationCodeFlowVariant.WALLET_INITIATED
		);
		Set<VCICredentialOfferParameterVariant> excludedForWalletInitiated =
			exclusion.getExcludedValues(variantWalletInitiated);

		assertEquals(2, excludedForWalletInitiated.size());
		assertTrue(excludedForWalletInitiated.contains(VCICredentialOfferParameterVariant.BY_VALUE));
		assertTrue(excludedForWalletInitiated.contains(VCICredentialOfferParameterVariant.BY_REFERENCE));

		// When flow = issuer_initiated, nothing should be excluded
		Map<VariantService.ParameterHolder<? extends Enum<?>>, Enum<?>> variantIssuerInitiated = Map.of(
			flowParam, VCIWalletAuthorizationCodeFlowVariant.ISSUER_INITIATED
		);
		Set<VCICredentialOfferParameterVariant> excludedForIssuerInitiated =
			exclusion.getExcludedValues(variantIssuerInitiated);

		assertTrue(excludedForIssuerInitiated.isEmpty());
	}

	@Test
	void testTestModuleVariantInfoGetAllowedValuesForVariant() {
		// Test that TestModuleVariantInfo correctly computes effective allowed values
		VariantService.ParameterHolder<VCICredentialOfferParameterVariant> subOptionParam =
			new VariantService.ParameterHolder<>(VCICredentialOfferParameterVariant.class);

		VariantService.ParameterHolder<VCIWalletAuthorizationCodeFlowVariant> flowParam =
			new VariantService.ParameterHolder<>(VCIWalletAuthorizationCodeFlowVariant.class);

		// Create conditional exclusion
		Map<String, Set<VCICredentialOfferParameterVariant>> excludedByCondition = Map.of(
			"wallet_initiated", Set.of(
				VCICredentialOfferParameterVariant.BY_VALUE,
				VCICredentialOfferParameterVariant.BY_REFERENCE
			)
		);

		VariantService.ConditionalExclusion<VCICredentialOfferParameterVariant> exclusion =
			new VariantService.ConditionalExclusion<>(flowParam, excludedByCondition);

		// Create TestModuleVariantInfo with the conditional exclusion
		VariantService.TestModuleVariantInfo<VCICredentialOfferParameterVariant> variantInfo =
			new VariantService.TestModuleVariantInfo<>(
				subOptionParam,
				Set.of(),  // no static exclusions
				Map.of(),  // no config fields
				Map.of(),  // no hidden config fields
				Map.of(),  // no setup methods
				java.util.List.of(exclusion)
			);

		// When flow = wallet_initiated, no values should be allowed
		Map<VariantService.ParameterHolder<? extends Enum<?>>, Enum<?>> variantWalletInitiated = Map.of(
			flowParam, VCIWalletAuthorizationCodeFlowVariant.WALLET_INITIATED
		);
		Set<VCICredentialOfferParameterVariant> allowedForWalletInitiated =
			variantInfo.getAllowedValuesForVariant(variantWalletInitiated);

		assertTrue(allowedForWalletInitiated.isEmpty(),
			"No values should be allowed when all are conditionally excluded");
		assertTrue(variantInfo.isFullyExcludedForVariant(variantWalletInitiated),
			"Parameter should be fully excluded");

		// When flow = issuer_initiated, all values should be allowed
		Map<VariantService.ParameterHolder<? extends Enum<?>>, Enum<?>> variantIssuerInitiated = Map.of(
			flowParam, VCIWalletAuthorizationCodeFlowVariant.ISSUER_INITIATED
		);
		Set<VCICredentialOfferParameterVariant> allowedForIssuerInitiated =
			variantInfo.getAllowedValuesForVariant(variantIssuerInitiated);

		assertEquals(2, allowedForIssuerInitiated.size(),
			"All values should be allowed when condition is not met");
		assertFalse(variantInfo.isFullyExcludedForVariant(variantIssuerInitiated),
			"Parameter should not be fully excluded");
	}

	@Test
	void testGetConditionalExclusionsForUi() {
		// Test that getConditionalExclusionsForUi returns correct structure
		VariantService.ParameterHolder<VCICredentialOfferParameterVariant> subOptionParam =
			new VariantService.ParameterHolder<>(VCICredentialOfferParameterVariant.class);

		VariantService.ParameterHolder<VCIWalletAuthorizationCodeFlowVariant> flowParam =
			new VariantService.ParameterHolder<>(VCIWalletAuthorizationCodeFlowVariant.class);

		// Create conditional exclusion
		Map<String, Set<VCICredentialOfferParameterVariant>> excludedByCondition = Map.of(
			"wallet_initiated", Set.of(
				VCICredentialOfferParameterVariant.BY_VALUE,
				VCICredentialOfferParameterVariant.BY_REFERENCE
			)
		);

		VariantService.ConditionalExclusion<VCICredentialOfferParameterVariant> exclusion =
			new VariantService.ConditionalExclusion<>(flowParam, excludedByCondition);

		VariantService.TestModuleVariantInfo<VCICredentialOfferParameterVariant> variantInfo =
			new VariantService.TestModuleVariantInfo<>(
				subOptionParam,
				Set.of(),
				Map.of(),
				Map.of(),
				Map.of(),
				java.util.List.of(exclusion)
			);

		// Get the UI representation
		Map<String, Map<String, Set<String>>> uiInfo = variantInfo.getConditionalExclusionsForUi();

		assertNotNull(uiInfo);
		assertTrue(uiInfo.containsKey("vci_authorization_code_flow_variant"),
			"Should contain the condition parameter name");

		Map<String, Set<String>> byFlowType = uiInfo.get("vci_authorization_code_flow_variant");
		assertTrue(byFlowType.containsKey("wallet_initiated"),
			"Should contain the condition value");

		Set<String> excludedValues = byFlowType.get("wallet_initiated");
		assertTrue(excludedValues.contains("by_value"));
		assertTrue(excludedValues.contains("by_reference"));
	}

	@Test
	void testPartialExclusion() {
		// Test partial exclusion (only some values excluded)
		VariantService.ParameterHolder<VCICredentialOfferParameterVariant> subOptionParam =
			new VariantService.ParameterHolder<>(VCICredentialOfferParameterVariant.class);

		VariantService.ParameterHolder<VCIWalletAuthorizationCodeFlowVariant> flowParam =
			new VariantService.ParameterHolder<>(VCIWalletAuthorizationCodeFlowVariant.class);

		// Only exclude BY_REFERENCE when flow = wallet_initiated
		Map<String, Set<VCICredentialOfferParameterVariant>> excludedByCondition = Map.of(
			"wallet_initiated", Set.of(VCICredentialOfferParameterVariant.BY_REFERENCE)
		);

		VariantService.ConditionalExclusion<VCICredentialOfferParameterVariant> exclusion =
			new VariantService.ConditionalExclusion<>(flowParam, excludedByCondition);

		VariantService.TestModuleVariantInfo<VCICredentialOfferParameterVariant> variantInfo =
			new VariantService.TestModuleVariantInfo<>(
				subOptionParam,
				Set.of(),
				Map.of(),
				Map.of(),
				Map.of(),
				java.util.List.of(exclusion)
			);

		Map<VariantService.ParameterHolder<? extends Enum<?>>, Enum<?>> variantWalletInitiated = Map.of(
			flowParam, VCIWalletAuthorizationCodeFlowVariant.WALLET_INITIATED
		);
		Set<VCICredentialOfferParameterVariant> allowed =
			variantInfo.getAllowedValuesForVariant(variantWalletInitiated);

		assertEquals(1, allowed.size(), "Only BY_VALUE should be allowed");
		assertTrue(allowed.contains(VCICredentialOfferParameterVariant.BY_VALUE));
		assertFalse(allowed.contains(VCICredentialOfferParameterVariant.BY_REFERENCE));

		// Parameter is not fully excluded since BY_VALUE is still allowed
		assertFalse(variantInfo.isFullyExcludedForVariant(variantWalletInitiated));
	}

	@Test
	void testMultipleConditionValues() {
		// Test exclusion when condition has multiple possible values
		VariantService.ParameterHolder<VCICredentialOfferParameterVariant> subOptionParam =
			new VariantService.ParameterHolder<>(VCICredentialOfferParameterVariant.class);

		VariantService.ParameterHolder<VCIWalletAuthorizationCodeFlowVariant> flowParam =
			new VariantService.ParameterHolder<>(VCIWalletAuthorizationCodeFlowVariant.class);

		// Exclude BY_REFERENCE for both issuer_initiated and issuer_initiated_dc_api
		Map<String, Set<VCICredentialOfferParameterVariant>> excludedByCondition = Map.of(
			"issuer_initiated", Set.of(VCICredentialOfferParameterVariant.BY_REFERENCE),
			"issuer_initiated_dc_api", Set.of(VCICredentialOfferParameterVariant.BY_REFERENCE)
		);

		VariantService.ConditionalExclusion<VCICredentialOfferParameterVariant> exclusion =
			new VariantService.ConditionalExclusion<>(flowParam, excludedByCondition);

		VariantService.TestModuleVariantInfo<VCICredentialOfferParameterVariant> variantInfo =
			new VariantService.TestModuleVariantInfo<>(
				subOptionParam,
				Set.of(),
				Map.of(),
				Map.of(),
				Map.of(),
				java.util.List.of(exclusion)
			);

		// Test issuer_initiated
		Map<VariantService.ParameterHolder<? extends Enum<?>>, Enum<?>> variantIssuerInitiated = Map.of(
			flowParam, VCIWalletAuthorizationCodeFlowVariant.ISSUER_INITIATED
		);
		Set<VCICredentialOfferParameterVariant> allowedIssuerInitiated =
			variantInfo.getAllowedValuesForVariant(variantIssuerInitiated);

		assertEquals(1, allowedIssuerInitiated.size());
		assertTrue(allowedIssuerInitiated.contains(VCICredentialOfferParameterVariant.BY_VALUE));

		// Test issuer_initiated_dc_api
		Map<VariantService.ParameterHolder<? extends Enum<?>>, Enum<?>> variantDcApi = Map.of(
			flowParam, VCIWalletAuthorizationCodeFlowVariant.ISSUER_INITIATED_DC_API
		);
		Set<VCICredentialOfferParameterVariant> allowedDcApi =
			variantInfo.getAllowedValuesForVariant(variantDcApi);

		assertEquals(1, allowedDcApi.size());
		assertTrue(allowedDcApi.contains(VCICredentialOfferParameterVariant.BY_VALUE));

		// Test wallet_initiated (no exclusion)
		Map<VariantService.ParameterHolder<? extends Enum<?>>, Enum<?>> variantWalletInitiated = Map.of(
			flowParam, VCIWalletAuthorizationCodeFlowVariant.WALLET_INITIATED
		);
		Set<VCICredentialOfferParameterVariant> allowedWalletInitiated =
			variantInfo.getAllowedValuesForVariant(variantWalletInitiated);

		assertEquals(2, allowedWalletInitiated.size());
	}
}
