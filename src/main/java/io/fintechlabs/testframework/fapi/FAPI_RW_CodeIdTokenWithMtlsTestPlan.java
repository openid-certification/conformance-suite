package io.fintechlabs.testframework.fapi;

import io.fintechlabs.testframework.plan.PublishTestPlan;
import io.fintechlabs.testframework.plan.TestPlan;

/**
 * @author jheenan
 *
 */
@PublishTestPlan (
	testPlanName = "fapi-rw-code-id-token-with-mtls-test-plan",
	displayName = "FAPI-RW: code id_token with mtls Test Plan",
	profile = "FAPI-RW",
	testModuleNames = {
		"fapi-rw-code-id-token-with-mtls",
		"fapi-rw-ensure-request-object-signature-algorithm-is-not-null",
		"fapi-rw-reject-code-flow-test",
		"fapi-rw-ensure-client-id-in-token-endpoint-with-mtls",
		"fapi-rw-ensure-request-object-without-exp-fails-with-mtls",
		"fapi-rw-ensure-request-object-without-scope-fails-with-mtls",
		"fapi-rw-ensure-request-object-without-state-fails-with-mtls",
		"fapi-rw-ensure-request-object-without-nonce-fails-with-mtls",
		"fapi-rw-ensure-request-object-without-redirect-uri-fails-with-mtls",
		"fapi-rw-ensure-request-object-with-multiple-aud-succeeds-with-mtls",
		"fapi-rw-ensure-expired-request-object-fails-with-mtls",
	}
)
public class FAPI_RW_CodeIdTokenWithMtlsTestPlan implements TestPlan {

}
