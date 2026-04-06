package net.openid.conformance.vp1finalwallet;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.VPProfile;
import net.openid.conformance.variant.VariantSelection;

import java.util.ArrayList;
import java.util.List;

@PublishTestPlan(
	testPlanName = "oid4vp-1final-wallet-haip-test-plan",
	displayName = "OpenID for Verifiable Presentations 1.0 Final/HAIP: Test a wallet - alpha tests (not currently part of certification program)",
	profile = TestPlan.ProfileNames.wallettest,
	specFamily = TestPlan.SpecFamilyNames.oid4vp,
	specVersion = TestPlan.SpecVersionNames.oid4vp1Final
)
public class VP1FinalWalletTestPlanHaip implements TestPlan {
	@Override
	public List<ModuleListEntry> testModulesWithVariants() {

		var testModules = new ArrayList<>(VP1FinalWalletTestPlan.testModules);
		testModules.remove(VP1FinalWalletResponseUriNotClientId.class); // excluded due to @VariantNotApplicable for all client_id_prefix and response_mode values used in HAIP

		var unsignedTestModules = new ArrayList<>(testModules);
		unsignedTestModules.remove(VP1FinalWalletInvalidRequestObjectSignature.class); // excluded due to @VariantNotApplicable with request_uri_unsigned

		// HAIP requires encrypted responses, so only direct_post.jwt and dc_api.jwt are supported
		return List.of(
			// direct_post.jwt: signed request, x509_hash
			new ModuleListEntry(
				testModules,
				List.of(
					new Variant(VPProfile.class, "haip"),
					new Variant(VP1FinalWalletClientIdPrefix.class, "x509_hash"),
					new Variant(VP1FinalWalletRequestMethod.class, "request_uri_signed")
				),
				List.of(new VariantCondition(VP1FinalWalletResponseMode.class,
					"direct_post.jwt"))
			),
			// dc_api.jwt + unsigned: web_origin
			new ModuleListEntry(
				unsignedTestModules,
				List.of(
					new Variant(VPProfile.class, "haip"),
					new Variant(VP1FinalWalletClientIdPrefix.class, "web-origin"),
					new Variant(VP1FinalWalletRequestMethod.class, "request_uri_unsigned")
				),
				List.of(new VariantCondition(VP1FinalWalletResponseMode.class,
					"dc_api.jwt"))
			),
			// dc_api.jwt + signed: x509_san_dns
			new ModuleListEntry(
				testModules,
				List.of(
					new Variant(VPProfile.class, "haip"),
					new Variant(VP1FinalWalletClientIdPrefix.class, "x509_san_dns"),
					new Variant(VP1FinalWalletRequestMethod.class, "request_uri_signed")
				),
				List.of(new VariantCondition(VP1FinalWalletResponseMode.class,
					"dc_api.jwt"))
			),
			// dc_api.jwt + multisigned: x509_hash
			new ModuleListEntry(
				testModules,
				List.of(
					new Variant(VPProfile.class, "haip"),
					new Variant(VP1FinalWalletClientIdPrefix.class, "x509_hash"),
					new Variant(VP1FinalWalletRequestMethod.class, "request_uri_multisigned")
				),
				List.of(new VariantCondition(VP1FinalWalletResponseMode.class,
					"dc_api.jwt"))
			)
		);
	}

	@Override
	public List<String> certificationProfileName(VariantSelection variantSelection) {
		// For HAIP, request_method and client_id_prefix are fixed by the plan's
		// VariantCondition entries (determined by response_mode), so only
		// response_mode and credential_format are user-selected and meaningful
		// in the certification profile name.
		String responseMode = variantSelection.getVariantParameterValue(VP1FinalWalletResponseMode.class);
		String credentialFormat = variantSelection.getVariantParameterValue(VP1FinalWalletCredentialFormat.class);

		String certProfile = String.format("%s %s %s %s", "OID4VP-1.0-FINAL+HAIP-1.0-FINAL", "Wallet", credentialFormat, responseMode);

		return List.of(certProfile);
	}



}
