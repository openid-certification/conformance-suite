package net.openid.conformance.vpid3verifier;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.VPID3VerifierCredentialFormat;
import net.openid.conformance.variant.VPID3VerifierResponseMode;
import net.openid.conformance.variant.VariantSelection;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Map;

@PublishTestPlan(
	testPlanName = "oid4vp-id3-verifier-test-plan",
	displayName = "OpenID for Verifiable Presentations ID3 (plus draft 24): Test a verifier - alpha tests (not currently part of certification program)",
	profile = TestPlan.ProfileNames.verifierTest
)
public class VPID3VerifierTestPlan implements TestPlan {
	public static List<ModuleListEntry> testModulesWithVariants() {
		return List.of(
			new ModuleListEntry(
				List.of(
					// positive tests
					VPID3VerifierHappyFlow.class
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
		String clientIDScheme = v.get("client_id_scheme");
		String queryLanguage = v.get("query_language");

		String certProfile = "OID4VPID3+draft24 Verifier";

		if (credentialFormat.equals(VPID3VerifierCredentialFormat.ISO_MDL.toString()) &&
			!responseMode.equals(VPID3VerifierResponseMode.DIRECT_POST_JWT.toString())) {
			throw new RuntimeException(String.format("Invalid configuration for %s: Direct POST JWT must be used for ISO mDL as the JWE header apu is needed to validate the mdoc device binding.",
				MethodHandles.lookup().lookupClass().getSimpleName()));
		}

		certProfile += " " + credentialFormat + " " + requestMethod + " " + clientIDScheme + " " + responseMode + " " + queryLanguage;

		return certProfile;
	}

}
