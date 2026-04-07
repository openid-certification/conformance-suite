package net.openid.conformance.openid.ssf;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.testmodule.TestModule;

import java.util.List;

@PublishTestPlan(
	testPlanName = "openid-ssf-transmitter-test-plan",
	displayName = "OpenID Shared Signals Framework 1.0 Final: Transmitter test (alpha version - may be incomplete or incorrect, please email certification@oidf.org)",
	summary = "Collection of tests to verify the behavior of a OpenID Shared Signals Framework Transmitter.",
	profile = TestPlan.ProfileNames.ssftest,
	specFamily = TestPlan.SpecFamilyNames.ssf
)
public class OIDSSFTransmitterTestPlan implements TestPlan {

	public static final List<Class<? extends TestModule>> testModules = List.of(
		OIDSSFTransmitterMetadataTest.class,
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
