package net.openid.conformance.cmf.rp

import net.openid.conformance.condition.Condition.ConditionResult
import net.openid.conformance.condition.`as`.AddAudValueAsArrayToIdToken
import net.openid.conformance.condition.`as`.CalculateAtHash
import net.openid.conformance.condition.`as`.CheckForInvalidCharsInNonce
import net.openid.conformance.condition.`as`.CheckForInvalidCharsInState
import net.openid.conformance.condition.`as`.CheckNonceMaximumLength
import net.openid.conformance.condition.`as`.CheckStateLength
import net.openid.conformance.condition.`as`.CreateAuthorizationEndpointResponseParams
import net.openid.conformance.condition.`as`.CreateEffectiveAuthorizationRequestParameters
import net.openid.conformance.condition.`as`.GenerateAccessTokenExpiration
import net.openid.conformance.condition.`as`.RemoveAccessTokenExpiration
import net.openid.conformance.condition.`as`.RemoveMtlsEndpointAliasesFromServerConfiguration
import net.openid.conformance.condition.`as`.SetParEndpointToMtlsParEndpoint
import net.openid.conformance.condition.`as`.SetTokenEndpointToMtlsTokenEndpoint
import net.openid.conformance.condition.`as`.SetTokenResponseTokenTypeToInvertedCase
import net.openid.conformance.condition.`as`.SetUserinfoEndpointToMtlsUserinfoEndpoint
import net.openid.conformance.condition.`as`.SignIdToken
import net.openid.conformance.condition.`as`.SignIdTokenBypassingNimbusChecks
import net.openid.conformance.condition.common.CheckDistinctKeyIdValueInServerJWKs
import net.openid.conformance.condition.common.GrantManagementSupport
import net.openid.conformance.sequence.Steps
import net.openid.conformance.sequence.Steps.condition
import net.openid.conformance.sequence.Steps.continueOnFailure
import net.openid.conformance.sequence.Steps.stopOnFailure
import net.openid.conformance.testmodule.PublishTestModule
import net.openid.conformance.testmodule.TestExecutionUnit
import net.openid.conformance.variant.FAPIClientType
import net.openid.conformance.variant.VariantNotApplicable

const val CMF_RP_PROFILE = "CMF-Chile-RP"

@PublishTestModule(
	testName = "cmf-chile-rp-happy-path",
	displayName = "Chile CMF RP: client test for happy path",
	summary = "Tests a 'happy path' flow; the client should perform OpenID discovery from the displayed discoveryUrl, push a signed authorization request to the PAR endpoint, call the authorization endpoint (which will immediately redirect back), exchange the authorization code for an access token at the token endpoint and make a GET request to the accounts endpoint displayed. Every call except discovery, jwks and the authorization endpoint must use the mtls_endpoint_aliases.",
	profile = CMF_RP_PROFILE
)
open class CMFChileRPHappyPath : AbstractCMFChileRPTestModule() {

	override fun extraConfiguration(): TestExecutionUnit =
		continueOnFailure().onFail(ConditionResult.WARNING).requirements("RFC7517-4.5", "FAPI2-SP-FINAL-5.4.2-3")
			.condition(CheckDistinctKeyIdValueInServerJWKs::class.java)

	override fun server(): RPServer {
		val server = super.server()
		return server.copy(
			authorizationEndpoint = Steps.modify(server.authorizationEndpoint)
				.insertBefore(CreateAuthorizationEndpointResponseParams::class.java, requestValueChecks())
				.build()
		)
	}

	/** The nonce, or the state when it is the only value the client binds the response with, is well formed. */
	private fun requestValueChecks(): TestExecutionUnit =
		if (openid) {
			Steps.of(
				"Nonce checks",
				continueOnFailure().onFail(ConditionResult.WARNING).condition(CheckForInvalidCharsInNonce::class.java)
					.skipIfStringMissing("nonce").onSkip(ConditionResult.INFO),
				continueOnFailure().onFail(ConditionResult.WARNING).condition(CheckNonceMaximumLength::class.java)
					.skipIfStringMissing("nonce").onSkip(ConditionResult.INFO)
			)
		} else {
			Steps.of(
				"State checks",
				continueOnFailure().onFail(ConditionResult.WARNING).condition(CheckForInvalidCharsInState::class.java)
					.skipIfElementMissing(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, CreateEffectiveAuthorizationRequestParameters.STATE)
					.onSkip(ConditionResult.INFO),
				continueOnFailure().onFail(ConditionResult.WARNING).condition(CheckStateLength::class.java)
					.skipIfElementMissing(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, CreateEffectiveAuthorizationRequestParameters.STATE)
					.onSkip(ConditionResult.INFO)
			)
		}
}

