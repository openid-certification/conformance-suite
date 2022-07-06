package net.openid.conformance.openbanking_brasil.plans.v2;

import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.plans.PlanNames;
import net.openid.conformance.openbanking_brasil.testmodules.*;
import net.openid.conformance.openbanking_brasil.testmodules.v2.resources.ResourcesApiOperationalLimitsTestModuleV2;
import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.FAPI1FinalOPProfile;

import java.util.List;

@PublishTestPlan(
	testPlanName = "Resources api test " + PlanNames.LATEST_VERSION_2,
	profile = OBBProfile.OBB_PROFIlE_PHASE2,
	displayName = PlanNames.RESOURCES_API_PLAN_NAME_V2,
	summary = "Structural and logical tests for OpenBanking Brasil-conformant Resources API"
)
public class ResourcesApiTestPlanV2 implements TestPlan {
	public static List<ModuleListEntry> testModulesWithVariants() {
		return List.of(
			new ModuleListEntry(
				List.of(
					PreFlightCertCheckModule.class,
					ResourcesApiOperationalLimitsTestModuleV2.class
				),
				List.of(
					new Variant(FAPI1FinalOPProfile.class, "openbanking_brazil")
				)
			)
		);
	}
}
