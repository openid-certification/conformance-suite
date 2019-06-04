package io.fintechlabs.testframework.fapiciba;

import io.fintechlabs.testframework.plan.PublishTestPlan;
import io.fintechlabs.testframework.plan.TestPlan;

@PublishTestPlan (
	testPlanName = "fapi-ciba-poll-test-plan",
	displayName = "FAPI-CIBA: poll test plan",
	profile = "FAPI-CIBA",
	testModules = {
		FAPICIBAPoll.class,
		FAPICIBAPollEnsureAuthorizationRequestWithBindingMessageSucceeds.class,
	}
)
public class FAPICIBAPollTestPlan implements TestPlan {

}
