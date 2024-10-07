package net.openid.conformance.openid.ssf;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.VariantSelection;

@PublishTestPlan(
	testPlanName = "openid-ssf-transmitter-test-plan",
	displayName = "OpenID Shared Signals Framework: Transmitter test (alpha - INCOMPLETE/INCORRECT, please email certification team if interested)",
	profile = TestPlan.ProfileNames.ssftest,
	testModules = {
		OpenIdSsfTransmitterMetadataTest.class,
		OpenIdSsfTransmitterStreamControlTest.class,
		OpenIdSsfTransmitterSubjectControlTest.class,
	}
)
public class OpenIdSsfTransmitterTestPlan implements TestPlan {

	public static String certificationProfileName(VariantSelection variant) {
		return "OpenID SSF Transmitter";
	}

}
