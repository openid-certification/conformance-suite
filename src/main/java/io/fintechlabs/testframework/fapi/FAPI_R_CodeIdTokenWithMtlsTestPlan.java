package io.fintechlabs.testframework.fapi;

import io.fintechlabs.testframework.plan.PublishTestPlan;
import io.fintechlabs.testframework.plan.TestPlan;

/**
 * @author jheenan
 *
 */
@PublishTestPlan (
	testPlanName = "fapi-r-code-id-token-with-mtls-test-plan",
	displayName = "FAPI-R: code id_token with mtls Test Plan",
	profile = "FAPI-R",
	testModuleNames = {
		"fapi-r-code-id-token-with-mtls",
		"fapi-r-ensure-redirect-uri-in-authorization-request",
		"fapi-r-ensure-redirect-uri-is-registered",
		"fapi-r-require-pkce",
		"fapi-r-reject-plain-pkce"
	}
)
public class FAPI_R_CodeIdTokenWithMtlsTestPlan implements TestPlan {

}
