package net.openid.conformance.cmf.op

import net.openid.conformance.condition.Condition.ConditionResult
import net.openid.conformance.condition.client.*
import net.openid.conformance.condition.common.RARSupport
import net.openid.conformance.sequence.Steps
import net.openid.conformance.sequence.Steps.condition
import net.openid.conformance.sequence.Steps.continueOnFailure
import net.openid.conformance.sequence.Steps.stopOnFailure
import net.openid.conformance.testmodule.PublishTestModule
import net.openid.conformance.testmodule.TestExecutionUnit

@PublishTestModule(
	testName = "cmf-chile-op-refresh-token",
	displayName = "Chile CMF OP: test refresh token behaviours",
	summary = "This test obtains refresh tokens for both clients and performs various checks, including that a refreshed access token works at the resource endpoint, that a rotated refresh token's predecessor remains usable (FAPI 2.0 Security Profile 5.3.2.1), that the refresh token is bound to the client's certificate and that client 1 cannot use the refresh token issued to client 2.",
	profile = CMF_OP_PROFILE
)
class CMFChileOPRefreshToken : AbstractCMFChileOPMultipleClientTestModule() {

	override fun legs(): List<AuthorizationLeg> {
		val first = flow(OPClient.FIRST)
		val second = flow(OPClient.SECOND)

		return listOf(
			AuthorizationLeg(
				beforeRedirect = withPromptConsent(first),
				afterCallback = first.afterCallback(postAuthorization = withRefreshToken(first, refreshTokenWithoutCertificate()))
			),
			AuthorizationLeg(
				beforeRedirect = Steps.of(
					"Second client authorization request",
					Steps.action("Forget the first client's tokens") { env ->
						env.removeNativeValue("refresh_token")
						env.unmapKey("access_token")
						env.unmapKey("id_token")
					},
					secondClientSetupSteps(),
					withPromptConsent(second)
				),
				afterCallback = second.afterCallback(postAuthorization = withRefreshToken(second, Steps.NONE))
			)
		)
	}

	override fun afterLastLeg(): TestExecutionUnit =
		Steps.of(
			"Client 2 tokens with client 1",
			client2TokenWithClient1CertificateSteps(),
			Steps.block(
				"Attempting to use refresh_token issued to client 2 with client 1",
				condition(CreateRefreshTokenRequest::class.java),
				mtlsClientAuthentication("token_endpoint_request_form_parameters", "token_endpoint_request_headers"),
				condition(CallTokenEndpointAndReturnFullResponse::class.java),
				condition(ValidateErrorFromTokenEndpointResponseError::class.java),
				continueOnFailure().requirements("OIDCC-3.1.3.4").of(
					"Error response",
					CheckTokenEndpointHttpStatus400::class.java,
					CheckTokenEndpointReturnedJsonContentType::class.java
				),
				continueOnFailure().requirements("RFC6749-5.2").condition(CheckErrorFromTokenEndpointResponseErrorInvalidGrant::class.java)
			)
		)

	/** offline_access needs prompt=consent (OIDCC 11), or the server need not issue a refresh token. */
	private fun withPromptConsent(flow: OPFlow): TestExecutionUnit =
		Steps.modify(flow.beforeRedirect())
			.insertAfter(
				RARSupport.AddRARToAuthorizationEndpointRequest::class.java,
				stopOnFailure().requirements("OIDCC-11").condition(AddPromptConsentToAuthorizationEndpointRequestIfScopeContainsOfflineAccess::class.java)
			)
			.build()

	/**
	 * The code exchange keeps its tokens as first_access_token and first_id_token; the refresh then puts the new
	 * ones in second_access_token and second_id_token, which the resource endpoint call and [CompareIdTokenClaims]
	 * use.
	 */
	private fun withRefreshToken(flow: OPFlow, clientBindingChecks: TestExecutionUnit): TestExecutionUnit =
		Steps.of(
			"Post authorization",
			Steps.action("Keep the code exchange tokens as first tokens") { env ->
				env.mapKey("access_token", "first_access_token")
				env.mapKey("id_token", "first_id_token")
			},
			flow.callTokenEndpoint,
			flow.verifyTokenEndpointResponse,
			Steps.action("Keep the refreshed tokens as second tokens") { env ->
				env.mapKey("access_token", "second_access_token")
				env.mapKey("id_token", "second_id_token")
			},
			refreshTokenSteps(flow),
			clientBindingChecks,
			flow.callResourceEndpoint
		)

