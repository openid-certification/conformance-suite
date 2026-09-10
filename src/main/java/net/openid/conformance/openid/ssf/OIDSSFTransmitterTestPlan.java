package net.openid.conformance.openid.ssf;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.testmodule.TestModule;

import java.util.List;

@PublishTestPlan(
	testPlanName = "openid-ssf-transmitter-test-plan",
	displayName = "OpenID Shared Signals Framework 1.0 Final: Transmitter test - alpha tests (not part of certification program - use the CAEP Interop transmitter plan to certify)",
	summary = "Collection of tests to verify the behavior of a OpenID Shared Signals Framework Transmitter.",
	profile = TestPlan.ProfileNames.ssftest,
	specFamily = TestPlan.SpecFamilyNames.ssf
)
public class OIDSSFTransmitterTestPlan implements TestPlan {

	public static final List<Class<? extends TestModule>> testModules = List.of(
		OIDSSFTransmitterMetadataTest.class,
		// The modules that leave no stream behind run before the happy path: run-test-plan.py
		// executes the plan in this order, and the suite-vs-suite counterpart (the emulated
		// transmitter hosted by a receiver test module) finishes - and stops serving - once
		// the happy path completes the full stream lifecycle.
		OIDSSFStreamControlNegativeTestReadStreamWithoutAccessToken.class,
		OIDSSFStreamControlNegativeTestReadStreamWithTokenInUriQuery.class,
		OIDSSFStreamControlNegativeTestCreateStreamWithReadOnlyToken.class,
		OIDSSFStreamControlNegativeTestReadStreamStatusWithInvalidAccessToken.class,
		OIDSSFStreamControlNegativeTestReadStatusOfUnknownStream.class,
		OIDSSFTransmitterStreamVerificationNegativeTestInvalidBody.class,
		OIDSSFTransmitterStreamVerificationNegativeTestInvalidToken.class,
		OIDSSFTransmitterStreamVerificationNegativeTestUnknownStream.class,
		OIDSSFTransmitterPollEndpointAuthorizationTest.class,
		OIDSSFStreamControlHappyPathTest.class,
		OIDSSFStreamControlNegativeTestCreateStreamWithBrokenInput.class,
		OIDSSFStreamControlNegativeTestCreateStreamWithInvalidAccessToken.class,
		OIDSSFStreamControlNegativeTestCreateStreamWithDuplicateConfig.class,
		OIDSSFStreamControlNegativeTestReadStreamWithInvalidAccessToken.class,
		OIDSSFStreamControlNegativeTestReadUnknownStream.class,
		OIDSSFStreamControlNegativeTestUpdateStreamWithInvalidToken.class,
		OIDSSFStreamControlNegativeTestUpdateStreamWithInvalidBody.class,
		OIDSSFStreamControlNegativeTestUpdateUnknownStream.class,
		OIDSSFStreamControlNegativeTestReplaceStreamWithInvalidBody.class,
		OIDSSFStreamControlNegativeTestReplaceStreamWithInvalidToken.class,
		OIDSSFStreamControlNegativeTestReplaceUnknownStream.class,
		OIDSSFStreamControlNegativeTestDeleteStreamWithInvalidAccessToken.class,
		OIDSSFStreamControlNegativeTestDeleteUnknownStream.class,
		OIDSSFStreamSubjectControlHappyPathTest.class,
		OIDSSFTransmitterStreamVerificationPushTest.class,
		OIDSSFTransmitterStreamVerificationPollOnlyTest.class,
		OIDSSFTransmitterStreamVerificationPollAndAckTest.class,
		OIDSSFTransmitterStreamVerificationAckOnlyTest.class,
		OIDSSFTransmitterStreamVerificationPushNoAuthTest.class
	);

	@Override
	public List<ModuleListEntry> testModulesWithVariants() {
		return List.of(new ModuleListEntry(testModules, List.of()));
	}
}
