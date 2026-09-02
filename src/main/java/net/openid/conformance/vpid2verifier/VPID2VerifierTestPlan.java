package net.openid.conformance.vpid2verifier;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.VPID2VerifierCredentialFormat;
import net.openid.conformance.variant.VPID2VerifierResponseMode;
import net.openid.conformance.variant.VariantSelection;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Map;

@PublishTestPlan(
	testPlanName = "oid4vp-id2-verifier-test-plan",
	displayName = "OpenID for Verifiable Presentations ID2: Test a verifier - alpha tests (not part of certification program - use the OID4VP 1.0 Final HAIP verifier plan to certify)",
	profile = TestPlan.ProfileNames.verifierTest,
	specFamily = TestPlan.SpecFamilyNames.oid4vp,
	specVersion = TestPlan.SpecVersionNames.oid4vpId2
)
public class VPID2VerifierTestPlan implements TestPlan {
	@Override
	public List<ModuleListEntry> testModulesWithVariants() {
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
	/**
	 * This plan is not part of the certification program - the OID4VP 1.0 Final HAIP verifier plan
	 * is what verifiers certify against - so no profile name is returned. The method is still
	 * overridden because it is the only hook that runs at plan creation time, and it is where the
	 * variant combinations this plan cannot support are rejected.
	 */
	@Override
	public List<String> certificationProfileName(VariantSelection variant) {

		Map<String, String> v = variant.getVariant();
		String responseMode = v.get("response_mode");
		String credentialFormat = v.get("credential_format");

		if (credentialFormat.equals(VPID2VerifierCredentialFormat.ISO_MDL.toString()) &&
			!responseMode.equals(VPID2VerifierResponseMode.DIRECT_POST_JWT.toString())) {
			throw new RuntimeException(String.format("Invalid configuration for %s: Direct POST JWT must be used for ISO mDL as the JWE header apu is needed to validate the mdoc device binding.",
				MethodHandles.lookup().lookupClass().getSimpleName()));
		}

		return List.of();
	}

}
