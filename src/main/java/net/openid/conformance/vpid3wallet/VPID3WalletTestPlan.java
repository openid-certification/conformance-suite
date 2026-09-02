package net.openid.conformance.vpid3wallet;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.VariantSelection;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Map;

@PublishTestPlan(
	testPlanName = "oid4vp-id3-wallet-test-plan",
	displayName = "OpenID for Verifiable Presentations ID3 (plus draft 24): Test a wallet - alpha tests (not part of certification program - use the OID4VP 1.0 Final HAIP wallet plan to certify)",
	profile = TestPlan.ProfileNames.wallettest,
	specFamily = TestPlan.SpecFamilyNames.oid4vp,
	specVersion = TestPlan.SpecVersionNames.oid4vpId3
)
public class VPID3WalletTestPlan implements TestPlan {
	@Override
	public List<ModuleListEntry> testModulesWithVariants() {
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
	/**
	 * This plan is not part of the certification program - the OID4VP 1.0 Final HAIP wallet plan
	 * is what wallets certify against - so no profile name is returned. The method is still
	 * overridden because it is the only hook that runs at plan creation time, and it is where the
	 * variant combinations this plan cannot support are rejected.
	 */
	@Override
	public List<String> certificationProfileName(VariantSelection variant) {

		Map<String, String> v = variant.getVariant();
		String responseMode = v.get("response_mode");
		String credentialFormat = v.get("credential_format");
		String requestMethod = v.get("request_method");
		String clientIDScheme = v.get("client_id_scheme");

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

		return List.of();
	}

}
