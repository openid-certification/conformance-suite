package net.openid.conformance.vpid2verifier;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.VariantSelection;
import net.openid.conformance.vpid2wallet.VPID2WalletClientIdScheme;
import net.openid.conformance.vpid2wallet.VPID2WalletCredentialFormat;
import net.openid.conformance.vpid2wallet.VPID2WalletRequestMethod;
import net.openid.conformance.vpid2wallet.VPID2WalletResponseMode;

import java.lang.invoke.MethodHandles;
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

		if (responseMode.equals(VPID2WalletResponseMode.W3C_DC_API.toString()) ||
			responseMode.equals(VPID2WalletResponseMode.W3C_DC_API_JWT.toString())) {
			throw new RuntimeException(String.format("Invalid configuration for %s: Browser API testing not supported yet",
				MethodHandles.lookup().lookupClass().getSimpleName()));
		}

		if (credentialFormat.equals(VPID2WalletCredentialFormat.ISO_MDL.toString()) &&
			responseMode.equals(VPID2WalletResponseMode.DIRECT_POST_JWT.toString()) &&
			requestMethod.equals(VPID2WalletRequestMethod.REQUEST_URI_SIGNED.toString()) &&
			clientIDScheme.equals(VPID2WalletClientIdScheme.X509_SAN_DNS.toString())) {
			certProfile += " ISO 18013-7";
		} else {
			certProfile += " " + credentialFormat + " " + requestMethod + " " + clientIDScheme + " " + responseMode;
		}

		return certProfile;
	}

}
