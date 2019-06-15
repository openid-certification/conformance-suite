package io.fintechlabs.testframework.fapiciba;

import io.fintechlabs.testframework.plan.PublishTestPlan;
import io.fintechlabs.testframework.plan.TestPlan;

@PublishTestPlan (
	testPlanName = "fapi-ciba-test-plan",
	displayName = "FAPI-CIBA: test plan",
	profile = "FAPI-CIBA",
	testModules = {
		FAPICIBADiscoveryEndpointVerification.class,
		FAPICIBA.class,
		FAPICIBAAuthReqIdExpired.class,
		FAPICIBAUserRejectsAuthentication.class,
//		FAPICIBAPollEnsureAuthorizationRequestWithBindingMessageSucceeds.class,
	},
	variants = {
		FAPICIBA.variant_ping_mtls,
		FAPICIBA.variant_ping_privatekeyjwt,
		FAPICIBA.variant_poll_mtls,
		FAPICIBA.variant_poll_privatekeyjwt,
	}
)
public class FAPICIBATestPlan implements TestPlan {

}
