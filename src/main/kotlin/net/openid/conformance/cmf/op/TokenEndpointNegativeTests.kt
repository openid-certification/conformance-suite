package net.openid.conformance.cmf.op

import net.openid.conformance.condition.Condition.ConditionResult
import net.openid.conformance.condition.client.*
import net.openid.conformance.sequence.Steps
import net.openid.conformance.sequence.Steps.condition
import net.openid.conformance.sequence.Steps.continueOnFailure
import net.openid.conformance.sequence.Steps.stopOnFailure
import net.openid.conformance.testmodule.Environment
import net.openid.conformance.testmodule.PublishTestModule
import net.openid.conformance.testmodule.TestExecutionUnit

private fun tokenEndpointReturned200(env: Environment): Boolean = env.getInteger("token_endpoint_response_http_status") == 200

@PublishTestModule(
	testName = "cmf-chile-op-ensure-client-id-in-token-endpoint",
	displayName = "Chile CMF OP: ensure client_id in token endpoint",
	summary = "Send client_id for the second client to the token endpoint, which should result in the token endpoint returning an error message that the client is invalid. Note that you must configure the second client to use client credentials that are not equivalent to those for the first client - e.g. the certificates must have different subject distinguished names.",
	profile = CMF_OP_PROFILE
)
class CMFChileOPEnsureClientIdInTokenEndpoint : AbstractCMFChileOPMultipleClientTestModule() {

	override fun legs(): List<AuthorizationLeg> {
		val flow = flow()
		return listOf(
			AuthorizationLeg(
				beforeRedirect = flow.beforeRedirect(),
				afterCallback = flow.afterCallback(
					postAuthorization = Steps.of(
						"Token endpoint with second client's client_id",
						// client 1's code and certificate, with client 2's client_id
						Steps.modify(flow.callTokenEndpoint)
							.replace(
								AddClientIdToRequest::class.java,
								Steps.block(
									"Swapping to Client2",
									mapKey("Use second client", "client", "client2"),
									stopOnFailure().requirements("RFC6749-5.2").condition(AddClientIdToRequest::class.java),
									unmapKey("Use first client", "client")
								)
							)
							.build(),
						// the specs define no order for checking the client against its credentials and against the
						// code, so either error is accepted
						tokenEndpointErrorSteps(
							continueOnFailure().requirements("RFC6749-5.2").condition(CheckTokenEndpointHttpStatusIs400Allowing401ForInvalidClientError::class.java),
							continueOnFailure().requirements("RFC6749-5.2").condition(CheckErrorFromTokenEndpointResponseErrorInvalidClientOrInvalidGrant::class.java)
						)
					)
				)
			)
		)
	}
}

@PublishTestModule(
	testName = "cmf-chile-op-ensure-holder-of-key-required",
	displayName = "Chile CMF OP: ensure holder of key required",
	summary = "This test ensures that all endpoints comply with the TLS version/cipher limitations and that the token endpoint returns an error if a valid request is sent without a holder of key mechanism (i.e. without MTLS).",
	profile = CMF_OP_PROFILE
)
class CMFChileOPEnsureHolderOfKeyRequired : AbstractCMFChileOPTestModule() {

	override fun legs(): List<AuthorizationLeg> {
		val flow = flow()
		return listOf(
			AuthorizationLeg(
				beforeRedirect = Steps.of(
					"TLS tests and authorization request",
					endpointTlsSteps(),
					flow.beforeRedirect()
				),
				afterCallback = flow.afterCallback(
					postAuthorization = Steps.of(
						"Token endpoint without certificate",
						// client_id identifies the client, so the server cannot answer invalid_client merely because
						// it does not know which client is calling
						Steps.modify(flow.callTokenEndpoint)
							.replace(
								CallTokenEndpointAndReturnFullResponse::class.java,
								Steps.of(
									null,
									condition(RemoveMTLSCertificates::class.java),
									stopOnFailure().requirements("FAPI2-SP-FINAL-5.3.2.1-6").condition(CallTokenEndpointAllowingTLSFailure::class.java)
								)
							)
							.build(),
						tokenEndpointRejectedWithoutCertificateSteps(httpStatusWarnOnly = true)
					)
				)
			)
		)
	}

