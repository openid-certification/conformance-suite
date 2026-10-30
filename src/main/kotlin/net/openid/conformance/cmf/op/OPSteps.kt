package net.openid.conformance.cmf.op

import com.google.gson.JsonObject
import net.openid.conformance.condition.Condition.ConditionResult
import net.openid.conformance.condition.`as`.FAPI2FinalEnsureMinimumClientKeyLength
import net.openid.conformance.condition.`as`.FAPI2FinalEnsureMinimumServerKeyLength
import net.openid.conformance.condition.client.*
import net.openid.conformance.condition.common.CheckDistinctKeyIdValueInClientJWKs
import net.openid.conformance.condition.common.CheckForBCP195InsecureFAPICiphers
import net.openid.conformance.condition.common.CheckForKeyIdInClientJWKs
import net.openid.conformance.condition.common.CheckForKeyIdInServerJWKs
import net.openid.conformance.condition.common.CheckServerConfiguration
import net.openid.conformance.condition.common.DisallowTLS10
import net.openid.conformance.condition.common.DisallowTLS11
import net.openid.conformance.condition.common.EnsureTLS12RequireBCP195Ciphers
import net.openid.conformance.condition.common.EnsureTLS13OrLater
import net.openid.conformance.condition.common.EnsureTLS13PreferredOverTLS12
import net.openid.conformance.condition.common.FAPI2CheckKeyAlgInClientJWKs
import net.openid.conformance.condition.common.GrantManagementSupport
import net.openid.conformance.condition.common.RARSupport
import net.openid.conformance.condition.common.RequireOnlyBCP195RecommendedCiphersForTLS12
import net.openid.conformance.sequence.Steps
import net.openid.conformance.sequence.Steps.condition
import net.openid.conformance.sequence.Steps.continueOnFailure
import net.openid.conformance.sequence.Steps.stopOnFailure
import net.openid.conformance.sequence.ValidateJwksSequence
import net.openid.conformance.sequence.client.PerformStandardIdTokenChecks
import net.openid.conformance.testmodule.ConditionCallBuilder
import net.openid.conformance.testmodule.Environment
import net.openid.conformance.testmodule.TestExecutionUnit

/*
 * The steps of the FAPI2 Message Signing authorization code flow as the Chile Open Finance (CMF) profile fixes it:
 * tls_client_auth, mTLS sender constrained tokens, PAR with a signed request object, RAR, plain (non-JARM)
 * responses and grant management. Only openid is left to the tester.
 *
 * Each function returns an immutable unit. Modules change a flow with Steps.modify(...) on the unit returned here,
 * targeting conditions by class and actions by the names below.
 */

/** Which of the two configured clients a flow runs as. */
enum class OPClient(val logPrefix: String) {
	FIRST(""),
	SECOND("Second client: ");

	val isSecond: Boolean get() = this == SECOND
}

// Action names, so modules can target them with Steps.modify
const val SET_REQUESTED_STATE_LENGTH = "Set requested state length"
const val SET_REQUESTED_NONCE_LENGTH = "Set requested nonce length"

internal fun parAccepted(env: Environment): Boolean {
	val status = env.getInteger(CallPAREndpoint.RESPONSE_KEY, "status")
	return status != null && status in 200..299
}

internal fun hasAuthorizationError(env: Environment): Boolean =
	env.getObject("authorization_endpoint_response")?.has("error") == true

internal fun putInteger(name: String, key: String, value: Int): TestExecutionUnit =
	Steps.action(name) { env -> env.putInteger(key, value) }

internal fun mapKey(name: String, from: String, to: String): TestExecutionUnit =
	Steps.action(name) { env -> env.mapKey(from, to) }

internal fun unmapKey(name: String, key: String): TestExecutionUnit =
	Steps.action(name) { env -> env.unmapKey(key) }

/** Runs [conditions] against the response stored under [responseKey] by mapping it to endpoint_response. */
internal fun onEndpointResponse(responseKey: String, vararg conditions: TestExecutionUnit): TestExecutionUnit =
	Steps.of(
		null,
		mapKey("Map $responseKey to endpoint_response", "endpoint_response", responseKey),
		Steps.of(null, *conditions),
		unmapKey("Unmap endpoint_response from $responseKey", "endpoint_response")
	)

/** Runs tls_client_auth client authentication on the given request, which for mTLS is just client_id. */
internal fun mtlsClientAuthentication(formParamsKey: String, headersKey: String): TestExecutionUnit =
	Steps.of(
		null,
		Steps.action("Map client authentication to $formParamsKey") { env ->
			if (env.getObject(headersKey) == null) {
				env.putObject(headersKey, JsonObject())
			}
			if (env.getObject(formParamsKey) == null) {
				env.putObject(formParamsKey, JsonObject())
			}
			env.mapKey("request_form_parameters", formParamsKey)
			env.mapKey("request_headers", headersKey)
		},
		condition(AddClientIdToRequest::class.java),
		Steps.action("Unmap client authentication from $formParamsKey") { env ->
			env.unmapKey("request_form_parameters")
			env.unmapKey("request_headers")
		}
	)

