package net.openid.conformance.openid.ssf;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.VariantSelection;

import java.util.List;

@PublishTestPlan(
	testPlanName = "openid-ssf-transmitter-test-plan",
	displayName = "OpenID Shared Signals Framework 1.0 Final: Transmitter",
	summary = "Collection of tests to verify the behavior of a OpenID Shared Signals Framework Transmitter.",
	profile = TestPlan.ProfileNames.ssftest,
	specFamily = TestPlan.SpecFamilyNames.ssf,
	testModules = {
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
		OIDSSFTransmitterStreamVerificationEventsTest.class,
		OIDSSFTransmitterStreamVerificationPushNoAuthTest.class,
	}
)
public class OIDSSFTransmitterTestPlan implements TestPlan {

	@Override
	public List<String> certificationProfileName(VariantSelection variant) {
		return List.of("OpenID SSF Transmitter 1.0");
	}

}