@PublishTestModule(
	testName = "cmf-chile-rp-happy-path-no-mtls-endpoint-aliases",
	displayName = "Chile CMF RP: client test for happy path without mtls_endpoint_aliases",
	summary = "Tests a 'happy path' flow where the server does not publish mtls_endpoint_aliases: the token, userinfo, PAR and grant management endpoints in the discovery metadata are the mTLS ones. The client should perform OpenID discovery from the displayed discoveryUrl, push a signed authorization request to the PAR endpoint, call the authorization endpoint (which will immediately redirect back), exchange the authorization code for an access token at the token endpoint and make a GET request to the accounts endpoint displayed, calling each endpoint at the URL the metadata gives.",
	profile = CMF_RP_PROFILE
)
class CMFChileRPHappyPathNoMtlsEndpointAliases : CMFChileRPHappyPath() {

	override fun serverConfiguration(): TestExecutionUnit =
		Steps.modify(super.serverConfiguration())
			.insertAfter(GrantManagementSupport.AddGrantManagementToServerConfiguration::class.java, endpointsWithoutMtlsAliases())
			.build()

	/** Every endpoint the profile serves only over mTLS is published at its mTLS URL, without the aliases. */
	private fun endpointsWithoutMtlsAliases(): TestExecutionUnit =
		Steps.of(
			"Endpoints without mtls_endpoint_aliases",
			stopOnFailure().requirements("RFC8705-5").condition(SetTokenEndpointToMtlsTokenEndpoint::class.java),
			stopOnFailure().requirements("RFC8705-5").condition(SetUserinfoEndpointToMtlsUserinfoEndpoint::class.java),
			stopOnFailure().requirements("RFC8705-5").condition(SetParEndpointToMtlsParEndpoint::class.java),
			Steps.check("Set grant management endpoint to the mTLS grant management endpoint") { env, c ->
				val endpoint = env.getString("server", "mtls_endpoint_aliases.grant_management_endpoint")
				env.putString("server", "grant_management_endpoint", endpoint)
				c.log("Set grant management endpoint to be the MTLS grant management endpoint", mapOf("grant_management_endpoint" to endpoint))
			},
			stopOnFailure().requirements("RFC8705-5").condition(RemoveMtlsEndpointAliasesFromServerConfiguration::class.java)
		)
}

@PublishTestModule(
	testName = "cmf-chile-rp-valid-aud-as-array",
	displayName = "Chile CMF RP: client test - valid aud in id_token as data type array",
	summary = "This test should be successful. The value of aud within the id_token will be represented as array with one value",
	profile = CMF_RP_PROFILE
)
@VariantNotApplicable(parameter = FAPIClientType::class, values = ["plain_oauth"])
class CMFChileRPValidAudAsArray : AbstractCMFChileRPTestModule() {

	override fun server(): RPServer {
		val server = super.server()
		return server.copy(
			tokenEndpoint = Steps.modify(server.tokenEndpoint)
				.insertBefore(
					SignIdToken::class.java,
					stopOnFailure().requirements("OIDCC-3.1.3.7-3").condition(AddAudValueAsArrayToIdToken::class.java)
				)
				// signing through Nimbus claims would turn a single element aud array back into a string
				.replace(SignIdToken::class.java, condition(SignIdTokenBypassingNimbusChecks::class.java))
				.build()
		)
	}
}

@PublishTestModule(
	testName = "cmf-chile-rp-token-endpoint-response-without-expires-in",
	displayName = "Chile CMF RP: client test - return the token endpoint response without the expires_in parameter",
	summary = "Tests a 'happy path' flow where the token endpoint response does not contain expires_in; the client should perform OpenID discovery from the displayed discoveryUrl, push a signed authorization request to the PAR endpoint, call the authorization endpoint (which will immediately redirect back), exchange the authorization code for an access token at the token endpoint and make a GET request to the accounts endpoint displayed.",
	profile = CMF_RP_PROFILE
)
class CMFChileRPTokenEndpointResponseWithoutExpiresIn : AbstractCMFChileRPTestModule() {

	override fun server(): RPServer {
		val server = super.server()
		return server.copy(
			tokenEndpoint = Steps.modify(server.tokenEndpoint)
				.insertAfter(
					GenerateAccessTokenExpiration::class.java,
					continueOnFailure().onFail(ConditionResult.INFO).condition(RemoveAccessTokenExpiration::class.java)
				)
				.build()
		)
	}
}

@PublishTestModule(
	testName = "cmf-chile-rp-token-type-case-insensitivity",
	displayName = "Chile CMF RP: test client for case insensitivity of token type in token endpoint response",
	summary = "Tests whether the client has case sensitiveness when an all inverted case token_type is returned in the token endpoint response",
	profile = CMF_RP_PROFILE
)
class CMFChileRPTokenTypeCaseInsensitivity : AbstractCMFChileRPTestModule() {

	override fun server(): RPServer {
		val server = super.server()
		return server.copy(
			tokenEndpoint = Steps.modify(server.tokenEndpoint)
				.insertAfter(
					CalculateAtHash::class.java,
					continueOnFailure().onFail(ConditionResult.INFO).condition(SetTokenResponseTokenTypeToInvertedCase::class.java)
				)
				.build()
		)
	}
}
