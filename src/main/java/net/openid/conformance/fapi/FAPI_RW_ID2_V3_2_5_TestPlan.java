package net.openid.conformance.fapi;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;

@PublishTestPlan (
	testPlanName = "fapi-rw-id2-test-plan-v3.2.5",
	displayName = "FAPI-RW-ID2: Authorization server test (V3.2.5; allowable for certification until 30th November)",
	profile = "FAPI-RW-ID2-OpenID-Provider-Authorization-Server-Test",
	testModules = {
		// Normal well behaved client cases
		FAPIRWID2DiscoveryEndpointVerification.class,
		FAPIRWID2.class,
		FAPIRWID2UserRejectsAuthentication.class,
		FAPIRWID2EnsureServerAcceptsRequestObjectWithMultipleAud.class,
		FAPIRWID2EnsureAuthorizationRequestWithoutStateSuccess.class,

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
		FAPIRWID2EnsureRequestObjectSignatureAlgorithmIsNotNone.class,
		FAPIRWID2EnsureRequestObjectWithInvalidSignatureFails.class,
		FAPIRWID2EnsureMatchingKeyInAuthorizationRequest.class,

		// Negative tests for authorization request
		FAPIRWID2EnsureAuthorizationRequestWithoutRequestObjectFails.class,
		FAPIRWID2EnsureRedirectUriInAuthorizationRequest.class,
		FAPIRWID2EnsureResponseTypeCodeFails.class,

		// Negative tests for token endpoint
		FAPIRWID2EnsureClientIdInTokenEndpoint.class,
		FAPIRWID2EnsureMTLSHolderOfKeyRequired.class,
		FAPIRWID2EnsureAuthorizationCodeIsBoundToClient.class,

		// Private key specific tests
		FAPIRWID2EnsureSignedClientAssertionWithRS256Fails.class,
		FAPIRWID2EnsureClientAssertionInTokenEndpoint.class,

		//Refresh token tests
		FAPIRWID2RefreshToken.class,

		// OB systems specific tests
		FAPIRWID2EnsureServerHandlesNonMatchingIntentId.class,

		FAPIRWID2TestEssentialAcrScaClaim.class
	},
	variants = {
		FAPIRWID2.variant_mtls,
		FAPIRWID2.variant_privatekeyjwt,
		FAPIRWID2.variant_mtls_jarm,
		FAPIRWID2.variant_privatekeyjwt_jarm,
		FAPIRWID2.variant_openbankinguk_mtls,
		FAPIRWID2.variant_openbankinguk_privatekeyjwt
	}
)
public class FAPI_RW_ID2_V3_2_5_TestPlan implements TestPlan {

}