internal fun switchToSecondClient(): TestExecutionUnit =
	Steps.action("Switch to second client") { env ->
		env.mapKey("client", "client2")
		env.mapKey("client_jwks", "client_jwks2")
		env.mapKey("mutual_tls_authentication", "mutual_tls_authentication2")
	}

internal fun switchToFirstClient(): TestExecutionUnit =
	Steps.action("Switch to first client") { env ->
		env.unmapKey("client")
		env.unmapKey("client_jwks")
		env.unmapKey("mutual_tls_authentication")
	}

// ---------------------------------------------------------------------------------------------------------------
// Configuration
// ---------------------------------------------------------------------------------------------------------------

internal fun serverConfigurationSteps(openid: Boolean): TestExecutionUnit =
	Steps.of(
		"Server configuration",
		condition(CreateRedirectUri::class.java),
		if (openid) condition(GetDynamicServerConfiguration::class.java) else condition(GetOauthDynamicServerConfiguration::class.java),
		continueOnFailure().requirements("RFC8705-5").condition(AddMTLSEndpointAliasesToEnvironment::class.java),
		condition(CheckServerConfiguration::class.java),
		if (openid) serverKeySteps() else Steps.NONE,
		continueOnFailure().condition(RARSupport.ExtractRARFromConfig::class.java)
	)

private fun serverKeySteps(): TestExecutionUnit =
	Steps.of(
		"Server keys",
		condition(FetchServerKeys::class.java),
		continueOnFailure().onFail(ConditionResult.WARNING).condition(CheckServerKeysIsValid::class.java),
		ValidateJwksSequence("server_jwks", null, "server JWKS", "RFC7517-1.1"),
		continueOnFailure().requirements("OIDCC-10.1").condition(CheckForKeyIdInServerJWKs::class.java),
		continueOnFailure().requirements("FAPI2-SP-FINAL-5.4.1-2", "FAPI2-SP-FINAL-5.4.1-3")
			.condition(FAPI2FinalEnsureMinimumServerKeyLength::class.java)
	)

internal fun firstClientConfigurationSteps(): TestExecutionUnit =
	Steps.of(
		"Client configuration",
		condition(GetStaticClientConfiguration::class.java),
		continueOnFailure().onFail(ConditionResult.WARNING).condition(ValidateMTLSCertificatesHeader::class.java),
		continueOnFailure().condition(ExtractMTLSCertificatesFromConfiguration::class.java),
		clientValidationSteps()
	)

internal fun secondClientConfigurationSteps(): TestExecutionUnit =
	Steps.block(
		"Verify configuration of second client",
		switchToSecondClient(),
		condition(GetStaticClient2Configuration::class.java),
		continueOnFailure().onFail(ConditionResult.WARNING).condition(ValidateMTLSCertificates2Header::class.java),
		continueOnFailure().condition(ExtractMTLSCertificates2FromConfiguration::class.java),
		clientValidationSteps(),
		switchToFirstClient(),
		continueOnFailure().condition(ValidateClientPrivateKeysAreDifferent::class.java)
	)

private fun clientValidationSteps(): TestExecutionUnit =
	Steps.of(
		"Validate client configuration",
		stopOnFailure().requirements("RFC7517-1.1").condition(ValidateClientJWKsPrivatePart::class.java),
		condition(ExtractJWKsFromStaticClientConfiguration::class.java),
		stopOnFailure().requirements("OIDCC-10.1").condition(CheckForKeyIdInClientJWKs::class.java),
		continueOnFailure().requirements("RFC7517-4.5").condition(CheckDistinctKeyIdValueInClientJWKs::class.java),
		continueOnFailure().requirements("FAPI2-SP-FINAL-5.4").condition(FAPI2CheckKeyAlgInClientJWKs::class.java),
		continueOnFailure().requirements("FAPI2-SP-FINAL-5.4.1-2", "FAPI2-SP-FINAL-5.4.1-3")
			.condition(FAPI2FinalEnsureMinimumClientKeyLength::class.java),
		continueOnFailure().condition(ValidateMTLSCertificatesAsX509::class.java)
	)

internal fun resourceConfigurationSteps(): TestExecutionUnit =
	stopOnFailure().of(
		"Resource configuration",
		GetResourceEndpointConfiguration::class.java,
		SetProtectedResourceUrlToSingleResourceEndpoint::class.java
	)

// ---------------------------------------------------------------------------------------------------------------
// Authorization flow
// ---------------------------------------------------------------------------------------------------------------

