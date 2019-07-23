package io.fintechlabs.testframework.openbanking;

import io.fintechlabs.testframework.fapi.FAPIRWID2;
import io.fintechlabs.testframework.plan.PublishTestPlan;
import io.fintechlabs.testframework.plan.TestPlan;

@PublishTestPlan (
	testPlanName = "fapi-rw-id2-ob-with-private-key-and-mtls-holder-of-key-test-plan",
	displayName = "FAPI-RW-ID2-OpenBankingUK: Authorization server test using private_key_jwt client authentication",
	profile = "FAPI-RW-ID2-OpenBankingUK-OpenID-Provider-Authorization-Server-Test",
	testModules = {
		// Normal well behaved client cases
		FAPIRWID2OBDiscoveryEndpointVerification.class,
		FAPIRWID2OBWithPrivateKeyAndMTLSHolderOfKey.class,
		FAPIRWID2OBUserRejectsAuthenticationWithPrivateKeyAndMTLSHolderOfKey.class,
		FAPIRWID2OBEnsureRequestObjectWithMultipleAudSucceedsWithPrivateKeyAndMTLSHolderOfKey.class,
		FAPIRWID2OBEnsureAuthorizationRequestWithoutStateSuccessWithPrivateKeyAndMTLSHolderOfKey.class,

		// Possible failure case
		FAPIRWID2OBEnsureResponseModeQueryWithPrivateKeyAndMTLSHolderOfKey.class,
		FAPIRWID2OBEnsureDifferentNonceInsideAndOutsideRequestObjectWithPrivateKeyAndMTLSHolderOfKey.class,
		FAPIRWID2OBEnsureRegisteredRedirectUriWithPrivateKeyAndMTLSHolderOfKey.class,

		// Negative tests for request objects
		FAPIRWID2OBEnsureRequestObjectWithoutExpFailsWithPrivateKeyAndMTLSHolderOfKey.class,
		FAPIRWID2OBEnsureRequestObjectWithoutScopeFailsWithPrivateKeyAndMTLSHolderOfKey.class,
		FAPIRWID2OBEnsureRequestObjectWithoutStateWithPrivateKeyAndMTLSHolderOfKey.class,
		FAPIRWID2OBEnsureRequestObjectWithoutNonceFailsWithPrivateKeyAndMTLSHolderOfKey.class,
		FAPIRWID2OBEnsureRequestObjectWithoutRedirectUriFailsWithPrivateKeyAndMTLSHolderOfKey.class,
		FAPIRWID2OBEnsureExpiredRequestObjectFailsWithPrivateKeyAndMTLSHolderOfKey.class,
		FAPIRWID2OBEnsureRequestObjectWithBadAudFailsWithPrivateKeyAndMTLSHolderOfKey.class,
		FAPIRWID2OBEnsureSignedRequestObjectWithRS256FailsWithPrivateKeyAndMTLSHolderOfKey.class,
		FAPIRWID2OBEnsureRequestObjectSignatureAlgorithmIsNotNoneWithPrivateKeyAndMTLS.class,
		FAPIRWID2OBEnsureMatchingKeyInAuthorizationRequestWithPrivateKeyAndMTLSHolderOfKey.class,

		// Negative tests for authorization request
		FAPIRWID2OBEnsureAuthorizationRequestWithoutRequestObjectFailsWithPrivateKeyAndMTLSHolderOfKey.class,
		FAPIRWID2OBEnsureRedirectUriInAuthorizationRequestWithPrivateKeyAndMTLSHolderOfKey.class,
		FAPIRWID2OBEnsureResponseTypeCodeFailsWithPrivateKeyAndMTLSHolderOfKey.class,

		// Negative tests for token endpoint
		FAPIRWID2OBEnsureClientIdInTokenEndpointWithPrivateKeyAndMTLSHolderOfKey.class,
		FAPIRWID2OBEnsureMTLSHolderOfKeyRequiredWithPrivateKeyAndMTLSHolderOfKey.class,
		FAPIRWID2OBEnsureAuthorizationCodeIsBoundToClientIsBoundToClientWithPrivateKeyAndMTLSHolderOfKey.class,

		// Private key specific tests
		FAPIRWID2OBEnsureSignedClientAssertionWithRS256FailsWithPrivateKeyAndMTLSHolderOfKey.class,
		FAPIRWID2OBEnsureClientAssertionInTokenEndpointWithPrivateKeyAndMTLSHolderOfKey.class,

		//Refresh token tests
		FAPIRWID2OBRefreshTokenWithPrivateKeyAndMTLSHolderOfKey.class,

		// OB systems specific tests
		FAPIRWID2OBEnsureServerHandlesNonMatchingIntentIdWithPrivateKeyAndMTLSHolderOfKey.class,
	},
	variants = {
		FAPIRWID2.variant_openbankinguk_privatekeyjwt
	}
)
public class FAPIRWID2OBWithPrivateKeyAndMTLSHolderOfKeyTestPlan implements TestPlan {

}
