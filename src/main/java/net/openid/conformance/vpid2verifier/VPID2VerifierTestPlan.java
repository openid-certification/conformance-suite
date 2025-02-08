package net.openid.conformance.vpid2verifier;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.VPID2VerifierClientIdScheme;
import net.openid.conformance.variant.VPID2VerifierCredentialFormat;
import net.openid.conformance.variant.VPID2VerifierRequestMethod;
import net.openid.conformance.variant.VPID2VerifierResponseMode;
import net.openid.conformance.variant.VariantSelection;

import java.util.List;
import java.util.Map;

@PublishTestPlan(
	testPlanName = "oid4vp-id2-verifier-test-plan",
	displayName = "OpenID for Verifiable Presentations ID2: Test a verifier - alpha tests (not currently part of certification program)",
	profile = TestPlan.ProfileNames.verifierTest
)
public class VPID2VerifierTestPlan implements TestPlan {
	public static List<ModuleListEntry> testModulesWithVariants() {
		return List.of(
			new ModuleListEntry(
				List.of(
					// positive tests
					VPID2VerifierHappyFlow.class
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

		String certProfile = "OID4VPID2 Verifier";

		if (credentialFormat.equals(VPID2VerifierCredentialFormat.ISO_MDL.toString()) &&
			responseMode.equals(VPID2VerifierResponseMode.DIRECT_POST_JWT.toString()) &&
			requestMethod.equals(VPID2VerifierRequestMethod.REQUEST_URI_SIGNED.toString()) &&
			clientIDScheme.equals(VPID2VerifierClientIdScheme.X509_SAN_DNS.toString())) {
			certProfile += " ISO 18013-7";
		} else {
			certProfile += " " + credentialFormat + " " + requestMethod + " " + clientIDScheme + " " + responseMode;
		}

		return certProfile;
	}

}
