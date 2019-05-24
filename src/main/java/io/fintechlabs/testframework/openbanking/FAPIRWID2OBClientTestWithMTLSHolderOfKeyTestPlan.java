package io.fintechlabs.testframework.openbanking;

import io.fintechlabs.testframework.plan.PublishTestPlan;
import io.fintechlabs.testframework.plan.TestPlan;

@PublishTestPlan (
	testPlanName = "fapi-rw-id2-ob-client-test-with-mtls-holder-of-key-test-plan",
	displayName = "FAPI-RW-ID2-OB: client test (with MTLS) Test Plan",
	profile = "FAPI-RW-ID2-OB",
	testModuleNames = {
		"fapi-rw-id2-ob-client-test-with-mtls-holder-of-key",
		"fapi-rw-id2-ob-client-test-with-mtls-holder-of-key-missing-athash",
		"fapi-rw-id2-ob-client-test-with-mtls-holder-of-key-invalid-shash",
		"fapi-rw-id2-ob-client-test-with-mtls-holder-of-key-invalid-chash",
		"fapi-rw-id2-ob-client-test-with-mtls-holder-of-key-invalid-nonce",
		"fapi-rw-id2-ob-client-test-with-mtls-holder-of-key-invalid-iss",
		"fapi-rw-id2-ob-client-test-with-mtls-holder-of-key-invalid-aud",
		"fapi-rw-id2-ob-client-test-with-mtls-holder-of-key-invalid-secondary-aud",
		"fapi-rw-id2-ob-client-test-with-mtls-holder-of-key-invalid-openbanking-intent-id",
		"fapi-rw-id2-ob-client-test-with-mtls-holder-of-key-invalid-signature",
		"fapi-rw-id2-ob-client-test-with-mtls-holder-of-key-invalid-null-alg",
		"fapi-rw-id2-ob-client-test-with-mtls-holder-of-key-invalid-alternate-alg",
		"fapi-rw-id2-ob-client-test-with-mtls-holder-of-key-invalid-missing-exp",
		"fapi-rw-id2-ob-client-test-with-mtls-holder-of-key-invalid-expired-exp",
		"fapi-rw-id2-ob-client-test-with-mtls-holder-of-key-iat-is-week-in-past",
		"fapi-rw-id2-ob-client-test-with-mtls-holder-of-key-invalid-missing-aud",
		"fapi-rw-id2-ob-client-test-with-mtls-holder-of-key-invalid-missing-iss",
		"fapi-rw-id2-ob-client-test-with-mtls-holder-of-key-invalid-missing-nonce",
		"fapi-rw-id2-ob-client-test-with-mtls-holder-of-key-invalid-missing-shash",
		"fapi-rw-id2-ob-client-test-with-mtls-holder-of-key-valid-aud-as-array"
	}
)
public class FAPIRWID2OBClientTestWithMTLSHolderOfKeyTestPlan implements TestPlan {

}
