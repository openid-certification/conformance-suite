package net.openid.conformance.openinsurance.testplan.v1;

import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openinsurance.testmodule.structural.v1.OpinCustomerBusinessStructuralTestModule;
import net.openid.conformance.openinsurance.testmodule.structural.v1.OpinCustomerPersonalStructuralTestModule;
import net.openid.conformance.openinsurance.testmodule.structural.v1.OpinPatrimonialStructuralTestModule;
import net.openid.conformance.openinsurance.testplan.utils.PlanNames;
import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;

import java.util.List;

@PublishTestPlan(
	testPlanName = "Insurance structural tests testplan",
	profile = OBBProfile.OBB_PROFILE_OPEN_INSURANCE_PHASE2,
	displayName = PlanNames.STRUCTURAL_TEST_PLAN,
	summary = "Structural tests for OpenInsurance Brasil - Phase 2 - Customer Data"
)

public class StructuralTestPlan implements TestPlan{
	public static List<TestPlan.ModuleListEntry> testModulesWithVariants() {
		return List.of(
			new TestPlan.ModuleListEntry(
				List.of(
					OpinCustomerPersonalStructuralTestModule.class,
					OpinCustomerBusinessStructuralTestModule.class,
					OpinPatrimonialStructuralTestModule.class
				),
				List.of(

				)
			)
		);
	}
}
