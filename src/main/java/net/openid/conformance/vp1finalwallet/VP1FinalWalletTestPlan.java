package net.openid.conformance.vp1finalwallet;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.testmodule.TestModule;
import net.openid.conformance.variant.VariantSelection;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Map;

@PublishTestPlan(
	testPlanName = "oid4vp-1final-wallet-test-plan",
	displayName = "OpenID for Verifiable Presentations 1.0 Final: Test a wallet - alpha tests (not currently part of certification program)",
	profile = TestPlan.ProfileNames.wallettest,
	specFamily = TestPlan.SpecFamilyNames.oid4vp,
	specVersion = TestPlan.SpecVersionNames.oid4vp1Final
)
public class VP1FinalWalletTestPlan implements TestPlan {

	public static final List<Class<? extends TestModule>> testModules = List.of(
		// positive tests
		VP1FinalWalletHappyFlowNoState.class,
		VP1FinalWalletAlternateHappyFlow.class,
		VP1FinalWalletRequestUriMethodPost.class,

		// negative tests
		VP1FinalWalletResponseUriNotClientId.class,
		VP1FinalWalletInvalidRequestObjectSignature.class

		// negative tests:
		// try sending a redirect_uri in auth request with response_mode=direct_post
		// sending invalid client_id_scheme should cause an error?
		// flow without nonce
		// different client_id in request object and passed in url query? ("The Client Identifier value in the `client_id` Authorization Request parameter and the Request Object `client_id` claim value MUST be identical, including the Client Identifier Scheme.")
		// signed DC API request but no or wrong expected_origins
	);

	@Override
	public List<ModuleListEntry> testModulesWithVariants() {
		return List.of(
			new ModuleListEntry(
				testModules,
				List.of(
				)
			)
		);
	}
	@Override
	public List<String> certificationProfileName(VariantSelection variant) {

		Map<String, String> v = variant.getVariant();
		String responseMode = v.get("response_mode");
		String credentialFormat = v.get("credential_format");
		String requestMethod = v.get("request_method");
		String clientIDPrefix = v.get("client_id_prefix");

		String certProfile = "OID4VP-1.0-FINAL Wallet";

		if (requestMethod.equals(VP1FinalWalletRequestMethod.REQUEST_URI_SIGNED.toString()) ||
			requestMethod.equals(VP1FinalWalletRequestMethod.REQUEST_URI_MULTISIGNED.toString())) {
			if (clientIDPrefix.equals(VP1FinalWalletClientIdPrefix.REDIRECT_URI.toString())) {
				throw new RuntimeException(String.format("Invalid configuration for %s: Signed request methods do not permit the 'redirect_uri' Client ID Prefix.",
					MethodHandles.lookup().lookupClass().getSimpleName()));
			}
			if (clientIDPrefix.equals(VP1FinalWalletClientIdPrefix.WEB_ORIGIN.toString())) {
				throw new RuntimeException(String.format("Invalid configuration for %s: Signed request methods do not permit the 'web-origin' Client ID Prefix.",
					MethodHandles.lookup().lookupClass().getSimpleName()));
			}
		}

		if (responseMode.equals(VP1FinalWalletResponseMode.DC_API.toString()) ||
			responseMode.equals(VP1FinalWalletResponseMode.DC_API_JWT.toString())) {
			if (requestMethod.equals(VP1FinalWalletRequestMethod.REQUEST_URI_UNSIGNED.toString())) {
				if (!clientIDPrefix.equals(VP1FinalWalletClientIdPrefix.WEB_ORIGIN.toString())) {
					throw new RuntimeException(String.format("Invalid configuration for %s: When using unsigned DC API requests the Client ID Prefix must be 'web_origin'.",
						MethodHandles.lookup().lookupClass().getSimpleName()));
				}
			} else if (requestMethod.equals(VP1FinalWalletRequestMethod.REQUEST_URI_SIGNED.toString())) {
				// signed DC API uses the generic signed-request-method validation above
			} else if (requestMethod.equals(VP1FinalWalletRequestMethod.REQUEST_URI_MULTISIGNED.toString())) {
				// multi-signed DC API uses the generic signed-request-method validation above
			}
		} else if (requestMethod.equals(VP1FinalWalletRequestMethod.REQUEST_URI_MULTISIGNED.toString())) {
			throw new RuntimeException(String.format("Invalid configuration for %s: Multi-signed requests are only supported with DC API response modes (dc_api, dc_api.jwt).",
				MethodHandles.lookup().lookupClass().getSimpleName()));
		}

		certProfile += " " + credentialFormat + " " + requestMethod + " " + clientIDPrefix + " " + responseMode;

		return List.of(certProfile);
	}

}
