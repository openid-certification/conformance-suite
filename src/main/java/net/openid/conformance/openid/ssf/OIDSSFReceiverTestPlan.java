package net.openid.conformance.openid.ssf;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.VariantSelection;

@PublishTestPlan(
	testPlanName = "openid-ssf-receiver-test-plan",
	displayName = "OpenID Shared Signals Framework: Receiver test (alpha - INCOMPLETE/INCORRECT, please email certification team if interested)",
	summary = "Collection of tests to verify the behavior of a OpenID Shared Signals Framework Receiver.",
	profile = TestPlan.ProfileNames.ssftest,
	testModules = {
		OIDSSFReceiverHappyPathTest.class
	}
)
public class OIDSSFReceiverTestPlan implements TestPlan {

	public static String certificationProfileName(VariantSelection variant) {
		return "OpenID SSF Receiver 1.0";
	}
}
