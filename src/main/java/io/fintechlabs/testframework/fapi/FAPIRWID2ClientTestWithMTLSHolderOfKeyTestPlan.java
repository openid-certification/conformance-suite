package io.fintechlabs.testframework.fapi;

import io.fintechlabs.testframework.plan.PublishTestPlan;
import io.fintechlabs.testframework.plan.TestPlan;

@PublishTestPlan (
	testPlanName = "fapi-rw-id2-client-test-with-mtls-holder-of-key-test-plan",
	displayName = "FAPI-RW-ID2: client test (with mtls holder of key) Test Plan",
	profile = "FAPI-RW-ID2",
	testModuleNames = {
		"fapi-rw-id2-client-test-with-mtls-holder-of-key",
		"fapi-rw-id2-client-test-with-mtls-holder-of-key-invalid-shash",
		"fapi-rw-id2-client-test-with-mtls-holder-of-key-invalid-chash",
		"fapi-rw-id2-client-test-with-mtls-holder-of-key-invalid-nonce",
		"fapi-rw-id2-client-test-with-mtls-holder-of-key-invalid-iss",
		"fapi-rw-id2-client-test-with-mtls-holder-of-key-invalid-aud",
		"fapi-rw-id2-client-test-with-mtls-holder-of-key-invalid-secondary-aud",
		"fapi-rw-id2-client-test-with-mtls-holder-of-key-invalid-signature",
		"fapi-rw-id2-client-test-with-mtls-holder-of-key-invalid-null-alg",
		"fapi-rw-id2-client-test-with-mtls-holder-of-key-invalid-alternate-alg"
	}
)
public class FAPIRWID2ClientTestWithMTLSHolderOfKeyTestPlan implements TestPlan {

}