/**
 * The steps of one authorization code flow for one client. Every property is an independent unit, so a module
 * can modify one part and reassemble the rest; [beforeRedirect] and [afterCallback] assemble the standard flow.
 *
 * @param needsGrantManagementApiScopes request the GM 6.1 scopes; only modules that call the grant management
 * endpoint need them, and requesting them elsewhere changes what the other modules exercise
 */
class OPFlow(
	val client: OPClient,
	val openid: Boolean,
	needsGrantManagementApiScopes: Boolean = false
) {

	val createAuthorizationRequest: TestExecutionUnit = Steps.block(
		client.logPrefix + "Create authorization request",
		condition(CreateAuthorizationEndpointRequestFromClientInformation::class.java),
		Steps.action(SET_REQUESTED_STATE_LENGTH) { env ->
			if (client.isSecond) env.putInteger("requested_state_length", 128) else env.removeNativeValue("requested_state_length")
		},
		condition(CreateRandomStateValue::class.java),
		condition(AddStateToAuthorizationEndpointRequest::class.java),
		if (openid) Steps.of(
			"Nonce",
			Steps.action(SET_REQUESTED_NONCE_LENGTH) { env -> env.removeNativeValue("requested_nonce_length") },
			condition(CreateRandomNonceValue::class.java),
			condition(AddNonceToAuthorizationEndpointRequest::class.java)
		) else Steps.NONE,
		condition(SetAuthorizationEndpointRequestResponseTypeToCode::class.java),
		stopOnFailure().requirements("RFC7636-4.1").condition(CreateRandomCodeVerifier::class.java),
		stopOnFailure().requirements("RFC7636-4.2").condition(CreateS256CodeChallenge::class.java),
		stopOnFailure().requirements("RFC7636-4.3").condition(AddCodeChallengeToAuthorizationEndpointRequest::class.java),
		if (needsGrantManagementApiScopes) {
			stopOnFailure().requirements("GM-6.1")
				.condition(GrantManagementSupport.AddGrantManagementScopesToAuthorizationRequest::class.java)
		} else Steps.NONE,
		stopOnFailure().requirements("GM-5.2")
			.condition(GrantManagementSupport.AddGrantManagementActionCreateToAuthorizationRequest::class.java),
		condition(RARSupport.AddRARToAuthorizationEndpointRequest::class.java)
	)

	val createRequestObject: TestExecutionUnit = Steps.block(
		client.logPrefix + "Create request object",
		condition(ConvertAuthorizationEndpointRequestToRequestObject::class.java),
		if (client.isSecond) condition(AddIatToRequestObject::class.java) else Steps.NONE,
		stopOnFailure().requirements("FAPI2-MS-ID1-5.3.1-3").condition(AddNbfToRequestObject::class.java),
		stopOnFailure().requirements("FAPI2-MS-ID1-5.3.1-4").condition(AddExpToRequestObject::class.java),
		stopOnFailure().requirements("FAPI2-SP-FINAL-5.3.2.1-6").condition(AddAudToRequestObject::class.java),
		stopOnFailure().requirements("OIDCC-6.1").condition(AddIssToRequestObject::class.java),
		stopOnFailure().requirements("JAR-5", "FAPI2-MS-ID1-5.3.2-1").condition(AddClientIdToRequestObject::class.java),
		if (client.isSecond) {
			stopOnFailure().requirements("JAR-4").condition(SignRequestObjectIncludeMediaType::class.java)
		} else {
			condition(SignRequestObject::class.java)
		}
	)

	val pushAuthorizationRequest: TestExecutionUnit = pushAuthorizationRequest(signed = true)

	/**
	 * The PAR request. Unsigned requests send the authorization request parameters directly, so the implicit
	 * request object the redirect builder reads is the form body.
	 */
	fun pushAuthorizationRequest(signed: Boolean): TestExecutionUnit = Steps.block(
		client.logPrefix + "Make request to PAR endpoint",
		if (signed) {
			condition(BuildRequestObjectPostToPAREndpoint::class.java)
		} else {
			Steps.of(
				null,
				mapKey("Use PAR form as request object claims", "request_object_claims", "pushed_authorization_request_form_parameters"),
				condition(BuildUnsignedPAREndpointRequest::class.java)
			)
		},
		mtlsClientAuthentication("pushed_authorization_request_form_parameters", "pushed_authorization_request_endpoint_request_headers"),
		stopOnFailure().requirements("PAR-2.1").condition(CallPAREndpoint::class.java)
	)

	val processParResponse: TestExecutionUnit = Steps.of(
		"Process PAR response",
		stopOnFailure().requirements("PAR-2.2", "PAR-2.3", "PAR-2.4").condition(CheckPAREndpointResponse201WithNoError::class.java),
		stopOnFailure().requirements("PAR-2.2").condition(CheckForRequestUriValue::class.java),
		continueOnFailure().requirements("PAR-2.2").condition(CheckForPARResponseExpiresIn::class.java),
		onEndpointResponse(CallPAREndpoint.RESPONSE_KEY, continueOnFailure().condition(EnsureContentTypeJson::class.java)),
		condition(ExtractRequestUriFromPARResponse::class.java),
		continueOnFailure().requirements("PAR-2.2", "PAR-7.1", "JAR-10.2").condition(EnsureMinimumRequestUriEntropy::class.java)
	)

	val buildAuthorizationRedirect: TestExecutionUnit = Steps.block(
		client.logPrefix + "Make request to authorization endpoint",
		stopOnFailure().requirements("PAR-4").condition(BuildRequestObjectByReferenceRedirectToAuthorizationEndpoint::class.java)
	)

	/** FAPI2 always uses the code flow with plain responses, so the response is the callback query. */
	val useCallbackQueryAsAuthorizationResponse: TestExecutionUnit =
		mapKey("Use callback query as authorization response", "authorization_endpoint_response", "callback_query_params")

	/** The callback checks every module runs before looking at the response itself. */
	val rejectResponseInFragment: TestExecutionUnit = Steps.of(
		"Reject response in fragment",
		continueOnFailure().requirements("OAuth2-RT-5").condition(RejectErrorInUrlFragment::class.java),
		continueOnFailure().requirements("OIDCC-3.3.2.5").condition(RejectAuthCodeInUrlFragment::class.java)
	)

	val verifySuccessfulAuthorizationResponse: TestExecutionUnit = Steps.of(
		"Verify successful authorization response",
		continueOnFailure().condition(CheckMatchingCallbackParameters::class.java),
		continueOnFailure().requirements("OIDCC-3.3.2.5").condition(RejectStateInUrlFragmentForCodeFlow::class.java),
		condition(CheckIfAuthorizationEndpointError::class.java),
		continueOnFailure().onFail(ConditionResult.WARNING)
			.condition(ValidateSuccessfulAuthCodeFlowResponseFromAuthorizationEndpoint::class.java),
		continueOnFailure().requirements("OIDCC-3.2.2.5").condition(CheckStateInAuthorizationResponse::class.java),
		continueOnFailure().requirements("OAuth2-iss-2", "FAPI2-SP-FINAL-5.3.2.2-7").condition(RequireIssInAuthorizationResponse::class.java),
		authorizationCodeSteps()
	)

	val callTokenEndpoint: TestExecutionUnit = Steps.block(
		client.logPrefix + "Call token endpoint",
		condition(CreateTokenEndpointRequestForAuthorizationCodeGrant::class.java),
		stopOnFailure().requirements("RFC7636-4.5", "FAPI2-SP-FINAL-5.3.3.2-3").condition(AddCodeVerifierToTokenEndpointRequest::class.java),
		mtlsClientAuthentication("token_endpoint_request_form_parameters", "token_endpoint_request_headers"),
		condition(CallTokenEndpointAndReturnFullResponse::class.java)
	)

	val verifyTokenEndpointResponse: TestExecutionUnit = Steps.block(
		client.logPrefix + "Verify token endpoint response",
		condition(CheckTokenEndpointHttpStatus200::class.java),
		condition(CheckIfTokenEndpointResponseError::class.java),
		stopOnFailure().requirements("RFC6749-4.1.4").condition(CheckForAccessTokenValue::class.java),
		condition(ExtractAccessTokenFromTokenResponse::class.java),
		continueOnFailure().onFail(ConditionResult.WARNING).requirements("RFC6749-5.1")
			.condition(ExtractExpiresInFromTokenEndpointResponse::class.java),
		continueOnFailure().requirements("RFC6749-5.1").condition(ValidateExpiresIn::class.java)
			.skipIfObjectMissing("expires_in").onSkip(ConditionResult.INFO),
		continueOnFailure().onFail(ConditionResult.INFO).condition(CheckForRefreshTokenValue::class.java),
		// under grant management the granted scope routinely differs from the configured one (RFC6749-6)
		continueOnFailure().onFail(ConditionResult.INFO).requirements("RFC6749-6")
			.condition(ExtractGrantedScopeFromTokenEndpointResponse::class.java),
		continueOnFailure().requirements("RFC6749-10.10").condition(EnsureMinimumRefreshTokenLength::class.java)
			.skipIfElementMissing("token_endpoint_response", "refresh_token").onSkip(ConditionResult.INFO),
		continueOnFailure().requirements("RFC6749-10.10").condition(EnsureMinimumRefreshTokenEntropy::class.java)
			.skipIfElementMissing("token_endpoint_response", "refresh_token").onSkip(ConditionResult.INFO),
		continueOnFailure().requirements("FAPI2-SP-FINAL-5.4.1-4").of(
			"Access token strength",
			EnsureMinimumAccessTokenLength::class.java,
			EnsureMinimumAccessTokenEntropy::class.java
		),
		if (openid) idTokenSteps() else condition(ExpectNoIdTokenInTokenResponse::class.java),
		stopOnFailure().requirements("RFC9396-7").condition(RARSupport.CheckForAuthorizationDetailsInTokenResponse::class.java),
		stopOnFailure().requirements("GM-5.5").condition(GrantManagementSupport.ExtractGrantIdFromTokenResponse::class.java),
		// GM 5.5 requires "sufficient entropy" but gives no minimum length, so these can only warn
		continueOnFailure().onFail(ConditionResult.WARNING).requirements("GM-5.5").of(
			"Grant id",
			GrantManagementSupport.CheckGrantIdIsUrlSafe::class.java,
			GrantManagementSupport.CheckGrantIdHasSufficientEntropy::class.java
		)
	)

	val callResourceEndpoint: TestExecutionUnit = Steps.block(
		client.logPrefix + "Resource server endpoint tests",
		condition(CreateEmptyResourceEndpointRequestHeaders::class.java),
		if (client.isSecond) Steps.NONE else stopOnFailure().of(
			"Customer presence headers",
			AddFAPIAuthDateToResourceEndpointRequest::class.java,
			AddIpV4FapiCustomerIpAddressToResourceEndpointRequest::class.java,
			CreateRandomFAPIInteractionId::class.java,
			AddFAPIInteractionIdToResourceEndpointRequest::class.java
		),
		stopOnFailure().requirements("FAPI2-SP-FINAL-5.3.4-2").condition(CallProtectedResource::class.java),
		onEndpointResponse("resource_endpoint_response_full", continueOnFailure().condition(EnsureHttpStatusCodeIs200or201::class.java)),
		continueOnFailure().requirements("RFC7231-7.1.1.2").condition(CheckForDateHeaderInResourceResponse::class.java),
		if (client.isSecond) Steps.NONE else Steps.of(
			"Interaction id",
			continueOnFailure().requirements("CID-SP-4.2-12", "FAPI2-IMP-2.1.1")
				.condition(CheckForFAPIInteractionIdInResourceResponse::class.java)
				.skipIfElementMissing("resource_endpoint_response_headers", "x-fapi-interaction-id")
				.onSkip(ConditionResult.INFO),
			continueOnFailure().requirements("CID-SP-4.2-12", "FAPI2-IMP-2.1.1")
				.condition(EnsureMatchingFAPIInteractionId::class.java)
				.skipIfElementMissing("resource_endpoint_response_headers", "x-fapi-interaction-id")
				.onSkip(ConditionResult.INFO)
		)
	)

	/**
	 * Everything up to the redirect to the authorization endpoint.
	 *
	 * @param parRejected when set, the PAR endpoint may reject the request: these steps check the error response
	 * instead of continuing to the redirect. Without it a PAR error fails the test.
	 * @param signed whether the authorization request is sent as a signed request object
	 */
	fun beforeRedirect(parRejected: TestExecutionUnit? = null, signed: Boolean = true): TestExecutionUnit {
		val afterPar = Steps.of("Authorization redirect", processParResponse, buildAuthorizationRedirect)
		return Steps.of(
			"Authorization request",
			createAuthorizationRequest,
			if (signed) createRequestObject else Steps.NONE,
			if (signed) pushAuthorizationRequest else pushAuthorizationRequest(signed = false),
			if (parRejected == null) afterPar else Steps.`when`(
				"PAR endpoint accepted the request",
				{ parAccepted(it) },
				afterPar,
				Steps.block(client.logPrefix + "Verify PAR endpoint error response", parRejected)
			)
		)
	}

	/** Token endpoint and resource server calls after a successful authorization response. */
	fun postAuthorization(): TestExecutionUnit = Steps.of(
		"Post authorization",
		callTokenEndpoint,
		verifyTokenEndpointResponse,
		callResourceEndpoint
	)

	/**
	 * The callback handling through to the resource server.
	 *
	 * @param onError when set, the server may return an error instead: these steps check it and the flow ends
	 * there. Without it an error response fails the test.
	 * @param onSuccess the checks of a successful authorization response, before the token endpoint is called
	 */
	fun afterCallback(
		onError: TestExecutionUnit? = null,
		onSuccess: TestExecutionUnit = verifySuccessfulAuthorizationResponse,
		postAuthorization: TestExecutionUnit = postAuthorization()
	): TestExecutionUnit {
		val success = Steps.of(
			"Successful authorization response",
			Steps.block(client.logPrefix + "Verify authorization endpoint response", rejectResponseInFragment, onSuccess),
			postAuthorization
		)
		return Steps.of(
			"Authorization callback",
			useCallbackQueryAsAuthorizationResponse,
			if (onError == null) success else Steps.`when`(
				"authorization server returned an error",
				{ hasAuthorizationError(it) },
				Steps.block(client.logPrefix + "Verify authorization endpoint error response", rejectResponseInFragment, onError),
				success
			)
		)
	}

	/** The callback handling for a request the authorization server must reject. */
	fun errorCallback(checks: TestExecutionUnit): TestExecutionUnit = Steps.of(
		"Authorization error callback",
		useCallbackQueryAsAuthorizationResponse,
		Steps.block(client.logPrefix + "Verify authorization endpoint response", rejectResponseInFragment, checks)
	)

	private fun idTokenSteps(): TestExecutionUnit = Steps.of(
		"ID token",
		continueOnFailure().onFail(ConditionResult.WARNING).requirements("OIDCC-10.2")
			.condition(ValidateIdTokenFromTokenResponseEncryption::class.java)
			.skipIfObjectMissing("client_jwks").onSkip(ConditionResult.INFO),
		stopOnFailure().requirements("FAPI2-SP-FINAL-5.3.2.3", "OIDCC-3.3.2.5").condition(ExtractIdTokenFromTokenResponse::class.java),
		PerformStandardIdTokenChecks(),
		continueOnFailure().requirements("OIDCC-10.1").condition(EnsureIdTokenContainsKid::class.java),
		continueOnFailure().requirements("FAPI2-SP-FINAL-5.4").condition(FAPI2ValidateIdTokenSigningAlg::class.java),
		// code flow: all hashes are optional; the Extract*Hash conditions decide how strongly each is expected
		continueOnFailure().onFail(ConditionResult.INFO).requirements("OIDCC-3.3.2.11").condition(ExtractCHash::class.java),
		continueOnFailure().onFail(ConditionResult.INFO).requirements("FAPI1-ADV-5.2.2.1-5").condition(ExtractSHash::class.java),
		continueOnFailure().onFail(ConditionResult.INFO).requirements("OIDCC-3.3.2.11").condition(ExtractAtHash::class.java),
		continueOnFailure().requirements("OIDCC-3.3.2.11").condition(ValidateCHash::class.java)
			.skipIfObjectMissing("c_hash").onSkip(ConditionResult.INFO),
		continueOnFailure().requirements("FAPI1-ADV-5.2.2.1-5").condition(ValidateSHash::class.java)
			.skipIfObjectMissing("s_hash").onSkip(ConditionResult.INFO),
		continueOnFailure().requirements("OIDCC-3.3.2.11").condition(ValidateAtHash::class.java)
			.skipIfObjectMissing("at_hash").onSkip(ConditionResult.INFO)
	)
}

