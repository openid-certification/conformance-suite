package io.fintechlabs.testframework.fapiciba;

import io.fintechlabs.testframework.plan.PublishTestPlan;
import io.fintechlabs.testframework.plan.TestPlan;

@PublishTestPlan (
	testPlanName = "fapi-ciba-poll-with-mtls-test-plan",
	displayName = "FAPI-CIBA: poll with mtls client authentication test plan",
	profile = "FAPI-CIBA",
	testModules = {
		// Normal well behaved client cases
		FAPICIBAPollDiscoveryEndpointVerification.class,
		FAPICIBA.class,
		FAPICIBAUserRejectsAuthentication.class,
		FAPICIBAPollMultipleCallToTokenEndpointWithMTLS.class,
		FAPICIBAAuthReqIdExpired.class,
		FAPICIBAPollEnsureAuthorizationRequestWithBindingMessageSucceeds.class,

		// Possible failure case
		FAPICIBAPollEnsureAuthorizationRequestWithPotentiallyBadBindingMessageWithMTLS.class,

		// Negative tests for request objects
		FAPICIBAPollWithMTLSEnsureRequestObjectMissingAudFails.class,
		FAPICIBAPollWithMTLSEnsureRequestObjectBadAudFails.class,
		FAPICIBAPollWithMTLSEnsureRequestObjectMissingIssFails.class,
		FAPICIBAPollWithMTLSEnsureRequestObjectBadIssFails.class,
		FAPICIBAPollWithMTLSEnsureRequestObjectMissingExpFails.class,
		FAPICIBAPollWithMTLSEnsureRequestObjectExpiredExpFails.class,
		FAPICIBAPollWithMTLSEnsureRequestObjectExpIsYearInFutureFails.class,
		FAPICIBAPollWithMTLSEnsureRequestObjectMissingIatFails.class,
		FAPICIBAPollWithMTLSEnsureRequestObjectIatIsWeekInPastFails.class,
		FAPICIBAPollWithMTLSEnsureRequestObjectIatIsHourInFutureFails.class,
		FAPICIBAPollWithMTLSEnsureRequestObjectMissingJtiFails.class,
		FAPICIBAPollEnsureRequestObjectSignatureAlgorithmIsNoneFailsWithMTLS.class,
		FAPICIBAPollEnsureRequestObjectSignatureAlgorithmIsBadFailsWithMTLS.class,
		FAPICIBAPollEnsureRequestObjectSignatureAlgorithmIsRS256FailsWithMTLS.class,
		FAPICIBAPollEnsureRequestObjectSignedByOtherClientFailsWithMTLS.class,

		// Negative tests for hints
		FAPICIBAPollEnsureAuthorizationRequestWithMultipleHintsFailsWithMTLS.class,

		// Negative tests for token endpoint
		FAPICIBAPollEnsureWrongAuthenticationRequestIdInTokenEndpointRequestWithMTLS.class,

		// MTLS specific tests - not possible to test with private_key_jwt
		FAPICIBAPollEnsureDifferentClientIdAndIssuerInBackchannelAuthorizationRequestWithMTLS.class,
		FAPICIBAPollEnsureWrongClientIdInTokenEndpointRequestWithMTLS.class,
		FAPICIBAPollEnsureWrongClientIdInBackchannelAuthorizationRequestWithMTLS.class,

		// FAPI specific
		FAPICIBAPollEnsureBackchannelAuthorizationRequestWithoutRequestFailsWithMTLS.class,
	}
)
public class FAPICIBAPollWithMTLSTestPlan implements TestPlan {

}
