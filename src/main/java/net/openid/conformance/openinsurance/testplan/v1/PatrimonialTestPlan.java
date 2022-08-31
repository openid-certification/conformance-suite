package net.openid.conformance.openinsurance.testplan.v1;

import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.testmodules.account.testmodule.v2.OpinPatrimonialWrongPermissionsTestModule;
import net.openid.conformance.openinsurance.testmodule.patrimonial.v1.OpinPatrimonialApiTestModule;
import net.openid.conformance.openinsurance.testplan.utils.PlanNames;
import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;

import java.util.List;

@PublishTestPlan(
	testPlanName = "Insurance patrimonial api test",
	profile = OBBProfile.OBB_PROFILE_OPEN_INSURANCE_PHASE2,
	displayName = PlanNames.PATRIMONIAL_API_TEST_PLAN,
	summary = "Structural and logical tests for OpenInsurance Brasil-patrimonial customer API"
)

public class PatrimonialTestPlan implements TestPlan {
	public static List<ModuleListEntry> testModulesWithVariants() {
		return List.of(
			new ModuleListEntry(
				List.of(
					OpinPatrimonialApiTestModule.class,
					OpinPatrimonialWrongPermissionsTestModule.class
				),
				List.of(

				)
			)
		);
	}
}