internal fun authorizationCodeSteps(): TestExecutionUnit = Steps.of(
	"Authorization code",
	condition(ExtractAuthorizationCodeFromAuthorizationResponse::class.java),
	continueOnFailure().requirements("RFC6749-10.10", "RFC6819-5.1.4.2-2").of(
		"Authorization code strength",
		EnsureMinimumAuthorizationCodeLength::class.java,
		EnsureMinimumAuthorizationCodeEntropy::class.java
	)
)

// ---------------------------------------------------------------------------------------------------------------
// Negative tests
// ---------------------------------------------------------------------------------------------------------------

/**
 * Creates the placeholder for the error page the authorization server may show instead of redirecting back.
 *
 * @param placeholderKey the environment string the expect condition stores the placeholder in
 */
internal fun expectErrorPage(
	expectCondition: ConditionCallBuilder,
	placeholderKey: String = "request_object_unverifiable_error"
): TestExecutionUnit = Steps.of(
	"Expect error page",
	expectCondition,
	Steps.action("Use $placeholderKey as error callback placeholder") { env ->
		env.putString("error_callback_placeholder", env.getString(placeholderKey))
	}
)

/**
 * The checks on an error returned to the redirect uri: it carries our state, it is an error, and it has no
 * unexpected parameters, then [errorCheck] decides whether it is the right error.
 */
