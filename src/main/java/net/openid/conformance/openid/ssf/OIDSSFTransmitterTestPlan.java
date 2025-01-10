package net.openid.conformance.openid.ssf;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.VariantSelection;

@PublishTestPlan(
	testPlanName = "openid-ssf-transmitter-test-plan",
	displayName = "OpenID Shared Signals Framework: Transmitter test (alpha - INCOMPLETE/INCORRECT, please email certification team if interested)",
	summary = "Collection of tests to verify the behavior of a OpenID Shared Signals Framework Transmitter.",
	profile = TestPlan.ProfileNames.ssftest,
	testModules = {
		OIDSSFTransmitterMetadataTest.class,
		OIDSSFStreamControlHappyPathTest.class,
		OIDSSFStreamControlErrorResponseTest.class,
		OIDSSFStreamSubjectControlHappyPathTest.class,
		OIDSSFTransmitterEventsTest.class,
	}
)
public class OIDSSFTransmitterTestPlan implements TestPlan {

	public static String certificationProfileName(VariantSelection variant) {
		return "OpenID SSF Transmitter 1.0";
	}

}
