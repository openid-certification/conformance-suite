package io.fintechlabs.testframework.openbanking;

import io.fintechlabs.testframework.plan.PublishTestPlan;
import io.fintechlabs.testframework.plan.TestPlan;

@PublishTestPlan (
	testPlanName = "fapi-rw-id2-ob-with-mtls-test-plan",
	displayName = "FAPI-RW-ID2-OpenBankingUK: Authorization server test using mtls client authentication",
	profile = "FAPI-RW-ID2-OpenBankingUK-OpenID-Provider-Authorization-Server-Test",
	testModules = {
		// Normal well behaved client cases
		FAPIRWID2OBDiscoveryEndpointVerification.class,
		FAPIRWID2OBWithMTLS.class,
		FAPIRWID2OBUserRejectsAuthenticationWithMTLS.class,
		FAPIRWID2OBEnsureRequestObjectWithMultipleAudSucceedsWithMTLS.class,
		FAPIRWID2OBEnsureAuthorizationRequestWithoutStateSuccessWithMTLS.class,

		// Possible failure case
		FAPIRWID2OBEnsureResponseModeQueryWithMTLS.class,
		FAPIRWID2OBEnsureDifferentNonceInsideAndOutsideRequestObjectWithMTLS.class,
		FAPIRWID2OBEnsureRegisteredRedirectUriWithMTLS.class,

		// Negative tests for request objects
		FAPIRWID2OBEnsureRequestObjectWithoutExpFailsWithMTLS.class,
		FAPIRWID2OBEnsureRequestObjectWithoutScopeFailsWithMTLS.class,
		FAPIRWID2OBEnsureRequestObjectWithoutStateWithMTLS.class,
		FAPIRWID2OBEnsureRequestObjectWithoutNonceFailsWithMTLS.class,
		FAPIRWID2OBEnsureRequestObjectWithoutRedirectUriFailsWithMTLS.class,
		FAPIRWID2OBEnsureExpiredRequestObjectFailsWithMTLS.class,
		FAPIRWID2OBEnsureRequestObjectWithBadAudFailsWithMTLS.class,
		FAPIRWID2OBEnsureSignedRequestObjectWithRS256FailsWithMTLS.class,
		FAPIRWID2OBEnsureRequestObjectSignatureAlgorithmIsNotNoneWithMTLS.class,
		FAPIRWID2OBEnsureMatchingKeyInAuthorizationRequestWithMTLS.class,

		// Negative tests for authorization request
		FAPIRWID2OBEnsureAuthorizationRequestWithoutRequestObjectFailsWithMTLS.class,
		FAPIRWID2OBEnsureRedirectUriInAuthorizationRequestWithMTLS.class,
		FAPIRWID2OBEnsureResponseTypeCodeFailsWithMTLS.class,

		// Negative tests for token endpoint
		FAPIRWID2OBEnsureClientIdInTokenEndpointWithMTLS.class,
		FAPIRWID2OBEnsureMTLSHolderOfKeyRequiredWithMTLS.class,
		FAPIRWID2OBEnsureAuthorizationCodeIsBoundToClientWithMTLS.class,

		// OB systems specific tests
		FAPIRWID2OBEnsureServerHandlesNonMatchingIntentIdWithMTLS.class,
	}
)
public class FAPIRWID2OBWithMTLSTestPlan implements TestPlan {

}