internal fun authorizationErrorResponseSteps(
	vararg errorChecks: TestExecutionUnit,
	stateCheck: TestExecutionUnit = continueOnFailure().condition(CheckStateInAuthorizationResponse::class.java)
): TestExecutionUnit = Steps.of(
	"Verify authorization error response",
	stateCheck,
	continueOnFailure().requirements("OIDCC-3.1.2.6").condition(EnsureErrorFromAuthorizationEndpointResponse::class.java),
	continueOnFailure().onFail(ConditionResult.WARNING).requirements("OIDCC-3.1.2.6")
		.condition(CheckForUnexpectedParametersInErrorResponseFromAuthorizationEndpoint::class.java),
	*errorChecks
)

/**
 * The PAR error checks for a request object the server must reject.
 */
internal fun parInvalidRequestObject(vararg requirements: String): TestExecutionUnit =
	continueOnFailure().requirements(*requirements).condition(EnsurePARInvalidRequestObjectError::class.java)

// ---------------------------------------------------------------------------------------------------------------
// Second client
// ---------------------------------------------------------------------------------------------------------------

/**
 * Switch to the second client and give it a redirect uri with a query, which RFC6749 3.1.2 permits and the
 * server must preserve. config.disableRedirectQueryTest lets a tester whose client is not yet registered with the
 * new redirect uri run the test, recording a failure.
 */
