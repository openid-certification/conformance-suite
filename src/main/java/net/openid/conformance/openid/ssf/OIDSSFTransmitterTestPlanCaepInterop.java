package net.openid.conformance.openid.ssf;

import net.openid.conformance.openid.ssf.variant.SsfProfile;
import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.VariantSelection;

import java.util.ArrayList;
import java.util.List;

@PublishTestPlan(
	testPlanName = "openid-ssf-transmitter-caep-test-plan",
	displayName = "OpenID Shared Signals Framework 1.0 Final/CAEP Interop Profile: Transmitter (alpha version - may be incomplete or incorrect, please email certification@oidf.org)",
	summary = "Collection of tests to verify the behavior of a OpenID Shared Signals Framework Transmitter according to the CAEP Interop Profile.",
	profile = TestPlan.ProfileNames.ssftest,
	specFamily = TestPlan.SpecFamilyNames.ssf
)
public class OIDSSFTransmitterTestPlanCaepInterop implements TestPlan {

	@Override
	public List<ModuleListEntry> testModulesWithVariants() {

		var testModules = new ArrayList<>(OIDSSFTransmitterTestPlan.testModules);

		testModules.removeAll(List.of(
			OIDSSFStreamControlNegativeTestUpdateStreamWithInvalidToken.class,
			OIDSSFStreamControlNegativeTestUpdateStreamWithInvalidBody.class,
			OIDSSFStreamControlNegativeTestUpdateUnknownStream.class,
			OIDSSFStreamControlNegativeTestReplaceStreamWithInvalidBody.class,
			OIDSSFStreamControlNegativeTestReplaceStreamWithInvalidToken.class,
			OIDSSFStreamControlNegativeTestReplaceUnknownStream.class,
			OIDSSFStreamSubjectControlHappyPathTest.class
		));

		return List.of(new ModuleListEntry(testModules, List.of(
			new Variant(SsfProfile.class, SsfProfile.CAEP_INTEROP)
		)));
	}

	@Override
	public List<String> certificationProfileName(VariantSelection variants) {
		return List.of("OpenID SSF Transmitter 1.0 Final+CAEP-Interop-1.0-ID2");
	}

}
