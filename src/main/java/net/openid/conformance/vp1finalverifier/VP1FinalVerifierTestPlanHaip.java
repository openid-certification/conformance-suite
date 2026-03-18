package net.openid.conformance.vp1finalverifier;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.VPProfile;
import net.openid.conformance.variant.VariantSelection;

import java.util.ArrayList;
import java.util.List;

@PublishTestPlan(
	testPlanName = "oid4vp-1final-verifier-haip-test-plan",
	displayName = "OpenID for Verifiable Presentations 1.0 Final/HAIP: Test a verifier - alpha tests (not currently part of certification program)",
	profile = TestPlan.ProfileNames.verifierTest
)
public class VP1FinalVerifierTestPlanHaip implements TestPlan {

	@Override
	public List<ModuleListEntry> testModulesWithVariants() {
		var testModules = new ArrayList<>(VP1FinalVerifierTestPlan.testModules);

		return List.of(
			new ModuleListEntry(
				testModules,
				List.of(
					new Variant(VPProfile.class, "haip"),
					new Variant(VP1FinalVerifierClientIdPrefix.class, "x509_hash"),
					new Variant(VP1FinalVerifierRequestMethod.class, "request_uri_signed")
				)
			)
		);
	}
	@Override
	public List<String> certificationProfileName(VariantSelection variantSelection) {

		String responseMode = variantSelection.getVariantParameterValue(VP1FinalVerifierResponseMode.class);
		String credentialFormat = variantSelection.getVariantParameterValue(VP1FinalVerifierCredentialFormat.class);

		String certProfile = String.format("%s %s %s %s", "OID4VP-1.0-FINAL+HAIP-1.0-FINAL", "Verifier", credentialFormat, responseMode);

		return List.of(certProfile);
	}

}
