package net.openid.conformance.openid.ssf;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.VariantSelection;

@PublishTestPlan(
	testPlanName = "openid-ssf-receiver-test-plan",
	displayName = "OpenID Shared Signals Framework: Receiver test (alpha version - may be incomplete or incorrect, please email certification@oidf.org)",
	summary = "Collection of tests to verify the behavior of a OpenID Shared Signals Framework Receiver.",
	profile = TestPlan.ProfileNames.ssftest,
	testModules = {
		OIDSSFReceiverHappyPathTest.class,
		OIDSSFReceiverStreamCreateDeleteTest.class,
		OIDSSFReceiverStreamStatusUpdateTest.class,
		OIDSSFReceiverStreamVerificationTest.class,
		OIDSSFReceiverSupportedEventsTest.class,
	}
)
public class OIDSSFReceiverTestPlan implements TestPlan {

	public static String certificationProfileName(VariantSelection variant) {
		return "OpenID SSF Receiver 1.0";
	}

}
