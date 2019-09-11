package io.fintechlabs.testframework.fapiciba;

import io.fintechlabs.testframework.plan.PublishTestPlan;
import io.fintechlabs.testframework.plan.TestPlan;

@PublishTestPlan (
	testPlanName = "fapi-ciba-test-plan",
	displayName = "FAPI-CIBA: test plan",
	profile = "FAPI-CIBA",
	testModules = {
		// Normal well behaved client cases
		FAPICIBADiscoveryEndpointVerification.class,
		FAPICIBA.class,
		FAPICIBAUserRejectsAuthentication.class,
		FAPICIBAMultipleCallToTokenEndpoint.class,
		FAPICIBAAuthReqIdExpired.class,
		FAPICIBAEnsureAuthorizationRequestWithBindingMessageSucceeds.class,

		// Possible failure case
		FAPICIBAEnsureAuthorizationRequestWithPotentiallyBadBindingMessage.class,

		// Negative tests for request objects
		FAPICIBAEnsureRequestObjectMissingAudFails.class,
		FAPICIBAEnsureRequestObjectBadAudFails.class,
		FAPICIBAEnsureRequestObjectMissingIssFails.class,
		FAPICIBAEnsureRequestObjectBadIssFails.class,
		FAPICIBAEnsureRequestObjectMissingExpFails.class,
		FAPICIBAEnsureRequestObjectExpiredExpFails.class,
		FAPICIBAEnsureRequestObjectExpIs70MinutesInFutureFails.class,
		FAPICIBAEnsureRequestObjectMissingIatFails.class,
		FAPICIBAEnsureRequestObjectMissingNbfFails.class,
		FAPICIBAEnsureRequestObjectNbfIs10MinutesInFutureFails.class,
		FAPICIBAEnsureRequestObjectNbfIs70MinutesInPastFails.class,
		FAPICIBAEnsureRequestObjectMissingJtiFails.class,
		FAPICIBAEnsureRequestObjectSignatureAlgorithmIsNoneFails.class,
		FAPICIBAEnsureRequestObjectSignatureAlgorithmIsBadFails.class,
		FAPICIBAEnsureRequestObjectSignatureAlgorithmIsRS256Fails.class,
		FAPICIBAEnsureRequestObjectSignedByOtherClientFails.class,

		// Negative tests for hints
		FAPICIBAEnsureAuthorizationRequestWithMultipleHintsFails.class,

		// Negative tests for token endpoint
		FAPICIBAEnsureWrongAuthenticationRequestIdInTokenEndpointRequest.class,

		// MTLS specific tests - not possible to test with private_key_jwt
		FAPICIBAEnsureDifferentClientIdAndIssuerInBackchannelAuthorizationRequest.class,
		FAPICIBAEnsureWrongClientIdInTokenEndpointRequest.class,
		FAPICIBAEnsureWrongClientIdInBackchannelAuthorizationRequest.class,

		// private_key_jwt specific tests - not possible to test with mtls
		FAPICIBAEnsureWithoutClientAssertionInTokenEndpointFails.class,
		FAPICIBAEnsureWithoutClientAssertionInBackchannelAuthorizationRequestFails.class,
		FAPICIBAEnsureClientAssertionSignatureAlgorithmInBackchannelAuthorizationRequestIsRS256Fails.class,
		FAPICIBAEnsureClientAssertionSignatureAlgorithmInTokenEndpointRequestIsRS256Fails.class,

		// Ping specific tests, not applicable to poll
		FAPICIBAPingNotificationEndpointReturnsABody.class,
		FAPICIBAPingNotificationEndpointReturns401.class,
		FAPICIBAPingNotificationEndpointReturns403.class,
		FAPICIBAPingNotificationEndpointReturns401AndRequireServerDoesNotRetry.class,
		FAPICIBAPingNotificationEndpointReturnsRedirectRequest.class,

		// FAPI specific
		FAPICIBAEnsureBackchannelAuthorizationRequestWithoutRequestFails.class,

		//Refresh token tests
		FAPICIBARefreshToken.class,

		FAPICIBAEnsureMTLSHolderOfKeyRequired.class,
	},
	variants = {
		FAPICIBA.variant_ping_mtls,
		FAPICIBA.variant_ping_privatekeyjwt,
		FAPICIBA.variant_poll_mtls,
		FAPICIBA.variant_poll_privatekeyjwt,
		FAPICIBA.variant_openbankinguk_ping_mtls,
		FAPICIBA.variant_openbankinguk_ping_privatekeyjwt,
		FAPICIBA.variant_openbankinguk_poll_mtls,
		FAPICIBA.variant_openbankinguk_poll_privatekeyjwt,
	}
)
public class FAPICIBATestPlan implements TestPlan {

}