internal fun secondClientSetupSteps(): TestExecutionUnit =
	Steps.block(
		OPClient.SECOND.logPrefix + "Setup",
		switchToSecondClient(),
		Steps.`when`(
			"redirect query test is disabled in the test configuration",
			{ env -> (env.getInteger("config", "disableRedirectQueryTest") ?: 0) != 0 },
			continueOnFailure().requirements("RFC6749-3.1.2").condition(RedirectQueryTestDisabled::class.java),
			stopOnFailure().requirements("RFC6749-3.1.2").condition(AddRedirectUriQuerySuffix::class.java)
		),
		stopOnFailure().requirements("RFC6749-3.1.2").condition(CreateRedirectUri::class.java)
	)

/**
 * RFC8705 3: a resource request presenting client 2's certificate-bound access token with client 1's certificate
 * must be rejected. Any 4xx is accepted and the WWW-Authenticate header is not checked.
 */
internal fun client2TokenWithClient1CertificateSteps(): TestExecutionUnit =
	Steps.of(
		"Client 2 token with client 1 certificate",
		switchToFirstClient(),
		Steps.block(
			"Try Client1's MTLS client certificate with Client2's access token",
			stopOnFailure().requirements("FAPIRW-5.2.2-5", "RFC8705-3").condition(CallProtectedResource::class.java),
			onEndpointResponse(
				"resource_endpoint_response_full",
				continueOnFailure().requirements("RFC6749-4.1.2", "RFC6750-3.1", "RFC8705-3").condition(EnsureHttpStatusCodeIs4xx::class.java)
			)
		)
	)

