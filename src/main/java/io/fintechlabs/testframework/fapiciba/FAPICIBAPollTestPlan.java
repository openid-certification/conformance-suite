package io.fintechlabs.testframework.fapiciba;

import io.fintechlabs.testframework.plan.PublishTestPlan;
import io.fintechlabs.testframework.plan.TestPlan;

@PublishTestPlan (
	testPlanName = "fapi-ciba-poll-test-plan",
	displayName = "FAPI-CIBA: poll test plan",
	profile = "FAPI-CIBA",
	testModules = {
		FAPICIBAPollDiscoveryEndpointVerification.class,
		FAPICIBA.class,
		FAPICIBAAuthReqIdExpired.class,
		FAPICIBAPollEnsureAuthorizationRequestWithBindingMessageSucceeds.class,
	},
	variants = { "mtls", "private-key-jwt-and-mtls-holder-of-key" }
)
public class FAPICIBAPollTestPlan implements TestPlan {

}
