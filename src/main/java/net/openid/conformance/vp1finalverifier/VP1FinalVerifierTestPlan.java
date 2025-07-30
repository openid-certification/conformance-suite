package net.openid.conformance.vp1finalverifier;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.VariantSelection;

import java.util.List;
import java.util.Map;

@PublishTestPlan(
	testPlanName = "oid4vp-1final-verifier-test-plan",
	displayName = "OpenID for Verifiable Presentations 1.0 Final: Test a verifier - alpha tests (not currently part of certification program)",
	profile = TestPlan.ProfileNames.verifierTest,
	requireClientLog = false
)
public class VP1FinalVerifierTestPlan implements TestPlan {
	public static List<ModuleListEntry> testModulesWithVariants() {
		return List.of(
			new ModuleListEntry(
				List.of(
					// positive tests
					VP1FinalVerifierHappyFlow.class
				),
				List.of(
				)
			)
		);
	}
	public static String certificationProfileName(VariantSelection variant) {

		Map<String, String> v = variant.getVariant();
		String responseMode = v.get("response_mode");
		String credentialFormat = v.get("credential_format");
		String requestMethod = v.get("request_method");
		String clientIdPrefix = v.get("client_id_prefix");

		String certProfile = "OID4VP-1.0-FINAL Verifier";

		certProfile += " " + credentialFormat + " " + requestMethod + " " + clientIdPrefix + " " + responseMode;

		return certProfile;
	}

}
