package net.openid.conformance.openid.ssf;

import net.openid.conformance.openid.ssf.variant.SsfAuthMode;
import net.openid.conformance.openid.ssf.variant.SsfDeliveryMode;
import net.openid.conformance.openid.ssf.variant.SsfProfile;
import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.testmodule.TestModule;
import net.openid.conformance.variant.VariantSelection;

import java.util.ArrayList;
import java.util.List;

@PublishTestPlan(
	testPlanName = "openid-ssf-receiver-caep-test-plan",
	displayName = "OpenID Shared Signals Framework 1.0 Final/CAEP Interop Profile: Receiver test - alpha tests (not currently part of certification program - please email certification@oidf.org)",
	summary = "Collection of tests to verify the behavior of a OpenID Shared Signals Framework Receiver according to the CAEP Interop Profile.",
	profile = TestPlan.ProfileNames.ssftest,
	specFamily = TestPlan.SpecFamilyNames.ssf
)
public class OIDSSFReceiverTestPlanCaepInterop implements TestPlan {

	@Override
	public List<ModuleListEntry> testModulesWithVariants() {
		List<Class<? extends TestModule>> testModules = new ArrayList<>(OIDSSFReceiverTestPlan.testModules);
		// Add explicit Caep interop test
		testModules.add(OIDSSFReceiverStreamCaepInteropTest.class);
		testModules.removeAll(
			List.of(
				// Happy path tests are not relevant to CAEP-Interop as stream update / replace operations are not supported
				OIDSSFReceiverHappyPathTest.class,

				// Happy path tests are not relevant to CAEP-Interop as stream update / replace operations are not supported
				OIDSSFReceiverStreamStatusUpdateTest.class
			)
		);

		return List.of(new ModuleListEntry(testModules,
			List.of(
				new Variant(SsfProfile.class, SsfProfile.CAEP_INTEROP)
			)));
	}

	@Override
	public List<String> certificationProfileName(VariantSelection variantSelection) {
		// The CAEP Interop Profile (2.4.3 / 2.7) requires receivers to use OAuth 2.0, so only
		// dynamic auth mode is certifiable. Static auth (a pre-shared bearer string) remains
		// selectable for testing but yields no certification profile name.
		String authMode = variantSelection.getVariantParameterValue(SsfAuthMode.class);
		if (!SsfAuthMode.DYNAMIC.name().equalsIgnoreCase(authMode)) {
			return List.of();
		}
		String deliveryMethod = variantSelection.getVariantParameterValue(SsfDeliveryMode.class);
		return List.of(String.format("%s %s %s", "OIDSSF-1.0-FINAL+CAEPIOP-1.0-FINAL", "Receiver", deliveryMethod));
	}

}
