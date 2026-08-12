package net.openid.conformance.variant;

import net.openid.conformance.info.Plan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.vp1finalwallet.VP1FinalWalletCredentialFormat;
import net.openid.conformance.vp1finalwallet.VP1FinalWalletResponseMode;
import net.openid.conformance.vp1finalwallet.VP1FinalWalletTestPlanHaip;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

	// Fixed-variant configuration fields of applicableWhen-guarded entries must not leak into
	// the flat plan-level lists; they are attached to the condition's variant value instead.
	@Test
	void testPlanLevelFieldsExcludeConditionalEntries() {
		List<String> fields = haipPlan.configurationFields();
		assertFalse(fields.contains("client2.jwks"), "client2.jwks should not be plan-level: " + fields);
		assertFalse(fields.contains("client2.client_id"), "client2.client_id should not be plan-level: " + fields);
		assertFalse(fields.contains("credential.trust_anchor_pem"),
			"credential.trust_anchor_pem should not be plan-level: " + fields);

		List<String> hides = haipPlan.hidesConfigurationFields();
		assertFalse(hides.contains("client2.client_id"), "client2.client_id hide should not be plan-level: " + hides);
	}

	@Test
	void testVariantSummaryCarriesConditionalFieldsPerResponseMode() {
		// dc_api.jwt is required by the multisigned entry, whose fixed request_uri_multisigned
		// variant needs the second client; x509_hash on the same entry hides client2.client_id
		Collection<String> dcApiFields = summaryFields("dc_api.jwt", "configurationFields");
		assertTrue(dcApiFields.contains("client2.jwks"), "dc_api.jwt fields: " + dcApiFields);
		assertTrue(dcApiFields.contains("client2.client_id"), "dc_api.jwt fields: " + dcApiFields);
		assertTrue(dcApiFields.contains("credential.trust_anchor_pem"), "dc_api.jwt fields: " + dcApiFields);
		Collection<String> dcApiHides = summaryFields("dc_api.jwt", "hidesConfigurationFields");
		assertTrue(dcApiHides.contains("client2.client_id"), "dc_api.jwt hides: " + dcApiHides);

		// no direct_post.jwt entry uses the multisigned request method
		Collection<String> directPostFields = summaryFields("direct_post.jwt", "configurationFields");
		assertTrue(directPostFields.contains("credential.trust_anchor_pem"),
			"direct_post.jwt fields: " + directPostFields);
		assertFalse(directPostFields.contains("client2.jwks"), "direct_post.jwt fields: " + directPostFields);
		Collection<String> directPostHides = summaryFields("direct_post.jwt", "hidesConfigurationFields");
		assertTrue(directPostHides.contains("client2.client_id"), "direct_post.jwt hides: " + directPostHides);
	}

	// Mirrors the UI: shown = configurationFields minus hidesConfigurationFields
	@Test
	void testNetEffectiveSecondClientFieldsPerResponseMode() {
		Set<String> dcApiNet = new HashSet<>(summaryFields("dc_api.jwt", "configurationFields"));
		dcApiNet.removeAll(summaryFields("dc_api.jwt", "hidesConfigurationFields"));
		assertTrue(dcApiNet.contains("client2.jwks"), "dc_api.jwt net fields: " + dcApiNet);
		assertFalse(dcApiNet.contains("client2.client_id"), "dc_api.jwt net fields: " + dcApiNet);

		Set<String> directPostNet = new HashSet<>(summaryFields("direct_post.jwt", "configurationFields"));
		directPostNet.removeAll(summaryFields("direct_post.jwt", "hidesConfigurationFields"));
		assertTrue(directPostNet.stream().noneMatch(f -> f.startsWith("client2.")),
			"direct_post.jwt must have no client2 fields: " + directPostNet);
	}

	@Test
	void testMultipleConditionsWithFixedFieldsRejected() {
		List<TestPlan.VariantCondition> twoConditions = List.of(
			new TestPlan.VariantCondition(VP1FinalWalletResponseMode.class, "dc_api.jwt"),
			new TestPlan.VariantCondition(VP1FinalWalletCredentialFormat.class, "sd_jwt_vc"));
		List<TestPlan.VariantCondition> oneCondition = List.of(
			new TestPlan.VariantCondition(VP1FinalWalletResponseMode.class, "dc_api.jwt"));

		assertThrows(RuntimeException.class, () -> VariantService.validateConditionalEntryFields(
			VP1FinalWalletTestPlanHaip.class, Object.class, twoConditions, List.of("client2.jwks"), List.of()));
		assertThrows(RuntimeException.class, () -> VariantService.validateConditionalEntryFields(
			VP1FinalWalletTestPlanHaip.class, Object.class, twoConditions, List.of(), List.of("client2.client_id")));

		assertDoesNotThrow(() -> VariantService.validateConditionalEntryFields(
			VP1FinalWalletTestPlanHaip.class, Object.class, oneCondition,
			List.of("client2.jwks"), List.of("client2.client_id")));
		assertDoesNotThrow(() -> VariantService.validateConditionalEntryFields(
			VP1FinalWalletTestPlanHaip.class, Object.class, twoConditions, List.of(), List.of()));
	}

	@SuppressWarnings("unchecked")
	private Collection<String> summaryFields(String responseModeValue, String key) {
		Map<String, Map<String, Object>> summary = (Map<String, Map<String, Object>>) haipPlan.getVariantSummary();
		Map<String, Object> variantValues = (Map<String, Object>) summary.get("response_mode").get("variantValues");
		Map<String, Object> valueData = (Map<String, Object>) variantValues.get(responseModeValue);
		return (Collection<String>) valueData.get(key);
	}
}
