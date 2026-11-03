package net.openid.conformance.cmf.rp

import com.google.gson.JsonObject
import net.openid.conformance.condition.Condition.ConditionResult
import net.openid.conformance.condition.`as`.*
import net.openid.conformance.condition.`as`.par.CreatePAREndpointResponse
import net.openid.conformance.condition.`as`.par.EnsureAuthorizationRequestContainsOnlyExpectedParamsWhenUsingPAR
import net.openid.conformance.condition.`as`.par.EnsureAuthorizationRequestDoesNotContainRequestWhenUsingPAR
import net.openid.conformance.condition.`as`.par.EnsureRequestObjectContainsCodeChallengeWhenUsingPAR
import net.openid.conformance.condition.`as`.par.ExtractRequestObjectFromPAREndpointRequest
import net.openid.conformance.condition.client.AugmentRealJwksWithDecoys
import net.openid.conformance.condition.client.EnsureIncomingRequestBodyIsEmpty
import net.openid.conformance.condition.client.EnsureIncomingUrlQueryIsEmpty
import net.openid.conformance.condition.client.ExtractJWKsFromStaticClientConfiguration
import net.openid.conformance.condition.client.FAPIValidateRequestObjectIdTokenACRClaims
import net.openid.conformance.condition.client.GetStaticClientConfiguration
import net.openid.conformance.condition.common.CheckDistinctKeyIdValueInClientJWKs
import net.openid.conformance.condition.common.CheckServerConfiguration
import net.openid.conformance.condition.common.EnsureIncomingTls12WithBCP195SecureCipherOrTls13
import net.openid.conformance.condition.common.EnsureIncomingTls13
import net.openid.conformance.condition.common.GrantManagementSupport
import net.openid.conformance.condition.common.RARSupport
import net.openid.conformance.condition.rs.ClearAccessTokenFromRequest
import net.openid.conformance.condition.rs.CreateFAPIAccountEndpointResponse
import net.openid.conformance.condition.rs.EnsureBearerAccessTokenNotInParams
import net.openid.conformance.condition.rs.EnsureIncomingRequestMethodIsGet
import net.openid.conformance.condition.rs.ExtractBearerAccessTokenFromHeader
import net.openid.conformance.condition.rs.ExtractFapiDateHeader
import net.openid.conformance.condition.rs.ExtractFapiInteractionIdHeader
import net.openid.conformance.condition.rs.ExtractFapiIpAddressHeader
import net.openid.conformance.condition.rs.LoadUserInfo
import net.openid.conformance.condition.rs.RequireMtlsAccessToken
import net.openid.conformance.sequence.Steps
import net.openid.conformance.sequence.Steps.condition
import net.openid.conformance.sequence.Steps.continueOnFailure
import net.openid.conformance.sequence.Steps.stopOnFailure
import net.openid.conformance.sequence.ValidateJwksSequence
import net.openid.conformance.sequence.`as`.AddPARToServerConfiguration
import net.openid.conformance.sequence.`as`.ValidateClientAuthenticationWithMTLS
import net.openid.conformance.testmodule.ConditionCallBuilder
import net.openid.conformance.testmodule.Endpoint
import net.openid.conformance.testmodule.Environment
import net.openid.conformance.testmodule.TestExecutionUnit
import java.util.function.Predicate

/*
 * The steps of the authorization server the suite emulates for FAPI2 Message Signing clients under the Chile Open
 * Finance (CMF) profile: tls_client_auth, mTLS sender constrained tokens, PAR with a signed request object, RAR,
 * plain (non-JARM) responses and grant management, with every client-authenticated and resource endpoint served
 * only at its mTLS alias. Only openid is left to the tester.
 *
 * Each endpoint is an immutable unit run on every request it receives, with the request stored under
 * Endpoint.CURRENT_REQUEST. Modules change one with Steps.modify(...), targeting conditions by class and actions
 * by name.
 */

/** The path of the accounts endpoint, the resource whose call completes a successful flow. */
const val ACCOUNTS_PATH = "open-banking/v1.1/accounts"

