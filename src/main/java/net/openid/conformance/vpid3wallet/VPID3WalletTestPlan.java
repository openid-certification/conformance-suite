package net.openid.conformance.vpid3wallet;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.VariantSelection;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Map;

@PublishTestPlan(
	testPlanName = "oid4vp-id3-wallet-test-plan",
	displayName = "OpenID for Verifiable Presentations ID3 (plus draft 24): Test a wallet (alpha version - may be incomplete or incorrect, please email certification@oidf.org)",
	profile = TestPlan.ProfileNames.wallettest,
	requireClientLog = false
)
public class VPID3WalletTestPlan implements TestPlan {
	public static List<ModuleListEntry> testModulesWithVariants() {
		return List.of(
			new ModuleListEntry(
				List.of(
					// positive tests
					VPID3WalletHappyFlowNoState.class,
					VPID3WalletHappyFlowWithStateAndRedirect.class,

					// negative tests
					VPID3WalletResponseUriNotClientId.class,
					VPID3WalletInvalidRequestObjectSignature.class

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
		String queryLanguage = v.get("query_language");

		String certProfile = "OID4VPID3+draft24 Wallet";

		if (responseMode.equals(VPID3WalletResponseMode.DC_API.toString()) ||
			responseMode.equals(VPID3WalletResponseMode.DC_API_JWT.toString())) {
			if (requestMethod.equals(VPID3WalletRequestMethod.REQUEST_URI_UNSIGNED.toString())) {
				if (!clientIDScheme.equals(VPID3WalletClientIdScheme.WEB_ORIGIN.toString())) {
					throw new RuntimeException(String.format("Invalid configuration for %s: When using unsigned DC API requests the Client ID Scheme must be 'web_origin'.",
						MethodHandles.lookup().lookupClass().getSimpleName()));
				}
			} else if (requestMethod.equals(VPID3WalletRequestMethod.REQUEST_URI_SIGNED.toString())) {
				if (clientIDScheme.equals(VPID3WalletClientIdScheme.WEB_ORIGIN.toString())) {
					throw new RuntimeException(String.format("Invalid configuration for %s: When using signed DC API requests the Client ID Scheme must not be 'web_origin'.",
						MethodHandles.lookup().lookupClass().getSimpleName()));
				}
			}

		}

		if (credentialFormat.equals(VPID3WalletCredentialFormat.ISO_MDL.toString()) &&
			responseMode.equals(VPID3WalletResponseMode.DIRECT_POST.toString())) {
			throw new RuntimeException(String.format("Invalid configuration for %s: Direct POST (without JWT) cannot be used for ISO mDL as the JWE header apu is needed to validate the mdoc device binding.",
				MethodHandles.lookup().lookupClass().getSimpleName()));
		}

		certProfile += " " + credentialFormat + " " + requestMethod + " " + clientIDScheme + " " + responseMode + " " + queryLanguage;

		return certProfile;
	}

}
