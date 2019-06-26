package io.fintechlabs.testframework.fapi;

import io.fintechlabs.testframework.plan.PublishTestPlan;
import io.fintechlabs.testframework.plan.TestPlan;

@PublishTestPlan (
	testPlanName = "fapi-r-code-id-token-with-client-secret-jwt-test-plan",
	displayName = "FAPI-R: code id_token with client secret JWT assertion Test Plan",
	profile = "FAPI-R",
	testModules = {
		CodeIdTokenWithClientSecretJWTAssertion.class,
		EnsureRedirectUriInAuthorizationRequest.class,
		EnsureRegisteredRedirectUri.class,
		RequirePKCE.class,
		RejectPlainPKCE.class
	}
)
public class FAPI_R_CodeIdTokenWithClientSecretJWTAssertionTestPlan implements TestPlan {

}
