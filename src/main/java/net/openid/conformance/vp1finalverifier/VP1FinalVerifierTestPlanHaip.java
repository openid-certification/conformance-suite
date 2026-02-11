package net.openid.conformance.vp1finalverifier;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.VPProfile;
import net.openid.conformance.variant.VariantSelection;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@PublishTestPlan(
	testPlanName = "oid4vp-1final-verifier-haip-test-plan",
	displayName = "OpenID for Verifiable Presentations 1.0 Final/HAIP: Test a verifier - alpha tests (not currently part of certification program)",
	profile = TestPlan.ProfileNames.verifierTest
)
public class VP1FinalVerifierTestPlanHaip implements TestPlan {

	public static List<ModuleListEntry> testModulesWithVariants() {
		var testModules = new ArrayList<>(VP1FinalVerifierTestPlan.testModules);

		return List.of(
			new ModuleListEntry(
				testModules,
				List.of(
					new Variant(VPProfile.class, "haip"),
					new Variant(VP1FinalVerifierClientIdPrefix.class, "x509_hash"),
					new Variant(VP1FinalVerifierRequestMethod.class, "request_uri_signed")
				)
			)
		);
	}
	public static String certificationProfileName(VariantSelection variant) {

		Map<String, String> v = variant.getVariant();
		String vpProfile = v.get("vp_profile");
		String responseMode = v.get("response_mode");
		String credentialFormat = v.get("credential_format");
		String requestMethod = v.get("request_method");
		String clientIdPrefix = v.get("client_id_prefix");

		String certProfile = "OID4VP-1.0-FINAL Verifier";

		certProfile += " " + vpProfile + " " + credentialFormat + " " + requestMethod + " " + clientIdPrefix + " " + responseMode;

		return certProfile;
	}

}
