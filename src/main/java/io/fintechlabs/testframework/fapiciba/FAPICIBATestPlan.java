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

		// Ping specific tests, not applicable to poll
		FAPICIBAPingNotificationEndpointReturnsABody.class,
		FAPICIBAPingNotificationEndpointReturns401.class,
		FAPICIBAPingNotificationEndpointReturns403.class,

		// FAPI specific
		FAPICIBAPollEnsureBackchannelAuthorizationRequestWithoutRequestFailsWithMTLS.class,

	},
	variants = {
		FAPICIBA.variant_ping_mtls,
		FAPICIBA.variant_ping_privatekeyjwt,
		FAPICIBA.variant_poll_mtls,
		FAPICIBA.variant_poll_privatekeyjwt,
	}
)
public class FAPICIBATestPlan implements TestPlan {

}
