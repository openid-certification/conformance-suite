package net.openid.conformance.variant;

import net.openid.conformance.info.Plan;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
			"oid4vp-1final-wallet-happy-flow-no-state",
			"oid4vp-1final-wallet-request-uri-method-post",
			"oid4vp-1final-wallet-fewer-claims-than-available",
			"oid4vp-1final-wallet-optional-credential-set",
			"oid4vp-1final-wallet-no-claims-in-dcql-query",
			"oid4vp-1final-wallet-negative-test-invalid-request-object-signature",
			"oid4vp-1final-wallet-negative-test-mismatched-client-id",
			"oid4vp-1final-wallet-negative-test-redirect-uri-with-direct-post",
			"oid4vp-1final-wallet-negative-test-missing-nonce",
			"oid4vp-1final-wallet-negative-test-invalid-client-id-prefix"
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

		// dc_api.jwt has two entries: unsigned (web-origin) and signed (x509_san_dns)
		// unsigned entry excludes InvalidRequestObjectSignature
		Map<String, List<Plan.Module>> byPrefix = modules.stream()
			.collect(Collectors.groupingBy(m -> m.getVariant().get("client_id_prefix")));

		assertTrue(byPrefix.containsKey("web-origin"), "should have web-origin modules");
		assertTrue(byPrefix.containsKey("x509_san_dns"), "should have x509_san_dns modules");

		// web-origin entry uses request_uri_unsigned and excludes InvalidRequestObjectSignature
		// HappyFlowNoState is also excluded via @VariantNotApplicable for dc_api.jwt
		Set<String> webOriginModules = byPrefix.get("web-origin").stream()
			.map(Plan.Module::getTestModule)
			.collect(Collectors.toSet());
		assertEquals(Set.of(
			"oid4vp-1final-wallet-alternate-happy-flow",
			"oid4vp-1final-wallet-fewer-claims-than-available",
			"oid4vp-1final-wallet-optional-credential-set",
			"oid4vp-1final-wallet-no-claims-in-dcql-query",
			"oid4vp-1final-wallet-negative-test-mismatched-client-id",
			"oid4vp-1final-wallet-negative-test-missing-nonce",
			"oid4vp-1final-wallet-negative-test-invalid-client-id-prefix",
			"oid4vp-1final-wallet-negative-test-wrong-expected-origins"
		), webOriginModules);

		// x509_san_dns entry uses request_uri_signed and includes InvalidRequestObjectSignature
		Set<String> sanDnsModules = byPrefix.get("x509_san_dns").stream()
			.map(Plan.Module::getTestModule)
			.collect(Collectors.toSet());
		assertEquals(Set.of(
			"oid4vp-1final-wallet-alternate-happy-flow",
			"oid4vp-1final-wallet-fewer-claims-than-available",
			"oid4vp-1final-wallet-optional-credential-set",
			"oid4vp-1final-wallet-no-claims-in-dcql-query",
			"oid4vp-1final-wallet-negative-test-invalid-request-object-signature",
			"oid4vp-1final-wallet-negative-test-mismatched-client-id",
			"oid4vp-1final-wallet-negative-test-missing-nonce",
			"oid4vp-1final-wallet-negative-test-invalid-client-id-prefix",
			"oid4vp-1final-wallet-negative-test-wrong-expected-origins"
		), sanDnsModules);
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
}