/** Only checks the state of an error response when the server returned one. */
internal fun stateCheckIfPresent(): TestExecutionUnit =
	continueOnFailure().condition(CheckStateInAuthorizationResponse::class.java)
		.skipIfElementMissing("authorization_endpoint_response", "state").onSkip(ConditionResult.INFO)

// ---------------------------------------------------------------------------------------------------------------
// TLS
// ---------------------------------------------------------------------------------------------------------------

/**
 * The TLS version and cipher checks of FAPI2 5.2 against the endpoint whose TLS test values are stored under
 * [tlsKey].
 *
 * @param optional the endpoint need not exist, so every check is skipped when it has no TLS test values
 * @param onlyRecommendedCiphers require only the BCP195 recommended TLS 1.2 ciphers; the authorization endpoint
 * is visited by browsers, so it may offer more
 * @param warnOnInsecureCiphers warn about the ciphers BCP195 calls insecure
 */
internal fun tlsEndpointSteps(
	label: String,
	tlsKey: String,
	optional: Boolean = false,
	onlyRecommendedCiphers: Boolean = true,
	warnOnInsecureCiphers: Boolean = false
): TestExecutionUnit {
	fun ifPresent(check: ConditionCallBuilder): ConditionCallBuilder =
		if (optional) check.skipIfObjectMissing("tls").onSkip(ConditionResult.INFO) else check

	val oldVersionRequirements = arrayOf("FAPI2-SP-FINAL-5.2.1-1", "FAPI2-SP-FINAL-5.2.1-2", "FAPI2-SP-FINAL-5.2.1-3")
	return Steps.block(
		label,
		mapKey("Use $tlsKey", "tls", tlsKey),
		ifPresent(continueOnFailure().requirements("FAPI2-SP-FINAL-5.2.3-2", "FAPI-ISSUES-847").condition(EnsureTLS12RequireBCP195Ciphers::class.java)),
		ifPresent(continueOnFailure().requirements(*oldVersionRequirements).condition(DisallowTLS10::class.java)),
		ifPresent(continueOnFailure().requirements(*oldVersionRequirements).condition(DisallowTLS11::class.java)),
		ifPresent(continueOnFailure().onFail(ConditionResult.WARNING).requirements("RFC9325-3.1.1").condition(EnsureTLS13OrLater::class.java)),
		continueOnFailure().requirements("RFC9325-3.1.1").condition(EnsureTLS13PreferredOverTLS12::class.java)
			.skipIfStringMissing("tls13_negotiated"),
		if (onlyRecommendedCiphers) {
			ifPresent(continueOnFailure().requirements("FAPI2-SP-FINAL-5.2.2", "FAPI-ISSUES-847")
				.condition(RequireOnlyBCP195RecommendedCiphersForTLS12::class.java))
		} else Steps.NONE,
		if (warnOnInsecureCiphers) {
			ifPresent(continueOnFailure().onFail(ConditionResult.WARNING).requirements("FAPI2-SP-FINAL-5.2.2", "RFC9325A-A", "RFC9325-4.2")
				.condition(CheckForBCP195InsecureFAPICiphers::class.java))
		} else Steps.NONE,
		unmapKey("Unmap $tlsKey", "tls")
	)
}

