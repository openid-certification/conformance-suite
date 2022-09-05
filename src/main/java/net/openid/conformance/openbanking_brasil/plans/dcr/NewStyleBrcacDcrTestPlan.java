package net.openid.conformance.openbanking_brasil.plans.dcr;

import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.plans.PlanNames;
import net.openid.conformance.openbanking_brasil.testmodules.dcr.DcrBrcac2022SupportTestModule;
import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.FAPI1FinalOPProfile;

import java.util.List;

@PublishTestPlan(
	testPlanName = PlanNames.NEW_STYLE_BRCAC_DCR,
	profile = OBBProfile.OBB_PROFILE_DCR,
	displayName = PlanNames.NEW_STYLE_BRCAC_DCR,
	summary = PlanNames.NEW_STYLE_BRCAC_DCR
)
public class NewStyleBrcacDcrTestPlan implements TestPlan {

	public static List<ModuleListEntry> testModulesWithVariants() {
		return List.of(
			new ModuleListEntry(
				List.of(
					DcrBrcac2022SupportTestModule.class
				),
				List.of(
					new Variant(FAPI1FinalOPProfile.class, "openbanking_brazil")
				)
			)
		);
	}
}
