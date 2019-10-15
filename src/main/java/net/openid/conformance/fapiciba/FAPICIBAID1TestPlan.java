package net.openid.conformance.fapiciba;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;

@PublishTestPlan (
	testPlanName = "fapi-ciba-id1-test-plan",
	displayName = "FAPI-CIBA-ID1: test plan",
	profile = "FAPI-CIBA-ID1",
	testModules = {
		// Normal well behaved client cases
		FAPICIBAID1DiscoveryEndpointVerification.class,
		FAPICIBAID1.class,
		FAPICIBAID1UserRejectsAuthentication.class,
		FAPICIBAID1MultipleCallToTokenEndpoint.class,
		FAPICIBAID1AuthReqIdExpired.class,
		FAPICIBAID1EnsureAuthorizationRequestWithBindingMessageSucceeds.class,

		// Possible failure case
		FAPICIBAID1EnsureAuthorizationRequestWithPotentiallyBadBindingMessage.class,

		// Negative tests for request objects
		FAPICIBAID1EnsureRequestObjectMissingAudFails.class,
		FAPICIBAID1EnsureRequestObjectBadAudFails.class,
		FAPICIBAID1EnsureRequestObjectMissingIssFails.class,
		FAPICIBAID1EnsureRequestObjectBadIssFails.class,
		FAPICIBAID1EnsureRequestObjectMissingExpFails.class,
		FAPICIBAID1EnsureRequestObjectExpiredExpFails.class,
		FAPICIBAID1EnsureRequestObjectExpIs70MinutesInFutureFails.class,
		FAPICIBAID1EnsureRequestObjectMissingIatFails.class,
		FAPICIBAID1EnsureRequestObjectMissingNbfFails.class,
		FAPICIBAID1EnsureRequestObjectNbfIs10MinutesInFutureFails.class,
		FAPICIBAID1EnsureRequestObjectNbfIs70MinutesInPastFails.class,
		FAPICIBAID1EnsureRequestObjectMissingJtiFails.class,
		FAPICIBAID1EnsureRequestObjectSignatureAlgorithmIsNoneFails.class,
		FAPICIBAID1EnsureRequestObjectSignatureAlgorithmIsBadFails.class,
		FAPICIBAID1EnsureRequestObjectSignatureAlgorithmIsRS256Fails.class,
		FAPICIBAID1EnsureRequestObjectSignedByOtherClientFails.class,

		// Negative tests for hints
		FAPICIBAID1EnsureAuthorizationRequestWithMultipleHintsFails.class,

		// Negative tests for token endpoint
		FAPICIBAID1EnsureWrongAuthenticationRequestIdInTokenEndpointRequest.class,

		// MTLS specific tests - not possible to test with private_key_jwt
		FAPICIBAID1EnsureDifferentClientIdAndIssuerInBackchannelAuthorizationRequest.class,
		FAPICIBAID1EnsureWrongClientIdInTokenEndpointRequest.class,
		FAPICIBAID1EnsureWrongClientIdInBackchannelAuthorizationRequest.class,

		// private_key_jwt specific tests - not possible to test with mtls
		FAPICIBAID1EnsureWithoutClientAssertionInTokenEndpointFails.class,
		FAPICIBAID1EnsureWithoutClientAssertionInBackchannelAuthorizationRequestFails.class,
		FAPICIBAID1EnsureClientAssertionSignatureAlgorithmInBackchannelAuthorizationRequestIsRS256Fails.class,
		FAPICIBAID1EnsureClientAssertionSignatureAlgorithmInTokenEndpointRequestIsRS256Fails.class,

		// Ping specific tests, not applicable to poll
		FAPICIBAID1PingNotificationEndpointReturnsABody.class,
		FAPICIBAID1PingNotificationEndpointReturns401.class,
		FAPICIBAID1PingNotificationEndpointReturns403.class,
		FAPICIBAID1PingNotificationEndpointReturns401AndRequireServerDoesNotRetry.class,
		FAPICIBAID1PingNotificationEndpointReturnsRedirectRequest.class,

		// FAPI specific
		FAPICIBAID1EnsureBackchannelAuthorizationRequestWithoutRequestFails.class,

		//Refresh token tests
		FAPICIBAID1RefreshToken.class,

		FAPICIBAID1EnsureMTLSHolderOfKeyRequired.class,
	},
	variants = {
		FAPICIBAID1.variant_ping_mtls,
		FAPICIBAID1.variant_ping_privatekeyjwt,
		FAPICIBAID1.variant_poll_mtls,
		FAPICIBAID1.variant_poll_privatekeyjwt,
		FAPICIBAID1.variant_openbankinguk_ping_mtls,
		FAPICIBAID1.variant_openbankinguk_ping_privatekeyjwt,
		FAPICIBAID1.variant_openbankinguk_poll_mtls,
		FAPICIBAID1.variant_openbankinguk_poll_privatekeyjwt,
	}
)
public class FAPICIBAID1TestPlan implements TestPlan {

}
