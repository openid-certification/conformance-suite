package io.fintechlabs.testframework.fapi;

import io.fintechlabs.testframework.openbanking.FAPIRWID2OBEnsureServerHandlesNonMatchingIntentId;
import io.fintechlabs.testframework.openbanking.FAPIRWID2OBEnsureSignedRequestObjectWithRS256FailsWithMTLS;
import io.fintechlabs.testframework.plan.PublishTestPlan;
import io.fintechlabs.testframework.plan.TestPlan;

@PublishTestPlan (
	testPlanName = "fapi-rw-id2-test-plan-v3.0.7",
	displayName = "FAPI-RW-ID2: Authorization server test (V3.0.7; allowable for certification until 24th August)",
	profile = "FAPI-RW-ID2-OpenID-Provider-Authorization-Server-Test",
	testModules = {
		// Normal well behaved client cases
		FAPIRWID2DiscoveryEndpointVerification.class,
		FAPIRWID2.class,
		FAPIRWID2UserRejectsAuthentication.class,
		FAPIRWID2EnsureServerAcceptsRequestObjectWithMultipleAud.class,

		// Possible failure case
		FAPIRWID2EnsureResponseModeQuery.class,
		FAPIRWID2EnsureDifferentNonceInsideAndOutsideRequestObject.class,
		FAPIRWID2EnsureRegisteredRedirectUri.class,

		// Negative tests for request objects
		FAPIRWID2EnsureRequestObjectWithoutExpFails.class,
		FAPIRWID2EnsureRequestObjectWithoutScopeFails.class,
		FAPIRWID2EnsureRequestObjectWithoutState.class,
		FAPIRWID2EnsureRequestObjectWithoutNonceFails.class,
		FAPIRWID2EnsureRequestObjectWithoutRedirectUriFails.class,
		FAPIRWID2EnsureExpiredRequestObjectFails.class,
		FAPIRWID2EnsureRequestObjectWithBadAudFails.class,
		FAPIRWID2EnsureSignedRequestObjectWithRS256Fails.class,
		FAPIRWID2OBEnsureSignedRequestObjectWithRS256FailsWithMTLS.class,
		FAPIRWID2EnsureRequestObjectSignatureAlgorithmIsNotNone.class,
		FAPIRWID2EnsureMatchingKeyInAuthorizationRequest.class,

		// Negative tests for authorization request
		FAPIRWID2EnsureAuthorizationRequestWithoutRequestObjectFails.class,
		FAPIRWID2EnsureRedirectUriInAuthorizationRequest.class,
		FAPIRWID2EnsureResponseTypeCodeFails.class,

		// Negative tests for token endpoint
		FAPIRWID2EnsureClientIdInTokenEndpointWithMTLS.class,
		FAPIRWID2EnsureClientIdInTokenEndpointWithPrivateKeyAndMTLSHolderOfKey.class,
		FAPIRWID2EnsureMTLSHolderOfKeyRequiredWithMTLS.class,
		FAPIRWID2EnsureMTLSHolderOfKeyRequiredWithPrivateKeyAndMTLSHolderOfKey.class,
		FAPIRWID2EnsureAuthorizationCodeIsBoundToClient.class,

		// Private key specific tests
		FAPIRWID2EnsureSignedClientAssertionWithRS256FailsWithPrivateKeyAndMTLSHolderOfKey.class,
		FAPIRWID2EnsureClientAssertionInTokenEndpointWithPrivateKeyAndMTLSHolderOfKey.class,

		// OB systems specific tests
		FAPIRWID2OBEnsureServerHandlesNonMatchingIntentId.class
	},
	variants = {
		FAPIRWID2.variant_mtls,
		FAPIRWID2.variant_privatekeyjwt,
		FAPIRWID2.variant_openbankinguk_mtls,
		FAPIRWID2.variant_openbankinguk_privatekeyjwt
	}
)
public class FAPI_RW_ID2_V3_0_7_TestPlan implements TestPlan {

}
