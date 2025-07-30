package net.openid.conformance.vpid2wallet;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.VariantSelection;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Map;

@PublishTestPlan(
	testPlanName = "oid4vp-id2-wallet-test-plan",
	displayName = "OpenID for Verifiable Presentations ID2: Test a wallet (alpha version - may be incomplete or incorrect, please email certification@oidf.org)",
	profile = TestPlan.ProfileNames.wallettest,
	requireClientLog = false
)
public class VPID2WalletTestPlan implements TestPlan {
	public static List<ModuleListEntry> testModulesWithVariants() {
		return List.of(
			new ModuleListEntry(
				List.of(
					// positive tests
					VPID2WalletHappyFlowNoState.class,
					VPID2WalletHappyFlowWithStateAndRedirect.class,

					// negative tests
					VPID2WalletResponseUriNotClientId.class,
					VPID2WalletInvalidRequestObjectSignature.class

					// negative tests:
					// try sending a redirect_uri in auth request with response_mode=direct_post
					// sending invalid client_id_scheme should cause an error?
					// flow without nonce
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

		String certProfile = "OID4VPID2";

		if (credentialFormat.equals(VPID2WalletCredentialFormat.ISO_MDL.toString()) &&
			!responseMode.equals(VPID2WalletResponseMode.DIRECT_POST_JWT.toString())) {
			throw new RuntimeException(String.format("Invalid configuration for %s: Direct POST JWT must be used for ISO mDL as the JWE header apu is needed to validate the mdoc device binding.",
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
