package net.openid.conformance.cmf.op

import net.openid.conformance.plan.PublishTestPlan
import net.openid.conformance.plan.TestPlan
import net.openid.conformance.variant.VariantSelection

/**
 * The FAPI2 Message Signing authorization server tests, as the Chile Open Finance (CMF) profile fixes them:
 * tls_client_auth, mTLS sender constraining, PAR with signed request objects, RAR, plain responses and grant
 * management. The only parameter left to the tester is whether OpenID Connect is used.
 */
@PublishTestPlan(
	testPlanName = "cmf-chile-fapi2-message-signing-op-test-plan",
	shortName = "Final + Chile CMF",
	certifiable = true,
	alpha = true,
	displayName = ("Chile Open Finance (CMF): FAPI2-Message-Signing-Final - Authorization server test "
		+ "(alpha version - may be incomplete or incorrect, please email certification@oidf.org)"),
	profile = TestPlan.ProfileNames.optest,
	specFamily = TestPlan.SpecFamilyNames.fapi2MessageSigning,
	specVersion = TestPlan.SpecVersionNames.fapi2MsFinal,
	summary = ("Tests an authorization server implementing FAPI2 Message Signing under the Chile Open Finance (CMF) "
		+ "profile. Configure two statically registered clients using tls_client_auth, each with its own mTLS "
		+ "certificate and signing key, and the rich authorization request to send.")
)
class CMFChileOPTestPlan : TestPlan {

	override fun testModulesWithVariants(): List<TestPlan.ModuleListEntry> =
		listOf(TestPlan.ModuleListEntry(testModules, listOf()))

	override fun certificationProfileName(variant: VariantSelection): List<String> = listOf("FAPI2MS OP CL-OF")

	companion object {
		@JvmField
		val testModules = listOf(
			// Normal well behaved client cases
			CMFChileOPDiscoveryEndpointVerification::class.java,
			CMFChileOPHappyFlow::class.java,
			CMFChileOPUserRejectsAuthentication::class.java,
			CMFChileOPEnsureServerAcceptsRequestObjectWithMultipleAud::class.java,
			CMFChileOPEnsureAuthorizationRequestWithoutStateSuccess::class.java,
			CMFChileOPEnsureAuthorizationRequestWithoutNonceSuccess::class.java,
			CMFChileOPEnsureAuthorizationRequestWith64CharNonceSuccess::class.java,
			CMFChileOPEnsureOtherScopeOrderSucceeds::class.java,
			CMFChileOPTestClaimsParameterIdentityClaims::class.java,
			CMFChileOPAccessTokenTypeHeaderCaseSensitivity::class.java,
			CMFChileOPEnsureRequestObjectWithNbf8SecondsInTheFutureIsAccepted::class.java,

			// Possible failure case
			CMFChileOPEnsureDifferentNonceInsideAndOutsideRequestObject::class.java,
			CMFChileOPEnsureDifferentStateInsideAndOutsideRequestObject::class.java,
			CMFChileOPEnsureAuthorizationRequestWithLongNonce::class.java,
			CMFChileOPEnsureAuthorizationRequestWithLongState::class.java,

			// Negative tests for request objects
			CMFChileOPEnsureRequestObjectWithoutExpFails::class.java,
			CMFChileOPEnsureRequestObjectWithoutNbfFails::class.java,
			CMFChileOPStateOnlyOutsideRequestObjectNotUsed::class.java,
			CMFChileOPEnsureRequestObjectWithoutRedirectUriFails::class.java,
			CMFChileOPEnsureExpiredRequestObjectFails::class.java,
			CMFChileOPEnsureRequestObjectWithBadAudFails::class.java,
			CMFChileOPEnsureRequestObjectWithExpOver60Fails::class.java,
			CMFChileOPEnsureRequestObjectWithNbfOver60Fails::class.java,
			CMFChileOPEnsureSignedRequestObjectWithRS256Fails::class.java,
			CMFChileOPEnsureRequestObjectSignatureAlgorithmIsNotNone::class.java,
			CMFChileOPEnsureRequestObjectWithInvalidSignatureFails::class.java,
			CMFChileOPEnsureMatchingKeyInAuthorizationRequest::class.java,
			CMFChileOPEnsureUnsignedRequestAtParEndpointFails::class.java,

			// Negative tests for authorization request
			CMFChileOPEnsureRegisteredRedirectUri::class.java,
			CMFChileOPEnsureUnsignedAuthorizationRequestWithoutUsingParFails::class.java,
			CMFChileOPEnsureRedirectUriInAuthorizationRequest::class.java,
			CMFChileOPEnsureResponseTypeCodeIdTokenFails::class.java,
			CMFChileOPEnsureResponseTypeTokenFails::class.java,

			// Negative tests for token endpoint
			CMFChileOPEnsureClientIdInTokenEndpoint::class.java,
			CMFChileOPEnsureHolderOfKeyRequired::class.java,
			CMFChileOPEnsureAuthorizationCodeIsBoundToClient::class.java,
			CMFChileOPAttemptReuseAuthorizationCodeAfterOneSecond::class.java,
			CMFChileOPAttemptToUseExpiredAuthCode::class.java,

			// Refresh token tests
			CMFChileOPRefreshToken::class.java,

			// PAR tests
			CMFChileOPPAREnsureServerAcceptsReusedRequestUriBeforeAuthenticationCompletion::class.java,
			CMFChileOPPARAttemptReuseRequestUri::class.java,
			CMFChileOPPARAttemptToUseExpiredRequestUri::class.java,
			CMFChileOPPAREnsureRequestUriIsBoundToClient::class.java,
			CMFChileOPPARRejectRequestUriInParAuthorizationFormParams::class.java,
			CMFChileOPPARRejectInvalidHttpVerb::class.java,

			// PKCE tests
			CMFChileOPPAREnsurePKCERequired::class.java,
			CMFChileOPPAREnsurePKCECodeVerifierRequired::class.java,
			CMFChileOPPARIncorrectPKCECodeVerifierRejected::class.java,
			CMFChileOPPAREnsurePlainPKCERejected::class.java,

			CMFChileOPPARRejectRequestUriInParAuthorizationRequest::class.java,

			CMFChileOPParWithoutDuplicateParameters::class.java,

			// Grant Management tests
			CMFChileOPGrantManagementQueryAndRevoke::class.java,
			CMFChileOPGrantManagementMerge::class.java,
			CMFChileOPGrantManagementReplace::class.java,
			CMFChileOPGrantManagementEnsureInvalidGrantIdFails::class.java,
			CMFChileOPGrantManagementEnsureQueryNonExistentGrantFails::class.java,
			CMFChileOPGrantManagementEnsureWrongClientCannotQueryGrant::class.java,
			CMFChileOPGrantManagementEnsureWrongClientCannotRevokeGrant::class.java
		)
	}
}