	private fun endpointTlsSteps(): TestExecutionUnit =
		Steps.of(
			"Endpoint TLS",
			condition(ExtractTLSTestValuesFromServerConfiguration::class.java),
			// browsers visit the authorization endpoint, so it may offer additional ciphers
			tlsEndpointSteps("Authorization endpoint TLS test", "authorization_endpoint_tls", onlyRecommendedCiphers = false),
			tlsEndpointSteps("Userinfo Endpoint TLS test", "userinfo_endpoint_tls", optional = true),
			tlsEndpointSteps("Token Endpoint TLS test", "token_endpoint_tls", warnOnInsecureCiphers = true),
			tlsEndpointSteps("Registration Endpoint TLS test", "registration_endpoint_tls", optional = true)
		)
}

@PublishTestModule(
	testName = "cmf-chile-op-ensure-authorization-code-is-bound-to-client",
	displayName = "Chile CMF OP: ensure authorization code is bound to client",
	summary = "This test ensures the token endpoint returns an error if a valid authorization code is used with another client's credentials.",
	profile = CMF_OP_PROFILE
)
class CMFChileOPEnsureAuthorizationCodeIsBoundToClient : AbstractCMFChileOPMultipleClientTestModule() {

	override fun legs(): List<AuthorizationLeg> {
		val flow = flow()
		return listOf(
			AuthorizationLeg(
				beforeRedirect = flow.beforeRedirect(),
				afterCallback = flow.afterCallback(
					postAuthorization = Steps.of(
						"Token endpoint with second client",
						switchToSecondClient(),
						flow.callTokenEndpoint,
						tokenEndpointErrorSteps(
							stopOnFailure().requirements("OIDCC-3.1.3.4").condition(CheckTokenEndpointHttpStatus400::class.java),
							continueOnFailure().requirements("RFC6749-5.2").condition(CheckErrorFromTokenEndpointResponseErrorInvalidGrant::class.java)
						),
						switchToFirstClient()
					)
				)
			)
		)
	}
}

@PublishTestModule(
	testName = "cmf-chile-op-attempt-reuse-authorization-code-after-one-second",
	displayName = "Chile CMF OP: try to reuse authorization code after one second",
	summary = "This test tries reusing an authorization code after one second, as the authorization code has already been used this must fail with the AS returning an invalid_grant error.\n\nAny issued access token 'should' also be revoked as per RFC6749 section 4.1.2 (although this is only recommended behaviour and this warning won't prevent you certifying) - see https://bitbucket.org/openid/fapi/issues/397/query-over-certification-test-for-access",
	profile = CMF_OP_PROFILE
)
class CMFChileOPAttemptReuseAuthorizationCodeAfterOneSecond : AbstractCMFChileOPTestModule() {

	private val codeFlow by lazy { flow() }

	override fun legs(): List<AuthorizationLeg> = listOf(AuthorizationLeg(codeFlow.beforeRedirect(), codeFlow.afterCallback()))

	override fun afterLastLeg(): TestExecutionUnit =
		Steps.of(
			"Reuse authorization code",
			Steps.block(
				"Attempting reuse of authorization code",
				condition(WaitForOneSecond::class.java),
				Steps.modify(codeFlow.callTokenEndpoint)
					.replace(
						CallTokenEndpointAndReturnFullResponse::class.java,
						stopOnFailure().requirements("FAPI2-SP-FINAL-5.3.2.2-9").condition(CallTokenEndpointAndReturnFullResponse::class.java)
					)
					.build(),
				Steps.`when`(
					"token endpoint accepted the reused code",
					::tokenEndpointReturned200,
					continueOnFailure().requirements("FAPI2-SP-FINAL-5.3.2.2-9").condition(ServerAllowedReusingAuthorizationCode::class.java),
					tokenEndpointInvalidGrantSteps()
				)
			),
			// the first access token is still in access_token, as the error response was not extracted
			Steps.block(
				"Testing if access token was revoked after authorization code reuse (the AS 'should' have revoked the access token)",
				condition(CreateEmptyResourceEndpointRequestHeaders::class.java),
				stopOnFailure().requirements("RFC6749-4.1.2").condition(CallProtectedResource::class.java),
				onEndpointResponse(
					"resource_endpoint_response_full",
					continueOnFailure().onFail(ConditionResult.WARNING).requirements("RFC6749-4.1.2", "RFC6750-3.1")
						.condition(EnsureHttpStatusCodeIs4xx::class.java)
				)
			)
		)
}