/** The path prefix of the grant management endpoint, followed by the grant_id of the grant the request is about. */
const val GRANTS_PATH = "grants/"

/** The environment key, suffixed to a response key, under which an endpoint stores a status other than the default. */
const val HTTP_STATUS_SUFFIX = "_http_status"

/** How many times the client has revoked its grant at the grant management endpoint. */
const val GRANT_REVOCATIONS = "grant_management_revocations"

/** Whether the client has queried its grant at the grant management endpoint. */
const val GRANT_QUERIED = "grant_management_grant_queried"

private const val GRANT_MANAGEMENT_RESPONSE = GrantManagementSupport.CreateGrantManagementQueryResponse.RESPONSE_KEY

/** Copies the form body of a POST request, the only method [endpoint] accepts, to [paramsKey]. */
private fun postParameters(endpoint: String, requestKey: String, paramsKey: String): ConditionCallBuilder =
	Steps.check("Extract $endpoint request parameters") { env, c ->
		val method = env.getString(requestKey, "method")
		if (method != "POST") {
			throw c.error("Got unexpected HTTP method to the $endpoint", mapOf("method" to method))
		}
		env.putObject(paramsKey, env.getObject(requestKey).getAsJsonObject("body_form_params"))
	}

private fun openidScopeRequested(env: Environment): Boolean = env.getString("request_scopes_contain_openid") == "yes"

// ---------------------------------------------------------------------------------------------------------------
// Configuration
// ---------------------------------------------------------------------------------------------------------------

/**
 * The emulated server's discovery metadata and keys. The metadata always advertises mtls_endpoint_aliases, which
 * RFC8705 5 requires clients to support.
 */
internal fun serverConfigurationSteps(openid: Boolean): TestExecutionUnit =
	Steps.of(
		"Server configuration",
		if (openid) {
			Steps.of(
				null,
				condition(GenerateServerConfigurationMTLS::class.java),
				condition(AddJwksUriToServerConfiguration::class.java)
			)
		} else condition(GenerateOauthServerConfigurationMTLS::class.java),
		serverJwksSteps(),
		stopOnFailure().requirements("FAPI2-SP-FINAL-5.3.2.2-1").condition(AddResponseTypeCodeToServerConfiguration::class.java),
		stopOnFailure().requirements("FAPI2-SP-FINAL-5.3.2.2-7").condition(AddIssSupportedToServerConfiguration::class.java),
		stopOnFailure().requirements("FAPI2-SP-FINAL-5.3.2.2").condition(AddCodeChallengeMethodToServerConfiguration::class.java),
		if (openid) {
			Steps.of(
				null,
				condition(AddScopesSupportedOpenIdToServerConfiguration::class.java),
				stopOnFailure().requirements("OIDCD-3").condition(AddSubjectTypesSupportedToServerConfiguration::class.java)
			)
		} else Steps.NONE,
		condition(ExtractServerSigningAlg::class.java),
		condition(AddIdTokenSigningAlgsToServerConfiguration::class.java),
		condition(FAPI2AddRequestObjectSigningAlgValuesSupportedToServerConfiguration::class.java),
		stopOnFailure().requirements("FAPI2-4.3.1-9").condition(AddTlsCertificateBoundAccessTokensTrueSupportedToServerConfiguration::class.java),
		condition(AddTLSClientAuthToServerConfiguration::class.java),
		AddPARToServerConfiguration(),
		condition(FAPI2AddTokenEndpointAuthSigningAlgValuesSupportedToServer::class.java),
		Steps.expose("discoveryUrl"),
		Steps.expose("issuer"),
		Steps.action("Set accounts endpoint") { env ->
			env.putString("accounts_endpoint", env.getString("base_mtls_url") + "/" + ACCOUNTS_PATH)
		},
		Steps.expose("accounts_endpoint"),
		condition(AddSupportedAuthorizationTypesToServerConfiguration::class.java),
		// without this a spec-following client has no reason to use grant management at all
		stopOnFailure().requirements("GM-7.1").condition(GrantManagementSupport.AddGrantManagementToServerConfiguration::class.java),
		stopOnFailure().requirements("OIDCD-3").condition(AddConfiguredScopesToServerConfiguration::class.java),
		condition(CheckServerConfiguration::class.java),
		if (openid) stopOnFailure().requirements("OIDCD-3").condition(EnsureServerConfigurationHasRequiredOidcMetadata::class.java) else Steps.NONE,
		stopOnFailure().requirements("FAPI2-SP-FINAL-5.4.1-2", "FAPI2-SP-FINAL-5.4.1-3").condition(FAPI2FinalEnsureMinimumServerKeyLength::class.java),
		condition(LoadUserInfo::class.java)
	)

