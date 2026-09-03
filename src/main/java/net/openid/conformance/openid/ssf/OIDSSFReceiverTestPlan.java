package net.openid.conformance.openid.ssf;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.testmodule.TestModule;

import java.util.List;

@PublishTestPlan(
	testPlanName = "openid-ssf-receiver-test-plan",
	displayName = "OpenID Shared Signals Framework 1.0 Final: Receiver test - alpha tests (not part of certification program - use the CAEP Interop receiver plan to certify)",
	summary = "Collection of tests to verify the behavior of a OpenID Shared Signals Framework Receiver.",
	profile = TestPlan.ProfileNames.ssftest,
	specFamily = TestPlan.SpecFamilyNames.ssf
)
public class OIDSSFReceiverTestPlan implements TestPlan {

	public static final List<Class<? extends TestModule>> testModules = List.of(
		OIDSSFReceiverHappyPathTest.class,
		OIDSSFReceiverStreamCreateDeleteTest.class,
		OIDSSFReceiverStreamStatusUpdateTest.class,
		OIDSSFReceiverStreamVerificationTest.class,
		OIDSSFReceiverUnsolicitedStreamVerificationTest.class,
		OIDSSFReceiverSupportedEventsTest.class
	);

	@Override
	public List<ModuleListEntry> testModulesWithVariants() {
		return List.of(new ModuleListEntry(testModules, List.of()));
	}

}
