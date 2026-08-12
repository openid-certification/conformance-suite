package net.openid.conformance.variant;

import net.openid.conformance.info.Plan;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VariantCondition_UnitTest {

	private VariantService variantService;
	private VariantService.TestPlanHolder haipPlan;

	@BeforeEach
	void setUp() {
		variantService = new VariantService(holder -> true);
		haipPlan = variantService.getTestPlan("oid4vp-1final-wallet-haip-test-plan");
	}

	@Test
	void testDirectPostJwtIncludesSignedModulesOnly() {
		VariantSelection variant = new VariantSelection(Map.of(
			"response_mode", "direct_post.jwt",
			"credential_format", "sd_jwt_vc"
		));

		List<Plan.Module> modules = haipPlan.getTestModulesForVariant(variant);

		Set<String> moduleNames = modules.stream()
			.map(Plan.Module::getTestModule)
			.collect(Collectors.toSet());

		// direct_post.jwt entry uses x509_hash + request_uri_signed
		assertEquals(Set.of(
			"oid4vp-1final-wallet-alternate-happy-flow",
			"oid4vp-1final-wallet-happy-flow",
			"oid4vp-1final-wallet-request-uri-method-post",
			"oid4vp-1final-wallet-fewer-claims-than-available",
			"oid4vp-1final-wallet-optional-credential-set",
			"oid4vp-1final-wallet-no-claims-in-dcql-query",
			"oid4vp-1final-wallet-negative-test-invalid-request-object-signature",
			"oid4vp-1final-wallet-negative-test-mismatched-client-id",
			"oid4vp-1final-wallet-negative-test-redirect-uri-with-direct-post",
			"oid4vp-1final-wallet-negative-test-missing-nonce",
			"oid4vp-1final-wallet-negative-test-invalid-client-id-prefix",
			"oid4vp-1final-wallet-negative-test-unknown-transaction-data-type",
			"oid4vp-1final-wallet-negative-test-required-non-matching-credential",
			"oid4vp-1final-wallet-ignores-unusable-encryption-key"
		), moduleNames);

		// all modules should have the same fixed variants
		for (Plan.Module module : modules) {
			assertEquals("x509_hash", module.getVariant().get("client_id_prefix"));
			assertEquals("request_uri_signed", module.getVariant().get("request_method"));
		}
	}

	@Test
	void testDcApiJwtIncludesSignedAndUnsignedEntries() {
		VariantSelection variant = new VariantSelection(Map.of(
			"response_mode", "dc_api.jwt",
			"credential_format", "sd_jwt_vc"
		));

		List<Plan.Module> modules = haipPlan.getTestModulesForVariant(variant);

		// dc_api.jwt has three entries — distinguished by request_method:
		//   unsigned     (request_uri_unsigned,     web-origin)
		//   signed       (request_uri_signed,       x509_hash)
		//   multi-signed (request_uri_multisigned,  x509_hash)
		Map<String, List<Plan.Module>> byRequestMethod = modules.stream()
			.collect(Collectors.groupingBy(m -> m.getVariant().get("request_method")));

		assertTrue(byRequestMethod.containsKey("request_uri_unsigned"), "should have unsigned modules");
		assertTrue(byRequestMethod.containsKey("request_uri_signed"), "should have signed modules");

		// All unsigned modules use web-origin; all signed modules use x509_hash (HAIP §5).
		for (Plan.Module module : byRequestMethod.get("request_uri_unsigned")) {
			assertEquals("web-origin", module.getVariant().get("client_id_prefix"));
		}
		for (Plan.Module module : byRequestMethod.get("request_uri_signed")) {
			assertEquals("x509_hash", module.getVariant().get("client_id_prefix"));
		}

		// unsigned entry uses request_uri_unsigned and therefore excludes
		// InvalidRequestObjectSignature and WrongExpectedOrigins (both @VariantNotApplicable
		// for request_uri_unsigned). MismatchedClientIdInRequestObject is also excluded
		// via @VariantNotApplicable for dc_api.jwt. InvalidClientIdPrefix is excluded via
		// @VariantNotApplicableWhen because unsigned DC API requests contain no client_id
		// (OID4VP Appendix A.2).
		Set<String> unsignedModules = byRequestMethod.get("request_uri_unsigned").stream()
			.map(Plan.Module::getTestModule)
			.collect(Collectors.toSet());
		assertEquals(Set.of(
			"oid4vp-1final-wallet-alternate-happy-flow",
			"oid4vp-1final-wallet-happy-flow",
			"oid4vp-1final-wallet-fewer-claims-than-available",
			"oid4vp-1final-wallet-optional-credential-set",
			"oid4vp-1final-wallet-no-claims-in-dcql-query",
			"oid4vp-1final-wallet-negative-test-missing-nonce",
			"oid4vp-1final-wallet-negative-test-unknown-transaction-data-type",
			"oid4vp-1final-wallet-negative-test-required-non-matching-credential",
			"oid4vp-1final-wallet-ignores-unusable-encryption-key"
		), unsignedModules);

		// signed entry uses request_uri_signed and includes InvalidRequestObjectSignature.
		// MismatchedClientIdInRequestObject is excluded via @VariantNotApplicable for dc_api.jwt.
		Set<String> signedModules = byRequestMethod.get("request_uri_signed").stream()
			.map(Plan.Module::getTestModule)
			.collect(Collectors.toSet());
		assertEquals(Set.of(
			"oid4vp-1final-wallet-alternate-happy-flow",
			"oid4vp-1final-wallet-happy-flow",
			"oid4vp-1final-wallet-fewer-claims-than-available",
			"oid4vp-1final-wallet-optional-credential-set",
			"oid4vp-1final-wallet-no-claims-in-dcql-query",
			"oid4vp-1final-wallet-negative-test-invalid-request-object-signature",
			"oid4vp-1final-wallet-negative-test-missing-nonce",
			"oid4vp-1final-wallet-negative-test-invalid-client-id-prefix",
			"oid4vp-1final-wallet-negative-test-wrong-expected-origins",
			"oid4vp-1final-wallet-negative-test-unknown-transaction-data-type",
			"oid4vp-1final-wallet-negative-test-required-non-matching-credential",
			"oid4vp-1final-wallet-ignores-unusable-encryption-key"
		), signedModules);
	}

	@Test
	void testNonMatchingResponseModeReturnsNoModules() {
		VariantSelection variant = new VariantSelection(Map.of(
			"response_mode", "direct_post",
			"credential_format", "sd_jwt_vc"
		));

		List<Plan.Module> modules = haipPlan.getTestModulesForVariant(variant);

		assertTrue(modules.isEmpty(), "direct_post (non-jwt) should not match any HAIP entries");
	}

	// The plan-level configurationFields() unions the fixed-variant fields of every module
	// entry without evaluating the entry's VariantCondition, so the multisigned entry
	// (dc_api.jwt only) leaks client2.* into the always-visible list. The client2 jwks is
	// only needed for multi-signed requests, which are DC API only (OID4VP Appendix A.3.2),
	// so AbstractVP1FinalWalletTest hides it for the direct_post response modes.
	// client2.client_id is hidden by the fixed x509_hash prefix in both cases.
	@Test
	void testClient2FieldsNotVisibleForDirectPostJwt() {
		Set<String> visible = effectiveVisibleFields("direct_post.jwt");

		assertFalse(visible.contains("client2.jwks"), "client2.jwks should not be visible for direct_post.jwt");
		assertFalse(visible.contains("client2.client_id"), "client2.client_id should not be visible for direct_post.jwt");
	}

	@Test
	void testClient2JwksVisibleForDcApiJwt() {
		Set<String> visible = effectiveVisibleFields("dc_api.jwt");

		assertTrue(visible.contains("client2.jwks"), "client2.jwks should be visible for dc_api.jwt (multi-signed entry)");
		// client2.client_id stays hidden: the fixed x509_hash prefix derives the id from the certificate
		assertFalse(visible.contains("client2.client_id"), "client2.client_id should stay hidden for x509_hash");
	}

	// Mirrors the field visibility logic of schedule-test.html / config-form-adapter.js:
	// plan.configurationFields plus the selected variant values' configurationFields,
	// minus plan.hidesConfigurationFields and the selected values' hidesConfigurationFields.
	@SuppressWarnings("unchecked")
	private Set<String> effectiveVisibleFields(String responseMode) {
		Map<String, String> selection = Map.of(
			"response_mode", responseMode,
			"credential_format", "sd_jwt_vc"
		);

		Set<String> show = new HashSet<>(haipPlan.configurationFields());
		Set<String> hide = new HashSet<>(haipPlan.hidesConfigurationFields());

		Map<String, Map<String, Object>> summary = (Map<String, Map<String, Object>>) haipPlan.getVariantSummary();
		selection.forEach((param, value) -> {
			Map<String, Object> info = summary.get(param);
			if (info == null) {
				return;
			}
			Map<String, Map<String, Collection<String>>> variantValues =
				(Map<String, Map<String, Collection<String>>>) info.get("variantValues");
			Map<String, Collection<String>> valueData = variantValues.get(value);
			assertTrue(valueData != null, "variant value '" + value + "' not offered for parameter '" + param + "'");
			show.addAll(valueData.get("configurationFields"));
			hide.addAll(valueData.get("hidesConfigurationFields"));
		});

		show.removeAll(hide);
		return show;
	}

	@Test
	void testCertificationProfileForDcApiJwt() {
		VariantSelection variant = new VariantSelection(Map.of(
			"response_mode", "dc_api.jwt",
			"credential_format", "sd_jwt_vc"
		));

		List<String> certProfiles = haipPlan.certificationProfileForVariant(variant);

		assertEquals(1, certProfiles.size());
		assertEquals("OID4VP-1.0-FINAL+HAIP-1.0-FINAL Wallet sd_jwt_vc dc_api.jwt", certProfiles.get(0));
	}

	@Test
	void testCertificationProfileForDirectPostJwt() {
		VariantSelection variant = new VariantSelection(Map.of(
			"response_mode", "direct_post.jwt",
			"credential_format", "sd_jwt_vc"
		));

		List<String> certProfiles = haipPlan.certificationProfileForVariant(variant);

		assertEquals(1, certProfiles.size());
		assertEquals("OID4VP-1.0-FINAL+HAIP-1.0-FINAL Wallet sd_jwt_vc direct_post.jwt", certProfiles.get(0));
	}
}