/** The configured server signing keys, or generated ones, published alongside decoys and unusable keys. */
private fun serverJwksSteps(): TestExecutionUnit =
	Steps.of(
		"Server keys",
		Steps.`when`(
			"server keys are configured",
			{ env -> env.getElementFromObject("config", "server.jwks") != null },
			Steps.of(
				null,
				condition(LoadServerJWKs::class.java),
				ValidateJwksSequence("server_jwks", null, "server signing keys", "RFC7517-1.1").allowingPrivateKeys()
			),
			condition(FAPI2GenerateServerJWKs::class.java)
		),
		continueOnFailure().onFail(ConditionResult.WARNING).requirements("FAPI2-SP-FINAL-5.4.3-3").condition(AugmentRealJwksWithDecoys::class.java),
		condition(SetRsaAltServerJwks::class.java),
		// published in all tests so the check does not depend on when the client last fetched the JWKS
		stopOnFailure().requirements("RFC7517-5").condition(AddUnusableKeysToServerPublicJwks::class.java)
	)

/** The client's static configuration; client.jwks holds the key the request object is signed with. */
internal fun clientConfigurationSteps(): TestExecutionUnit =
	Steps.block(
		"Verify configuration of first client",
		condition(GetStaticClientConfiguration::class.java),
		ValidateJwksSequence("client", "jwks", "client configuration", "RFC7517-1.1"),
		condition(ExtractJWKsFromStaticClientConfiguration::class.java),
		continueOnFailure().requirements("RFC7517-4.5").condition(CheckDistinctKeyIdValueInClientJWKs::class.java),
		stopOnFailure().requirements("FAPI2-SP-FINAL-5.4.1-2", "FAPI2-SP-FINAL-5.4.1-3").condition(FAPI2FinalEnsureMinimumClientKeyLength::class.java)
	)

// ---------------------------------------------------------------------------------------------------------------
// Endpoints
// ---------------------------------------------------------------------------------------------------------------

/**
 * The steps each endpoint of the emulated server runs on a request, and which requests finish the test. Every
 * property is independent; a module changes one through [copy], e.g.
 * `copy(authorizationEndpoint = Steps.modify(authorizationEndpoint)...)`.
 */
data class RPServer(
	val openid: Boolean,
	val incomingTlsChecks: TestExecutionUnit = incomingTlsChecks(),
	val discoveryEndpoint: TestExecutionUnit = Steps.NONE,
	val parEndpoint: TestExecutionUnit = parEndpoint(openid),
	val authorizationEndpoint: TestExecutionUnit = authorizationEndpoint(openid),
	val tokenEndpoint: TestExecutionUnit = tokenEndpoint(),
	val userinfoEndpoint: TestExecutionUnit = userinfoEndpoint(),
	val accountsEndpoint: TestExecutionUnit = accountsEndpoint(),
	val grantManagementEndpoint: TestExecutionUnit = grantManagementEndpoint(),
	val accountsEndpointFinishesTest: Predicate<Environment> = Predicate { true },
	val grantManagementEndpointFinishesTest: Predicate<Environment> = Predicate { false }
)

/** FAPI2 5.2.1 TLS requirements, checked on every request the client makes to the server. */
private fun incomingTlsChecks(): TestExecutionUnit =
	Endpoint.requestAs(
		"client_request",
		continueOnFailure().onFail(ConditionResult.WARNING)
			.requirements("FAPI2-SP-FINAL-5.2.1-1", "FAPI2-SP-FINAL-5.2.1-2", "FAPI2-SP-FINAL-5.2.1-3")
			.condition(EnsureIncomingTls12WithBCP195SecureCipherOrTls13::class.java),
		continueOnFailure().onFail(ConditionResult.WARNING).requirements("RFC9325-3.1.1").condition(EnsureIncomingTls13::class.java)
	)

