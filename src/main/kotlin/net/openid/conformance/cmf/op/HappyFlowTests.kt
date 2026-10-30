package net.openid.conformance.cmf.op

import net.openid.conformance.condition.Condition.ConditionResult
import net.openid.conformance.condition.client.*
import net.openid.conformance.sequence.Steps
import net.openid.conformance.sequence.Steps.condition
import net.openid.conformance.sequence.Steps.continueOnFailure
import net.openid.conformance.sequence.Steps.stopOnFailure
import net.openid.conformance.testmodule.PublishTestModule
import net.openid.conformance.testmodule.TestExecutionUnit

const val CMF_OP_PROFILE = "CMF-Chile-OP"

@PublishTestModule(
	testName = "cmf-chile-op-happy-flow",
	displayName = "Chile CMF OP: happy flow",
	summary = "Tests primarily 'happy' flows, using two different OAuth2 clients (and hence authenticating the user twice), and uses different variations on request objects, registered redirect uri (both redirect uris must be pre-registered as shown in the instructions). It also tests that sender constrained access tokens (required by the FAPI spec) are correctly implemented.",
	profile = CMF_OP_PROFILE
)
class CMFChileOPHappyFlow : AbstractCMFChileOPMultipleClientTestModule() {

	override fun extraConfiguration(): TestExecutionUnit =
		continueOnFailure().onFail(ConditionResult.WARNING).requirements("OIDCD-3", "RFC8414-2")
			.condition(CheckDiscEndpointScopesSupportedContainsRequestedScopes::class.java)
			.skipIfElementMissing("server", "scopes_supported").onSkip(ConditionResult.INFO)

	override fun legs(): List<AuthorizationLeg> {
		val first = flow(OPClient.FIRST)
		val second = flow(OPClient.SECOND)

		return listOf(
			AuthorizationLeg(
				beforeRedirect = first.beforeRedirect(),
				afterCallback = first.afterCallback(
					postAuthorization = Steps.of(
						"Post authorization",
						first.callTokenEndpoint,
						first.verifyTokenEndpointResponse,
						resourceEndpointTlsSteps(),
						first.callResourceEndpoint,
						additionalResourceEndpointSteps(),
						if (openid) {
							continueOnFailure().onFail(ConditionResult.WARNING)
								.condition(EnsureIdTokenDoesNotContainNonRequestedClaims::class.java)
						} else Steps.NONE
					)
				)
			),
			AuthorizationLeg(
				beforeRedirect = Steps.of(
					"Second client authorization request",
					secondClientSetupSteps(),
					Steps.modify(second.beforeRedirect()).apply {
						if (openid) {
							insertAfter(SET_REQUESTED_NONCE_LENGTH, putInteger("Request 43 character nonce", "requested_nonce_length", 43))
						}
						replace(
							BuildRequestObjectByReferenceRedirectToAuthorizationEndpoint::class.java,
							stopOnFailure().requirements("PAR-4")
								.condition(BuildRequestObjectByReferenceRedirectToAuthorizationEndpointReorderedParams::class.java)
						)
					}.build()
				),
				afterCallback = second.afterCallback()
			)
		)
	}

	override fun afterLastLeg(): TestExecutionUnit = client2TokenWithClient1CertificateSteps()

	private fun resourceEndpointTlsSteps(): TestExecutionUnit =
		Steps.of(
			"Resource endpoint TLS",
			condition(ExtractTLSTestValuesFromResourceConfiguration::class.java),
			tlsEndpointSteps("Resource endpoint TLS test", "resource_endpoint_tls", warnOnInsecureCiphers = true)
		)

	/** The access token must not be accepted in the query, and the server must accept other valid accept headers. */
	private fun additionalResourceEndpointSteps(): TestExecutionUnit =
		Steps.of(
			"Additional resource endpoint tests",
			continueOnFailure().requirements("FAPI2-SP-FINAL-5.3.4-2").condition(DisallowAccessTokenInQuery::class.java),
			stopOnFailure().requirements("FAPI1-BASE-6.2.2-4").condition(AddIpV6FapiCustomerIpAddressToResourceEndpointRequest::class.java),
			condition(SetUtf8JsonAcceptHeadersForResourceEndpointRequest::class.java),
			callResourceExpectingSuccess(),
			condition(SetPermissiveAcceptHeaderForResourceEndpointRequest::class.java),
			callResourceExpectingSuccess(),
			condition(ClearAcceptHeaderForResourceEndpointRequest::class.java)
		)

	private fun callResourceExpectingSuccess(): TestExecutionUnit =
		Steps.of(
			null,
			stopOnFailure().requirements("RFC7231-5.3.2").condition(CallProtectedResource::class.java),
			onEndpointResponse("resource_endpoint_response_full", continueOnFailure().condition(EnsureHttpStatusCodeIs200or201::class.java))
		)
}

@PublishTestModule(
	testName = "cmf-chile-op-user-rejects-authentication",
	displayName = "Chile CMF OP: user rejects authentication",
	summary = "This test requires the user to reject the authentication or consent, for example by pressing the 'cancel' button on the login screen. It verifies that the user is redirected back to the relying party's redirect_uri with an 'access_denied' error.\n\nYou may need to clear any cookies for the authorization server before running this test, to remove any existing login session and hence ensure the user is offered the opportunity to reject the authentication.",
	profile = CMF_OP_PROFILE
)
class CMFChileOPUserRejectsAuthentication : AbstractCMFChileOPMultipleClientTestModule() {

	override fun legs(): List<AuthorizationLeg> {
		val first = flow(OPClient.FIRST)
		val second = flow(OPClient.SECOND)

		return listOf(
			AuthorizationLeg(
				beforeRedirect = withLongState(first),
				afterCallback = first.errorCallback(accessDeniedChecks())
			),
			AuthorizationLeg(
				beforeRedirect = Steps.of("Second client authorization request", secondClientSetupSteps(), withLongState(second)),
				afterCallback = Steps.of(
					"Second client callback",
					Steps.action("Expect dummy1 and dummy2 in error response") { env ->
						// the second client's redirect uri has a query, which the server must preserve
						env.putBoolean(CheckForUnexpectedParametersInErrorResponseFromAuthorizationEndpoint.expectDummy1Dummy2Key, true)
					},
					second.errorCallback(accessDeniedChecks()),
					condition(CheckMatchingCallbackParameters::class.java)
				)
			)
		)
	}

	private fun withLongState(flow: OPFlow): TestExecutionUnit =
		Steps.modify(flow.beforeRedirect())
			.insertAfter(SET_REQUESTED_STATE_LENGTH, putInteger("Request 128 character state", "requested_state_length", 128))
			.build()

	private fun accessDeniedChecks(): TestExecutionUnit =
		authorizationErrorResponseSteps(
			continueOnFailure().requirements("OIDCC-3.1.2.6", "RFC6749-4.1.2.1")
				.condition(ExpectAccessDeniedErrorFromAuthorizationEndpointDueToUserRejectingRequest::class.java)
		)
}