// ---------------------------------------------------------------------------------------------------------------
// Token endpoint errors
// ---------------------------------------------------------------------------------------------------------------

/**
 * The checks on an error response from the token endpoint (RFC6749 5.2): [statusCheck], a JSON body, the error
 * code checked by [errorCheck], and well-formed error members.
 */
internal fun tokenEndpointErrorSteps(statusCheck: TestExecutionUnit, errorCheck: TestExecutionUnit): TestExecutionUnit =
	Steps.of(
		"Token endpoint error response",
		statusCheck,
		continueOnFailure().requirements("OIDCC-3.1.3.4").condition(CheckTokenEndpointReturnedJsonContentType::class.java),
		errorCheck,
		continueOnFailure().requirements("RFC6749-5.2").condition(ValidateErrorFromTokenEndpointResponseError::class.java),
		continueOnFailure().onFail(ConditionResult.WARNING).requirements("RFC6749-5.2")
			.condition(CheckErrorDescriptionFromTokenEndpointResponseErrorContainsCRLFTAB::class.java),
		continueOnFailure().requirements("RFC6749-5.2").of(
			"Error description and uri",
			ValidateErrorDescriptionFromTokenEndpointResponseError::class.java,
			ValidateErrorUriFromTokenEndpointResponseError::class.java
		)
	)

/** The token endpoint must reject the request with HTTP 400 and invalid_grant. */
internal fun tokenEndpointInvalidGrantSteps(vararg errorRequirements: String = arrayOf("RFC6749-5.2")): TestExecutionUnit =
	tokenEndpointErrorSteps(
		continueOnFailure().requirements("OIDCC-3.1.3.4").condition(CheckTokenEndpointHttpStatus400::class.java),
		continueOnFailure().requirements(*errorRequirements).condition(CheckErrorFromTokenEndpointResponseErrorInvalidGrant::class.java)
	)

/**
 * The result of a token endpoint call made without the client's certificate, through
 * [CallTokenEndpointAllowingTLSFailure]. Under tls_client_auth the certificate both authenticates the client and
 * binds the tokens, so the server may drop the TLS connection or return invalid_client or invalid_request.
 *
 * @param httpStatusWarnOnly only warn when the status is not 400 or 401, to allow for a TLS terminator returning a
 * generic 4xx response
 */
internal fun tokenEndpointRejectedWithoutCertificateSteps(httpStatusWarnOnly: Boolean = false): TestExecutionUnit =
	Steps.`when`(
		"the TLS connection was not dropped",
		{ env -> env.getBoolean("token_endpoint_response_ssl_error") != true },
		Steps.of(
			"Token endpoint error response",
			if (httpStatusWarnOnly) Steps.of(
				null,
				onEndpointResponse(
					"token_endpoint_response_full",
					continueOnFailure().requirements("RFC6749-5.2").condition(EnsureHttpStatusCodeIs4xx::class.java)
				),
				continueOnFailure().onFail(ConditionResult.WARNING).requirements("RFC6749-5.2")
					.condition(CheckTokenEndpointHttpStatus400or401::class.java)
			) else continueOnFailure().requirements("RFC6749-5.2").condition(CheckTokenEndpointHttpStatus400or401::class.java),
			// only a warning, to allow for a TLS terminator returning a generic 400 response
			continueOnFailure().onFail(ConditionResult.WARNING).requirements("OIDCC-3.1.3.4")
				.condition(CheckTokenEndpointReturnedJsonContentType::class.java),
			Steps.`when`(
				"the token endpoint returned JSON",
				{ env -> env.getBoolean(CheckTokenEndpointReturnedJsonContentType.tokenEndpointResponseWasJsonKey) == true },
				Steps.of(
					"Token endpoint error",
					continueOnFailure().requirements("RFC6749-5.2").of(
						"Invalid client error",
						CheckTokenEndpointHttpStatusIs400Allowing401ForInvalidClientError::class.java,
						CheckErrorFromTokenEndpointResponseErrorInvalidClientOrInvalidRequest::class.java,
						ValidateErrorFromTokenEndpointResponseError::class.java
					),
					continueOnFailure().onFail(ConditionResult.WARNING).requirements("RFC6749-5.2")
						.condition(CheckErrorDescriptionFromTokenEndpointResponseErrorContainsCRLFTAB::class.java),
					continueOnFailure().requirements("RFC6749-5.2").of(
						"Error description and uri",
						ValidateErrorDescriptionFromTokenEndpointResponseError::class.java,
						ValidateErrorUriFromTokenEndpointResponseError::class.java
					)
				)
			)
		)
	)

/** Only checks the state of an authorization error response when the server returned one. */
internal fun authorizationErrorWithOptionalState(vararg errorChecks: TestExecutionUnit): TestExecutionUnit =
	authorizationErrorResponseSteps(*errorChecks, stateCheck = stateCheckIfPresent())