/** The client certificate presented on the request mapped as token_endpoint_request must be the configured one. */
private fun mtlsCertificateChecks(): TestExecutionUnit =
	Steps.of(
		"Client certificate",
		continueOnFailure().condition(ExtractClientCertificateFromRequestHeaders::class.java),
		stopOnFailure().requirements("FAPI2-SP-FINAL-5.3.2.1-4", "FAPI2-SP-FINAL-5.3.2.1-5").condition(CheckForClientCertificate::class.java),
		continueOnFailure().condition(EnsureClientCertificateMatches::class.java)
	)

/** tls_client_auth on the request mapped as token_endpoint_request. */
private fun clientAuthenticationSteps(vararg clientIdRequirements: String, emptyClientIdRequirement: String): TestExecutionUnit =
	Steps.of(
		"Client authentication",
		stopOnFailure().requirements(*clientIdRequirements).condition(CheckClientIdMatchesOnTokenRequestIfPresent::class.java),
		continueOnFailure().onFail(ConditionResult.WARNING).requirements(emptyClientIdRequirement)
			.condition(EnsureClientIdIsNotEmptyOnTokenRequestIfPresent::class.java),
		mtlsCertificateChecks(),
		ValidateClientAuthenticationWithMTLS()
	)

private fun parEndpoint(openid: Boolean): TestExecutionUnit =
	Steps.block(
		"PAR endpoint",
		Endpoint.requestAs(
			listOf("par_endpoint_http_request", "incoming_request"),
			Steps.of(
				null,
				// later client_id checks see the request object's value, not the PAR form's
				Endpoint.requestAs(
					"token_endpoint_request",
					clientAuthenticationSteps("PAR-2", "PAR-3", "RFC8705-2", emptyClientIdRequirement = "RFC6749-3.1")
				),
				postParameters("PAR endpoint", "par_endpoint_http_request", "par_endpoint_http_request_params"),
				stopOnFailure().requirements("FAPI2-MS-ID1-5.3.2-1").condition(EnsureSignedPAREndpointRequestContainsRequestParameter::class.java),
				stopOnFailure().requirements("PAR-3").condition(ExtractRequestObjectFromPAREndpointRequest::class.java),
				continueOnFailure().requirements("PAR-3").condition(CheckForUnexpectedParametersInSignedPAREndpointRequest::class.java),
				stopOnFailure().requirements("PAR-2.1").condition(EnsurePAREndpointRequestDoesNotContainRequestUriParameter::class.java),
				encryptedRequestObjectKidCheck(),
				requestObjectChecks(openid),
				stopOnFailure().requirements("FAPI2-SP-FINAL-5.3.2.2-5").condition(EnsureRequestObjectContainsCodeChallengeWhenUsingPAR::class.java),
				stopOnFailure().requirements("PAR-2.2").condition(CreatePAREndpointResponse::class.java)
			)
		)
	)

private fun encryptedRequestObjectKidCheck(): ConditionCallBuilder =
	continueOnFailure().requirements("OIDCC-10.2", "OIDCC-10.2.1").condition(ValidateEncryptedRequestObjectHasKid::class.java)
		.skipIfElementMissing("authorization_request_object", "jwe_header").onSkip(ConditionResult.INFO)

