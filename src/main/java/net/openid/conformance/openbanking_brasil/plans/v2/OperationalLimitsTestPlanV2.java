package net.openid.conformance.openbanking_brasil.plans.v2;

import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.plans.PlanNames;
import net.openid.conformance.openbanking_brasil.testmodules.v2.PreFlightCheckOperationalV2Module;
import net.openid.conformance.openbanking_brasil.testmodules.v2.operationalLimits.*;
import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.FAPI1FinalOPProfile;

import java.util.List;

@PublishTestPlan(
	testPlanName = "Operational limits test " + PlanNames.LATEST_VERSION_2,
	profile = OBBProfile.OBB_PROFIlE_PHASE2_VERSION2,
	displayName = PlanNames.OPERATIONAL_LIMITS_PLAN_NAME_V2,
	summary = "Structural and logical tests for OpenBanking Brasil-conformant operational limits for customer data"
)

public class OperationalLimitsTestPlanV2 implements TestPlan {

	public static List<ModuleListEntry> testModulesWithVariants() {
		return List.of(
			new ModuleListEntry(
				List.of(
					PreFlightCheckOperationalV2Module.class,
					ResourcesApiOperationalLimitsTestModuleV2.class,
					CustomerBusinessApiOperationalLimitsTestModuleV2.class,
					CustomerPersonalApiOperationalLimitsTestModuleV2.class,
					ConsentsApiOperationalLimitsTestModuleV2.class,
					CreditCardsApiOperationalLimitsTestModuleV2.class,
					AccountsApiOperationalLimitsTestModule.class,
					LoansApiOperationalLimitsTestModuleV2.class,
					FinancingsApiOperationalLimitsTestModuleV2.class,
					InvoiceFinancingsApiOperationalLimitsTestModuleV2.class
					),
				List.of(
					new Variant(FAPI1FinalOPProfile.class, "openbanking_brazil")
				)
			)
		);
	}
}
