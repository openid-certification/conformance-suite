package io.fintechlabs.testframework.fapiciba;

import io.fintechlabs.testframework.plan.PublishTestPlan;
import io.fintechlabs.testframework.plan.TestPlan;

@PublishTestPlan (
	testPlanName = "fapi-ciba-poll-with-private-key-jwt-and-mtls-test-plan",
	displayName = "FAPI-CIBA: poll with private_key_jwt and MTLS client authentication test plan",
	profile = "FAPI-CIBA",
	testModuleNames = {
		"fapi-ciba-poll-with-private-key-jwt-and-mtls",
	}
)
public class FAPICIBAPollWithPrivateKeyJWTAndMTLSTestPlan implements TestPlan {

}