/** The checks on the signed request object pushed to the PAR endpoint. */
private fun requestObjectChecks(openid: Boolean): TestExecutionUnit =
	Steps.of(
		"Request object",
		stopOnFailure().requirements("FAPI2-SP-FINAL-5.4").condition(FAPI2ValidateRequestObjectSigningAlg::class.java),
		continueOnFailure().onFail(ConditionResult.WARNING).requirements("JAR-4").condition(FAPIValidateRequestObjectMediaType::class.java),
		if (openid) {
			continueOnFailure().onFail(ConditionResult.INFO).requirements("OIDCC-5.5.1.1").condition(FAPIValidateRequestObjectIdTokenACRClaims::class.java)
		} else Steps.NONE,
		stopOnFailure().requirements("RFC7519-4.1.4", "FAPI2-MS-ID1-5.3.1-4").condition(FAPIValidateRequestObjectExp::class.java),
		continueOnFailure().requirements("FAPI2-MS-ID1-5.3.1-3").condition(FAPI1AdvancedValidateRequestObjectNBFClaim::class.java),
		condition(ValidateRequestObjectClaims::class.java),
		stopOnFailure().requirements("OIDCC-13.3").condition(ValidateRequestObjectMaxAge::class.java),
		continueOnFailure().onFail(ConditionResult.WARNING).requirements("OIDCC-13.3").condition(EnsureNumericRequestObjectClaimsAreNotNull::class.java),
		continueOnFailure().requirements("OIDCC-6.1").condition(EnsureRequestObjectDoesNotContainRequestOrRequestUri::class.java),
		continueOnFailure().requirements("JAR-10.8").condition(EnsureRequestObjectDoesNotContainSubWithClientId::class.java),
		stopOnFailure().requirements("FAPI2-MS-ID1-5.3.1-1").condition(ValidateRequestObjectSignature::class.java),
		continueOnFailure().condition(EnsureMatchingRedirectUriInRequestObject::class.java)
	)

/** The authorization request, by reference to the pushed request, answered with a redirect carrying the code. */
private fun authorizationEndpoint(openid: Boolean): TestExecutionUnit =
	Steps.block(
		"Authorization endpoint",
		Endpoint.requestAs(
			"authorization_endpoint_http_request",
			Steps.check("Extract authorization request parameters") { env, c ->
				val method = env.getString("authorization_endpoint_http_request", "method")
				val params = when (method) {
					"POST" -> "body_form_params"
					"GET" -> "query_string_params"
					else -> throw c.error("Got unexpected HTTP method to authorization endpoint", mapOf("method" to method))
				}
				env.putObject(
					"authorization_endpoint_http_request_params",
					env.getObject("authorization_endpoint_http_request").getAsJsonObject(params)
				)
			},
			condition(EnsureAuthorizationRequestDoesNotContainRequestWhenUsingPAR::class.java),
			continueOnFailure().requirements("PAR-4", "FAPI2-SP-FINAL-5.3.3.2-6")
				.condition(EnsureAuthorizationRequestContainsOnlyExpectedParamsWhenUsingPAR::class.java),
			encryptedRequestObjectKidCheck(),
			condition(CreateEffectiveAuthorizationPARRequestParameters::class.java),
			stopOnFailure().requirements("RFC9396-2").condition(RARSupport.EnsureEffectiveAuthorizationEndpointRequestContainsValidRAR::class.java),
			stopOnFailure().requirements("FAPI2-SP-FINAL-5.3.2.2-1").condition(EnsureResponseTypeIsCode::class.java),
			continueOnFailure().onFail(ConditionResult.WARNING)
				.requirements(
					"RFC6749-4.1.1", "OIDCC-3.1.2.1", "RFC7636-4.3", "OAuth2-RT-2.1", "RFC7519-4.1", "DPOP-10",
					"RFC8485-4.1", "RFC8707-2.1", "RFC9396-2"
				)
				.condition(CheckForUnexpectedClaimsInRequestObject::class.java)
				.skipIfElementMissing("authorization_request_object", "claims").onSkip(ConditionResult.INFO),
			if (openid) claimsParameterChecks() else continueOnFailure().condition(EnsureClaimsParameterNotPresentInPlainOAuthRequest::class.java),
			stopOnFailure().requirements("FAPI2-SP-FINAL-5.3.3.2-3").condition(EnsureAuthorizationRequestContainsPkceCodeChallenge::class.java),
			continueOnFailure().requirements("FAPI2-MS-ID1-5.3.2-1")
				.condition(EnsureClientIdInAuthorizationRequestParametersMatchRequestObject::class.java),
			condition(ExtractRequestedScopes::class.java),
			condition(EnsureRequestedScopeIsEqualToConfiguredScope::class.java),
			if (openid) condition(EnsureOpenIDInScopeRequest::class.java) else Steps.NONE,
			stopOnFailure().requirements("OIDCC-3.1.2.1").condition(EnsureMatchingClientId::class.java),
			condition(CreateAuthorizationCode::class.java),
			if (openid) {
				continueOnFailure().requirements("OIDCC-3.2.2.1").condition(ExtractNonceFromAuthorizationRequest::class.java)
					.skipIfElementMissing(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, CreateEffectiveAuthorizationRequestParameters.NONCE)
					.onSkip(ConditionResult.INFO)
			} else Steps.`when`(
				"openid scope was requested",
				::openidScopeRequested,
				Steps.check("Ensure openid scope is not requested") { _, c -> throw c.error("openid scope cannot be used with PLAIN_OAUTH") },
				continueOnFailure().requirements("RFC6749-4.1.1").condition(EnsureAuthorizationRequestContainsStateParameter::class.java)
					.skipIfElementMissing(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, CreateEffectiveAuthorizationRequestParameters.STATE)
					.onSkip(ConditionResult.INFO)
			),
			condition(CreateAuthorizationEndpointResponseParams::class.java),
			stopOnFailure().requirements("OIDCC-3.3.2.5").condition(AddCodeToAuthorizationEndpointResponseParams::class.java),
			stopOnFailure().requirements("FAPI2-SP-FINAL-5.3.2.2-7").condition(AddIssToAuthorizationEndpointResponseParams::class.java),
			stopOnFailure().requirements("OIDCC-3.1.2.5").condition(SendAuthorizationResponseWithResponseModeQuery::class.java),
			Steps.expose("authorization_endpoint_response_redirect")
		)
	)

