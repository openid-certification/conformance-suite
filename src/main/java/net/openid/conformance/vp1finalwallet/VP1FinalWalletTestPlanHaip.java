package net.openid.conformance.vp1finalwallet;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.VPProfile;
import net.openid.conformance.variant.VariantSelection;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

@PublishTestPlan(
	testPlanName = "oid4vp-1final-wallet-haip-test-plan",
	displayName = "OpenID for Verifiable Presentations 1.0 Final/HAIP: Test a wallet - alpha tests (not currently part of certification program)",
	profile = TestPlan.ProfileNames.wallettest
)
public class VP1FinalWalletTestPlanHaip implements TestPlan {
	@Override
	public List<ModuleListEntry> testModulesWithVariants() {

		var testModules = new ArrayList<>(VP1FinalWalletTestPlan.testModules);
		testModules.remove(VP1FinalWalletResponseUriNotClientId.class); // excluded due to @VariantNotApplicable with x509_hash

		return List.of(
			new ModuleListEntry(
				testModules,
				List.of(
					new Variant(VPProfile.class, "haip"),
					new Variant(VP1FinalWalletClientIdPrefix.class, "x509_hash")
					// new Variant(VP1FinalWalletRequestMethod.class, "request_uri_signed"),
				)
			)
		);
	}

	@Override
	public List<String> certificationProfileName(VariantSelection variantSelection) {

		String responseMode = variantSelection.getVariantParameterValue(VP1FinalWalletResponseMode.class);
		String credentialFormat = variantSelection.getVariantParameterValue(VP1FinalWalletCredentialFormat.class);
		String requestMethod = variantSelection.getVariantParameterValue(VP1FinalWalletRequestMethod.class);
		String clientIDPrefix = variantSelection.getVariantParameterValue(VP1FinalWalletClientIdPrefix.class);

		if (responseMode.equals(VP1FinalWalletResponseMode.DC_API.toString()) ||
			responseMode.equals(VP1FinalWalletResponseMode.DC_API_JWT.toString())) {
			if (requestMethod.equals(VP1FinalWalletRequestMethod.REQUEST_URI_UNSIGNED.toString())) {
				if (!clientIDPrefix.equals(VP1FinalWalletClientIdPrefix.WEB_ORIGIN.toString())) {
					throw new RuntimeException(String.format("Invalid configuration for %s: When using unsigned DC API requests the Client ID Prefix must be 'web_origin'.",
						MethodHandles.lookup().lookupClass().getSimpleName()));
				}
			} else if (requestMethod.equals(VP1FinalWalletRequestMethod.REQUEST_URI_SIGNED.toString())) {
				if (clientIDPrefix.equals(VP1FinalWalletClientIdPrefix.WEB_ORIGIN.toString())) {
					throw new RuntimeException(String.format("Invalid configuration for %s: When using signed DC API requests the Client ID Prefix must not be 'web_origin'.",
						MethodHandles.lookup().lookupClass().getSimpleName()));
				}
			}
		}

		String certProfile = String.format("%s %s %s %s %s", "OID4VP-1.0-FINAL+HAIP-1.0-FINAL", "Wallet", credentialFormat, requestMethod, responseMode);

		return List.of(certProfile);
	}


}
