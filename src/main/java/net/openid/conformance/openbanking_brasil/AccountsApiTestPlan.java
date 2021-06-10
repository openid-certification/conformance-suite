package net.openid.conformance.openbanking_brasil;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;

@PublishTestPlan(
	testPlanName = "Account api test",
	profile = OBBProfile.OBB_PROFILE,
	displayName = "Functional tests for accounts API",
	summary = "Structural and logical tests for OpenBanking Brasil-conformant Account API",
	testModules = {
		AccountListsTestModule.class
	})
public class AccountsApiTestPlan implements TestPlan {
}