@PublishTestModule(
	testName = "cmf-chile-op-ensure-token-endpoint-fails-with-expired-auth-code",
	displayName = "Chile CMF OP: try to use authorization code after its lifetime has expired",
	summary = "This test tries using an authorization code, for the first time, after its lifetime of 60 seconds has expired, This must fail with the AS returning an invalid_grant error.",
	profile = CMF_OP_PROFILE
)
class CMFChileOPAttemptToUseExpiredAuthCode : AbstractCMFChileOPTestModule() {

	override fun legs(): List<AuthorizationLeg> {
		val flow = flow()
		return listOf(
			AuthorizationLeg(
				beforeRedirect = flow.beforeRedirect(),
				afterCallback = flow.afterCallback(
					postAuthorization = Steps.of(
						"Token endpoint with expired code",
						// 60 seconds plus 2 seconds of clock skew
						condition(WaitFor62Seconds::class.java),
						flow.callTokenEndpoint,
						Steps.block(
							"Verify token endpoint response",
							Steps.`when`(
								"token endpoint accepted the expired code",
								::tokenEndpointReturned200,
								continueOnFailure().requirements("FAPI2-SP-FINAL-5.3.2.1-11").condition(ServerAllowedExpiredAuthorizationCode::class.java),
								tokenEndpointInvalidGrantSteps()
							)
						)
					)
				)
			)
		)
	}
}

/** The token endpoint must reject the code exchange with invalid_grant because of the code_verifier. */
abstract class AbstractCMFChileOPCodeVerifierRejected : AbstractCMFChileOPTestModule() {

	/** Changes the code_verifier sent to the token endpoint. */
	protected abstract fun changeCodeVerifier(modifier: Steps.Modifier)

	override fun legs(): List<AuthorizationLeg> {
		val flow = flow()
		return listOf(
			AuthorizationLeg(
				beforeRedirect = flow.beforeRedirect(),
				afterCallback = flow.afterCallback(
					postAuthorization = Steps.of(
						"Token endpoint with bad code_verifier",
						Steps.modify(flow.callTokenEndpoint).also(::changeCodeVerifier).build(),
						Steps.block(
							"Verify token endpoint response",
							tokenEndpointErrorSteps(
								continueOnFailure().requirements("RFC6749-5.2").condition(CheckTokenEndpointHttpStatus400::class.java),
								continueOnFailure().requirements("RFC7636-4.6", "RFC6749-5.2")
									.condition(CheckErrorFromTokenEndpointResponseErrorInvalidGrant::class.java)
							)
						)
					)
				)
			)
		)
	}
}

@PublishTestModule(
	testName = "cmf-chile-op-ensure-pkce-code-verifier-required",
	displayName = "Chile CMF OP: Ensure PKCE code_verifier required",
	summary = "This test authenticates as normal except that when calling the token endpoint it omits the 'code_verifier' parameter. The test must end with the token endpoint returning an 'invalid_grant' error, as PKCE is required and requires the code_verifier parameter.",
	profile = CMF_OP_PROFILE
)
class CMFChileOPPAREnsurePKCECodeVerifierRequired : AbstractCMFChileOPCodeVerifierRejected() {
	override fun changeCodeVerifier(modifier: Steps.Modifier) {
		modifier.skip(AddCodeVerifierToTokenEndpointRequest::class.java, "The code_verifier is omitted from the token request")
	}
}

@PublishTestModule(
	testName = "cmf-chile-op-incorrect-pkce-code-verifier-rejected",
	displayName = "Chile CMF OP: Incorrect PKCE code_verifier rejected",
	summary = "This test authenticates as normal except that when calling the token endpoint it passes an incorrect value in the 'code_verifier' parameter. The test must end with the token endpoint returning an 'invalid_grant' error, due to the incorrect the code_verifier parameter.",
	profile = CMF_OP_PROFILE
)
class CMFChileOPPARIncorrectPKCECodeVerifierRejected : AbstractCMFChileOPCodeVerifierRejected() {
	override fun changeCodeVerifier(modifier: Steps.Modifier) {
		modifier.insertBefore(
			AddCodeVerifierToTokenEndpointRequest::class.java,
			stopOnFailure().requirements("RFC7636-4.1").condition(CreateRandomCodeVerifier::class.java)
		)
	}
}