	private fun refreshTokenSteps(flow: OPFlow): TestExecutionUnit =
		Steps.of(
			"Refresh token",
			Steps.block(
				flow.client.logPrefix + "Check for refresh token",
				continueOnFailure().onFail(ConditionResult.INFO).condition(ExtractRefreshTokenFromTokenResponse::class.java),
				Steps.`when`(
					"no refresh token was issued",
					{ env -> env.getString("refresh_token").isNullOrEmpty() },
					Steps.of(
						"No refresh token",
						continueOnFailure().onFail(ConditionResult.WARNING).requirements("OIDCD-3")
							.condition(FAPIEnsureServerConfigurationDoesNotSupportRefreshToken::class.java),
						Steps.action("Skip the test") { fireTestSkipped("Refresh tokens cannot be tested. No refresh token was issued.") }
					)
				),
				continueOnFailure().onFail(ConditionResult.WARNING).requirements("OIDCD-3")
					.condition(EnsureServerConfigurationSupportsRefreshToken::class.java),
				continueOnFailure().requirements("RFC6749-A.17").condition(EnsureRefreshTokenContainsAllowedCharactersOnly::class.java)
			),
			Steps.action("Keep the issued refresh token") { env ->
				env.putString("refresh_token_prev", env.getString("refresh_token"))
			},
			refreshTokenRequest(flow, "Refresh Token Request"),
			Steps.`when`(
				"refresh token was rotated",
				{ env -> env.getString("refresh_token_prev") != env.getString("refresh_token") },
				Steps.of(
					"Previous refresh token",
					Steps.action("Use the previous refresh token") { env ->
						env.putString("refresh_token", env.getString("refresh_token_prev"))
					},
					condition(WaitFor30Seconds::class.java),
					refreshTokenRequest(flow, "Refresh Token Request With Previous Token, FAPI 2.0 Security Profile 5.3.2.1-9")
				),
				Steps.check("Refresh token not rotated") { _, c ->
					c.log("Refresh token not rotated. Skipping lost refresh token test.")
				}
			),
			Steps.action("Forget the previous refresh token") { env -> env.removeNativeValue("refresh_token_prev") }
		)

	/** A successful refresh_token grant, as the configured client. */
	private fun refreshTokenRequest(flow: OPFlow, title: String): TestExecutionUnit =
		Steps.block(
			flow.client.logPrefix + title,
			condition(CreateRefreshTokenRequest::class.java),
			if (flow.client.isSecond) Steps.NONE else stopOnFailure().requirements("RFC6749-6").condition(AddScopeToTokenEndpointRequest::class.java),
			mtlsClientAuthentication("token_endpoint_request_form_parameters", "token_endpoint_request_headers"),
			// so that the iat of a refreshed id token differs from the original
			condition(WaitForOneSecond::class.java),
			condition(CallTokenEndpointAndReturnFullResponse::class.java),
			continueOnFailure().requirements("RFC6749-5.1").of(
				"Token endpoint response",
				CheckTokenEndpointHttpStatus200::class.java,
				CheckTokenEndpointReturnedJsonContentType::class.java,
				CheckTokenEndpointCacheHeaders::class.java
			),
			condition(CheckIfTokenEndpointResponseError::class.java),
			condition(ExtractAccessTokenFromTokenResponse::class.java),
			continueOnFailure().requirements("FAPI-R-6.2.2-1", "FAPI1-BASE-6.2.2-1").condition(CheckTokenTypeIsBearer::class.java),
			continueOnFailure().requirements("FAPI-R-5.2.2-16", "FAPI1-BASE-5.2.2-16").condition(EnsureMinimumAccessTokenEntropy::class.java),
			continueOnFailure().requirements("RFC6749-A.12").condition(EnsureAccessTokenContainsAllowedCharactersOnly::class.java),
			continueOnFailure().onFail(ConditionResult.WARNING).requirements("RFC6749-6", "RFC6749-5.1")
				.condition(ExtractExpiresInFromTokenEndpointResponse::class.java),
			continueOnFailure().requirements("RFC6749-5.1").condition(ValidateExpiresIn::class.java).skipIfObjectMissing("expires_in"),
			continueOnFailure().onFail(ConditionResult.INFO).condition(EnsureAccessTokenValuesAreDifferent::class.java),
			if (openid) Steps.NONE else condition(ExpectNoIdTokenInTokenResponse::class.java),
			continueOnFailure().onFail(ConditionResult.INFO).condition(ValidateIdTokenFromTokenResponseEncryption::class.java)
				.skipIfObjectMissing("client_jwks").onSkip(ConditionResult.INFO),
			continueOnFailure().onFail(ConditionResult.INFO).condition(ExtractIdTokenFromTokenResponse::class.java),
			// a server need not issue a new refresh token, in which case the previous one stays in use
			continueOnFailure().onFail(ConditionResult.INFO).condition(ExtractRefreshTokenFromTokenResponse::class.java),
			continueOnFailure().requirements("RFC6749-10.10").condition(EnsureMinimumRefreshTokenLength::class.java)
				.skipIfElementMissing("token_endpoint_response", "refresh_token"),
			continueOnFailure().requirements("RFC6749-10.10").condition(EnsureMinimumRefreshTokenEntropy::class.java)
				.skipIfElementMissing("token_endpoint_response", "refresh_token"),
			continueOnFailure().requirements("OIDCC-12.2").condition(CompareIdTokenClaims::class.java).skipIfObjectMissing("second_id_token")
		)

	/**
	 * The refresh token is bound to the client's certificate, which under tls_client_auth also authenticates the
	 * client, so a request without the certificate must be rejected, either by dropping the TLS connection or with
	 * an error response.
	 */
	private fun refreshTokenWithoutCertificate(): TestExecutionUnit =
		Steps.block(
			"Attempting to use an MTLS sender constrained refresh_token without proof of possession",
			mapKey("Remove the client certificate", "mutual_tls_authentication", "none_existent_key"),
			condition(CreateRefreshTokenRequest::class.java),
			stopOnFailure().requirements("RFC6749-6").condition(AddScopeToTokenEndpointRequest::class.java),
			mtlsClientAuthentication("token_endpoint_request_form_parameters", "token_endpoint_request_headers"),
			stopOnFailure().requirements("FAPI2-SP-FINAL-5.3.2.1-6").condition(CallTokenEndpointAllowingTLSFailure::class.java),
			tokenEndpointRejectedWithoutCertificateSteps(),
			unmapKey("Restore the client certificate", "mutual_tls_authentication")
		)
}