private fun claimsParameterChecks(): TestExecutionUnit {
	fun ifClaimsRequested(check: ConditionCallBuilder): ConditionCallBuilder =
		check.skipIfElementMissing(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, "claims").onSkip(ConditionResult.INFO)

	return Steps.of(
		"Claims parameter",
		ifClaimsRequested(continueOnFailure().onFail(ConditionResult.WARNING).requirements("OIDCC-5.5")
			.condition(CheckForUnexpectedClaimsInClaimsParameter::class.java)),
		ifClaimsRequested(continueOnFailure().onFail(ConditionResult.WARNING).requirements("OIDCC-5.1", "OIDCC-5.5.1.1")
			.condition(CheckForUnexpectedOpenIdClaims::class.java)),
		ifClaimsRequested(continueOnFailure().requirements("OIDCC-5.5").condition(CheckRequestClaimsParameterValues::class.java)),
		ifClaimsRequested(continueOnFailure().requirements("OIDCC-5.5.1").condition(CheckRequestClaimsParameterMemberValues::class.java))
	)
}

private fun tokenEndpoint(): TestExecutionUnit =
	Steps.block(
		"Token endpoint",
		Endpoint.requestAs(
			"token_endpoint_request",
			clientAuthenticationSteps("RFC8705-2", emptyClientIdRequirement = "RFC6749-3.2"),
			Steps.check("Check grant type") { env, c ->
				when (val grantType = env.getString("token_endpoint_request", "body_form_params.grant_type")) {
					null -> throw c.error("Token endpoint body does not contain the mandatory 'grant_type' parameter")
					"authorization_code", "refresh_token" -> {}
					else -> throw c.error("Got an unexpected grant type on the token endpoint", mapOf("grant_type" to grantType))
				}
			},
			Steps.`when`(
				"grant type is authorization_code",
				{ env -> env.getString("token_endpoint_request", "body_form_params.grant_type") == "authorization_code" },
				authorizationCodeGrant(),
				refreshTokenGrant()
			)
		)
	)

/** A certificate-bound access token; the at_hash is calculated in case an id token is issued with it. */
private fun issueAccessToken(): TestExecutionUnit =
	Steps.of(
		"Issue access token",
		condition(GenerateBearerAccessToken::class.java),
		continueOnFailure().onFail(ConditionResult.INFO).condition(GenerateAccessTokenExpiration::class.java),
		stopOnFailure().requirements("OIDCC-3.3.2.11").condition(CalculateAtHash::class.java)
	)

