package io.fintechlabs.testframework.fapi;

import io.fintechlabs.testframework.plan.PublishTestPlan;
import io.fintechlabs.testframework.plan.TestPlan;

@PublishTestPlan (
	testPlanName = "fapi-rw-id2-client-test-with-private-key-jwt-and-mtls-holder-of-key-test-plan",
	displayName = "FAPI-RW-ID2: client test (with private_key_jwt and mtls holder of key) Test Plan",
	profile = "FAPI-RW-ID2",
	testModuleNames = {
		"fapi-rw-id2-client-test-with-private-key-jwt-and-mtls-holder-of-key",
		"fapi-rw-id2-client-test-with-private-key-jwt-and-mtls-holder-of-key-invalid-shash",
		"fapi-rw-id2-client-test-with-private-key-jwt-and-mtls-holder-of-key-invalid-chash",
		"fapi-rw-id2-client-test-with-private-key-jwt-and-mtls-holder-of-key-invalid-nonce",
		"fapi-rw-id2-client-test-with-private-key-jwt-and-mtls-holder-of-key-invalid-iss",
		"fapi-rw-id2-client-test-with-private-key-jwt-and-mtls-holder-of-key-invalid-aud",
		"fapi-rw-id2-client-test-with-private-key-jwt-and-mtls-holder-of-key-invalid-secondary-aud",
		"fapi-rw-id2-client-test-with-private-key-jwt-and-mtls-holder-of-key-invalid-signature",
		"fapi-rw-id2-client-test-with-private-key-jwt-and-mtls-holder-of-key-invalid-null-alg",
		"fapi-rw-id2-client-test-with-private-key-jwt-and-mtls-holder-of-key-invalid-alternate-alg",
		"fapi-rw-id2-client-test-with-private-key-jwt-and-mtls-holder-of-key-invalid-missing-exp",
		"fapi-rw-id2-client-test-with-private-key-jwt-and-mtls-holder-of-key-invalid-expired-exp",
		"fapi-rw-id2-client-test-with-private-key-jwt-and-mtls-holder-of-key-invalid-expired-iat",
		"fapi-rw-id2-client-test-with-private-key-jwt-and-mtls-holder-of-key-invalid-missing-aud",
		"fapi-rw-id2-client-test-with-private-key-jwt-and-mtls-holder-of-key-invalid-missing-iss",
		"fapi-rw-id2-client-test-with-private-key-jwt-and-mtls-holder-of-key-invalid-missing-nonce",
		"fapi-rw-id2-client-test-with-private-key-jwt-and-mtls-holder-of-key-invalid-missing-shash",
		"fapi-rw-id2-client-test-with-private-key-jwt-and-mtls-holder-of-key-valid-aud-as-array"
	}
)
public class FAPIRWID2ClientTestWithPrivateKeyJWTAndMTLSHolderOfKeyTestPlan implements TestPlan {

}
