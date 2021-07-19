package net.openid.conformance.openbanking_brasil.plans;

import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.testmodules.customerAPI.testmodule.CustomerBusinessDataApiTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.customerAPI.testmodule.CustomerBusinessWrongPermissionsTestModule;
import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;

@PublishTestPlan(
	testPlanName = "Business Customer Data api test",
	profile = OBBProfile.OBB_PROFILE,
	displayName = PlanNames.CUSTOMER_BUSINESS_DATA_API_PLAN_NAME,
	summary = "Structural and logical tests for OpenBanking Brasil-conformant Business Customer Data API",
	testModules = {
		CustomerBusinessDataApiTestModule.class,
		CustomerBusinessWrongPermissionsTestModule.class
	})
public class CustomerBusinessDataApiTestPlan implements TestPlan {
}