private fun authorizationCodeGrant(): TestExecutionUnit =
	Steps.of(
		"Authorization code grant",
		condition(ValidateAuthorizationCode::class.java),
		continueOnFailure().condition(ValidateRedirectUri::class.java),
		CheckPkceCodeVerifier(),
		issueAccessToken(),
		condition(CreateRefreshToken::class.java),
		Steps.`when`("openid scope was requested", ::openidScopeRequested, issueIdToken()),
		condition(CreateTokenEndpointResponse::class.java),
		condition(RARSupport.AddRarToTokenEndpointResponse::class.java),
		stopOnFailure().requirements("GM-5.5").condition(GrantManagementSupport.AddGrantIdToTokenEndpointResponse::class.java)
	)

/** The id token returned from the token endpoint, so without c_hash and s_hash. */
private fun issueIdToken(): TestExecutionUnit =
	Steps.of(
		"Issue id token",
		condition(GenerateIdTokenClaims::class.java),
		continueOnFailure().requirements("OIDCC-3.3.2.11").condition(AddAtHashToIdTokenClaims::class.java)
			.skipIfStringMissing("at_hash").onSkip(ConditionResult.INFO),
		continueOnFailure().requirements("OIDCC-3.1.3.7-12").condition(AddACRClaimToIdTokenClaims::class.java)
			.skipIfStringMissing("requested_id_token_acr_values").onSkip(ConditionResult.INFO),
		condition(SignIdToken::class.java)
	)

/** Refresh tokens are rotated, and the response carries no id token. */
private fun refreshTokenGrant(): TestExecutionUnit =
	Steps.of(
		"Refresh token grant",
		condition(ValidateRefreshToken::class.java),
		issueAccessToken(),
		condition(CreateRefreshToken::class.java),
		Steps.action("Forget the id token") { env -> env.removeNativeValue("id_token") },
		condition(CreateTokenEndpointResponse::class.java)
	)

/** The checks on a request to a resource endpoint: a certificate-bound access token and the FAPI headers. */
private fun resourceRequestChecks(): TestExecutionUnit =
	Steps.of(
		"Resource request",
		Endpoint.requestAs("token_endpoint_request", mtlsCertificateChecks()),
		stopOnFailure().requirements("FAPI2-SP-FINAL-5.3.4-2").of(
			"Access token",
			EnsureBearerAccessTokenNotInParams::class.java,
			ExtractBearerAccessTokenFromHeader::class.java
		),
		RequireMtlsAccessToken(),
		// optional headers, so only checked when present
		continueOnFailure().requirements("FAPI1-BASE-6.2.2-3").condition(ExtractFapiDateHeader::class.java)
			.skipIfElementMissing("incoming_request", "headers.x-fapi-auth-date").onSkip(ConditionResult.INFO),
		continueOnFailure().requirements("FAPI1-BASE-6.2.2-4").condition(ExtractFapiIpAddressHeader::class.java)
			.skipIfElementMissing("incoming_request", "headers.x-fapi-customer-ip-address").onSkip(ConditionResult.INFO),
		continueOnFailure().requirements("FAPI2-IMP-2.1.1").condition(ExtractFapiInteractionIdHeader::class.java)
			.skipIfElementMissing("incoming_request", "headers.x-fapi-interaction-id").onSkip(ConditionResult.INFO),
		continueOnFailure().requirements("CID-SP-4.3-9", "FAPI2-IMP-2.1.1").condition(ValidateFAPIInteractionIdInResourceRequest::class.java)
	)

private fun userinfoEndpoint(): TestExecutionUnit =
	Steps.block(
		"Userinfo endpoint",
		Endpoint.requestAs(
			"incoming_request",
			resourceRequestChecks(),
			condition(FilterUserInfoForScopes::class.java),
			condition(ClearAccessTokenFromRequest::class.java)
		)
	)

