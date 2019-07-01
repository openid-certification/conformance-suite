package io.fintechlabs.testframework.fapi;

import io.fintechlabs.testframework.plan.PublishTestPlan;
import io.fintechlabs.testframework.plan.TestPlan;

@PublishTestPlan (
	testPlanName = "fapi-rw-id2-with-mtls-test-plan",
	displayName = "FAPI-RW-ID2: Authorization server test using mtls client authentication",
	profile = "FAPI-RW-ID2-OpenID-Provider-Authorization-Server-Test",
	testModules = {
		// Normal well behaved client cases
		FAPIRWID2DiscoveryEndpointVerification.class,
		FAPIRWID2WithMTLS.class,
		FAPIRWID2UserRejectsAuthenticationWithMTLS.class,
		FAPIRWID2EnsureServerAcceptsRequestObjectWithMultipleAudWithMTLS.class,
		FAPIRWID2EnsureAuthorizationRequestWithoutStateSuccessWithMTLS.class,

		// Possible failure case
		FAPIRWID2EnsureResponseModeQueryWithMTLS.class,
		FAPIRWID2EnsureDifferentNonceInsideAndOutsideRequestObjectWithMTLS.class,
		FAPIRWID2EnsureRegisteredRedirectUriWithMTLS.class,

		// Negative tests for request objects
		FAPIRWID2EnsureRequestObjectWithoutExpFailsWithMTLS.class,
		FAPIRWID2EnsureRequestObjectWithoutScopeFailsWithMTLS.class,
		FAPIRWID2EnsureRequestObjectWithoutStateWithMTLS.class,
		FAPIRWID2EnsureRequestObjectWithoutNonceFailsWithMTLS.class,
		FAPIRWID2EnsureRequestObjectWithoutRedirectUriFailsWithMTLS.class,
		FAPIRWID2EnsureExpiredRequestObjectFailsWithMTLS.class,
		FAPIRWID2EnsureRequestObjectWithBadAudFailsWithMTLS.class,
		FAPIRWID2EnsureSignedRequestObjectWithRS256FailsWithMTLS.class,
		FAPIRWID2EnsureRequestObjectSignatureAlgorithmIsNotNoneWithMTLS.class,
		FAPIRWID2EnsureMatchingKeyInAuthorizationRequestWithMTLS.class,

		// Negative tests for authorization request
		FAPIRWID2EnsureAuthorizationRequestWithoutRequestObjectFailsWithMTLS.class,
		FAPIRWID2EnsureRedirectUriInAuthorizationRequestWithMTLS.class,
		FAPIRWID2EnsureResponseTypeCodeFailsWithMTLS.class,

		// Negative tests for token endpoint
		FAPIRWID2EnsureClientIdInTokenEndpointWithMTLS.class,
		FAPIRWID2EnsureMTLSHolderOfKeyRequiredWithMTLS.class,
		FAPIRWID2EnsureAuthorizationCodeIsBoundToClientWithMTLS.class,
	}
)
public class FAPI_RW_ID2_WithMTLSTestPlan implements TestPlan {

}
