package net.openid.conformance.openbanking_brasil.plans.v1;

import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.testmodules.ResourcesDcmSubjectDnTestModule;
import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.FAPI1FinalOPProfile;

import java.util.List;

@PublishTestPlan(
	testPlanName = "resources-dcm-subjectdn-test-plan",
	profile = OBBProfile.DEV_ONLY,
	displayName = "Resources DCM test for changing subjectdn",
	summary = "For servers that support MTLS client authentication, check that subjectdn can be updated using the dynamic client management endpoint."
)
public class ResourcesDCMSubjectDnTestPlan implements TestPlan {
	public static List<ModuleListEntry> testModulesWithVariants() {
		return List.of(
			new ModuleListEntry(
				List.of(
					ResourcesDcmSubjectDnTestModule.class
				),
				List.of(
					new Variant(FAPI1FinalOPProfile.class, "openbanking_brazil")
				)
			)
		);
	}
}