private fun accountsEndpoint(): TestExecutionUnit =
	Steps.block(
		"Accounts endpoint",
		Endpoint.requestAs(
			"incoming_request",
			resourceRequestChecks(),
			stopOnFailure().requirements("FAPI2-IMP-2.1.1").condition(CreateFapiInteractionIdIfNeeded::class.java),
			condition(CreateFAPIAccountEndpointResponse::class.java),
			condition(ClearAccessTokenFromRequest::class.java)
		)
	)

/**
 * The emulated grant management API (GM 6), a protected resource like the accounts endpoint: GET returns the grant
 * (GM 6.4), DELETE revokes it (GM 6.5), and a query of a revoked grant returns 404 (GM 6.6). The response is stored
 * under [GrantManagementSupport.CreateGrantManagementQueryResponse.RESPONSE_KEY], with its status under that key
 * suffixed with [HTTP_STATUS_SUFFIX].
 */
private fun grantManagementEndpoint(): TestExecutionUnit =
	Steps.block(
		"Grant management endpoint",
		Endpoint.requestAs(
			"incoming_request",
			Steps.check("Extract grant_id from the grant management request") { env, c ->
				val grantId = env.getString("incoming_request", "request_url").substringAfter("/$GRANTS_PATH", "")
				env.putString(GrantManagementSupport.EnsureGrantManagementRequestIsForIssuedGrant.REQUESTED_GRANT_ID, grantId)
				c.logSuccess("Extracted the grant_id from the grant management request path", mapOf("grant_id" to grantId))
			},
			resourceRequestChecks(),
			continueOnFailure().requirements("GM-6.3").condition(GrantManagementSupport.EnsureGrantManagementRequestIsForIssuedGrant::class.java),
			// neither the query nor the revoke request carries a body or query parameters
			continueOnFailure().requirements("GM-6.4", "GM-6.5").condition(EnsureIncomingRequestBodyIsEmpty::class.java),
			continueOnFailure().requirements("GM-6.4", "GM-6.5").condition(EnsureIncomingUrlQueryIsEmpty::class.java),
			Steps.`when`(
				"the client revokes the grant",
				{ env -> env.getString("incoming_request", "method") == "DELETE" },
				Steps.action("Revoke the grant") { env ->
					env.putInteger(GRANT_REVOCATIONS, (env.getInteger(GRANT_REVOCATIONS) ?: 0) + 1)
					env.storeResponseStatus(GRANT_MANAGEMENT_RESPONSE, 204)
				},
				Steps.of(
					null,
					continueOnFailure().requirements("GM-6.4", "GM-6.5").condition(EnsureIncomingRequestMethodIsGet::class.java),
					Steps.`when`(
						"the grant has been revoked",
						{ env -> env.getInteger(GRANT_REVOCATIONS) != null },
						Steps.action("Answer that the grant does not exist") { env -> env.storeResponseStatus(GRANT_MANAGEMENT_RESPONSE, 404) },
						Steps.of(
							null,
							stopOnFailure().requirements("GM-6.4").condition(GrantManagementSupport.CreateGrantManagementQueryResponse::class.java),
							Steps.action("Record the query") { env ->
								env.putBoolean(GRANT_QUERIED, true)
								env.putInteger(GRANT_MANAGEMENT_RESPONSE + HTTP_STATUS_SUFFIX, 200)
							}
						)
					)
				)
			),
			condition(ClearAccessTokenFromRequest::class.java)
		)
	)

/** The request being served is the client's first revocation of its grant. */
val firstGrantRevocation: Predicate<Environment> = Predicate { env ->
	env.getString(Endpoint.CURRENT_REQUEST, "method") == "DELETE" && env.getInteger(GRANT_REVOCATIONS) == 1
}

/** A response under [key] with [status] and no body, replacing any body an earlier request stored there. */
internal fun Environment.storeResponseStatus(key: String, status: Int) {
	removeObject(key)
	removeObject(key + "_headers")
	putInteger(key + HTTP_STATUS_SUFFIX, status)
}

/** The object stored under [key], for building a response from it. */
internal fun Environment.responseBody(key: String): JsonObject =
	getObject(key) ?: throw IllegalStateException("$key is missing from the environment")
