package net.openid.conformance.openid.client;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.as.AddAtHashToIdTokenClaims;
import net.openid.conformance.condition.as.AddCHashToIdTokenClaims;
import net.openid.conformance.condition.as.AddCodeToAuthorizationEndpointResponseParams;
import net.openid.conformance.condition.as.AddIdTokenToAuthorizationEndpointResponseParams;
import net.openid.conformance.condition.as.AddTokenToAuthorizationEndpointResponseParams;
import net.openid.conformance.condition.as.CalculateAtHash;
import net.openid.conformance.condition.as.CalculateCHash;
import net.openid.conformance.condition.as.CreateAuthorizationCode;
import net.openid.conformance.condition.as.CreateAuthorizationEndpointResponseParams;
import net.openid.conformance.condition.as.CreateTokenEndpointResponse;
import net.openid.conformance.condition.as.EnsureClientDoesNotHaveBothJwksAndJwksUri;
import net.openid.conformance.condition.as.EnsureClientHasJwksOrJwksUri;
import net.openid.conformance.condition.as.EnsureClientJwksDoesNotContainPrivateOrSymmetricKeys;
import net.openid.conformance.condition.as.EnsureMatchingClientId;
import net.openid.conformance.condition.as.EnsureNumericRequestObjectClaimsAreNotNull;
import net.openid.conformance.condition.as.EnsureOpenIDInScopeRequest;
import net.openid.conformance.condition.as.EnsureRequestDoesNotContainRequestObject;
import net.openid.conformance.condition.as.EnsureRequestUriIsHttpsOrRequestObjectIsSigned;
import net.openid.conformance.condition.as.EnsureResponseTypeIsCode;
import net.openid.conformance.condition.as.EnsureResponseTypeIsCodeIdToken;
import net.openid.conformance.condition.as.EnsureResponseTypeIsCodeIdTokenToken;
import net.openid.conformance.condition.as.EnsureResponseTypeIsCodeToken;
import net.openid.conformance.condition.as.EnsureResponseTypeIsIdToken;
import net.openid.conformance.condition.as.EnsureResponseTypeIsIdTokenToken;
import net.openid.conformance.condition.as.EnsureValidRedirectUriForAuthorizationEndpointRequest;
import net.openid.conformance.condition.as.ExtractNonceFromAuthorizationRequest;
import net.openid.conformance.condition.as.ExtractRequestObject;
import net.openid.conformance.condition.as.ExtractRequestedScopes;
import net.openid.conformance.condition.as.ExtractServerSigningAlg;
import net.openid.conformance.condition.as.FetchClientKeys;
import net.openid.conformance.condition.as.FetchRequestUriAndExtractRequestObject;
import net.openid.conformance.condition.as.FilterUserInfoForScopes;
import net.openid.conformance.condition.as.GenerateBearerAccessToken;
import net.openid.conformance.condition.as.GenerateIdTokenClaims;
import net.openid.conformance.condition.as.CreateEffectiveAuthorizationRequestParameters;
import net.openid.conformance.condition.as.OIDCCEnsureAuthorizationHttpRequestContainsOpenIDScope;
import net.openid.conformance.condition.as.OIDCCEnsureOptionalAuthorizationRequestParametersMatchRequestObject;
import net.openid.conformance.condition.as.OIDCCEnsureRequiredAuthorizationRequestParametersMatchRequestObject;
import net.openid.conformance.condition.as.OIDCCGenerateServerConfiguration;
import net.openid.conformance.condition.as.OIDCCGenerateServerJWKs;
import net.openid.conformance.condition.as.OIDCCGetStaticClientConfigurationForRPTests;
import net.openid.conformance.condition.as.SendAuthorizationResponseWithResponseModeFragment;
import net.openid.conformance.condition.as.SendAuthorizationResponseWithResponseModeQuery;
import net.openid.conformance.condition.as.SetRequestParameterSupportedToTrueInServerConfiguration;
import net.openid.conformance.condition.as.SetRequestUriParameterSupportedToTrueInServerConfiguration;
import net.openid.conformance.condition.as.SetTokenEndpointAuthMethodsSupportedToClientSecretBasicOnly;
import net.openid.conformance.condition.as.SetTokenEndpointAuthMethodsSupportedToClientSecretJWTOnly;
import net.openid.conformance.condition.as.SetTokenEndpointAuthMethodsSupportedToClientSecretPostOnly;
import net.openid.conformance.condition.as.SetTokenEndpointAuthMethodsSupportedToPrivateKeyJWTOnly;
import net.openid.conformance.condition.as.SignIdToken;
import net.openid.conformance.condition.as.ValidateAuthorizationCode;
import net.openid.conformance.condition.as.ValidateRedirectUriForTokenEndpointRequest;
import net.openid.conformance.condition.as.ValidateRequestObjectClaims;
import net.openid.conformance.condition.as.ValidateRequestObjectExp;
import net.openid.conformance.condition.as.ValidateRequestObjectSignature;
import net.openid.conformance.condition.as.dynregistration.OIDCCExtractDynamicRegistrationRequest;
import net.openid.conformance.condition.as.dynregistration.OIDCCRegisterClient;
import net.openid.conformance.condition.as.dynregistration.OIDCCValidateDynamicRegistrationRedirectUri;
import net.openid.conformance.condition.client.ExtractJWKsFromStaticClientConfiguration;
import net.openid.conformance.condition.client.GetDynamicClientConfiguration;
import net.openid.conformance.condition.client.ValidateClientJWKsPublicPart;
import net.openid.conformance.condition.client.ValidateServerJWKs;
import net.openid.conformance.condition.common.CheckDistinctKeyIdValueInClientJWKs;
import net.openid.conformance.condition.common.CheckDistinctKeyIdValueInServerJWKs;
import net.openid.conformance.condition.rs.ClearAccessTokenFromRequest;
import net.openid.conformance.condition.rs.LoadUserInfo;
import net.openid.conformance.condition.rs.OIDCCExtractBearerAccessTokenFromRequest;
import net.openid.conformance.condition.rs.RequireBearerAccessToken;
import net.openid.conformance.condition.rs.RequireOpenIDScope;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.sequence.as.OIDCCRegisterClientWithClientSecret;
import net.openid.conformance.sequence.as.OIDCCRegisterClientWithNone;
import net.openid.conformance.sequence.as.OIDCCRegisterClientWithPrivateKeyJwt;
import net.openid.conformance.sequence.as.OIDCCValidateClientAuthenticationWithClientSecretBasic;
import net.openid.conformance.sequence.as.OIDCCValidateClientAuthenticationWithClientSecretJWT;
import net.openid.conformance.sequence.as.OIDCCValidateClientAuthenticationWithClientSecretPost;
import net.openid.conformance.sequence.as.OIDCCValidateClientAuthenticationWithNone;
import net.openid.conformance.sequence.as.ValidateClientAuthenticationWithPrivateKeyJWT;
import net.openid.conformance.testmodule.AbstractTestModule;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.testmodule.TestFailureException;
import net.openid.conformance.testmodule.UserFacing;
import net.openid.conformance.variant.ClientAuthType;
import net.openid.conformance.variant.ClientRegistration;
import net.openid.conformance.variant.ClientRequestType;
import net.openid.conformance.variant.ResponseMode;
import net.openid.conformance.variant.ResponseType;
import net.openid.conformance.variant.VariantConfigurationFields;
import net.openid.conformance.variant.VariantHidesConfigurationFields;
import net.openid.conformance.variant.VariantParameters;
import net.openid.conformance.variant.VariantSetup;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;


@VariantParameters({
	ClientAuthType.class,
	ResponseType.class,
	ResponseMode.class,
	ClientRegistration.class,
	ClientRequestType.class
})
@VariantConfigurationFields(parameter = ClientAuthType.class, value = "client_secret_basic", configurationFields = {
	"client.client_secret"
})
@VariantConfigurationFields(parameter = ClientAuthType.class, value = "client_secret_post", configurationFields = {
	"client.client_secret"
})
@VariantConfigurationFields(parameter = ClientAuthType.class, value = "client_secret_jwt", configurationFields = {
	"client.client_secret",
	"client.client_secret_jwt_alg"
})
@VariantConfigurationFields(parameter = ClientAuthType.class, value = "private_key_jwt", configurationFields = {
	"client.jwks",
	"client.jwks_uri"
})
@VariantConfigurationFields(parameter = ClientRegistration.class, value = "static_client", configurationFields = {
	"client.client_id",
	"client.redirect_uri",
	"client.request_type"
})
@VariantHidesConfigurationFields(parameter = ClientRegistration.class, value = "dynamic_client", configurationFields = {
	"client.client_name",
	"client.client_secret",
	"client.jwks"
})
@VariantHidesConfigurationFields(parameter = ClientAuthType.class, value = "none", configurationFields = {
	"client.client_secret"
})
public abstract class AbstractOIDCCClientTest extends AbstractTestModule {
	protected ResponseType responseType;
	protected ResponseMode responseMode;
	protected ClientRequestType clientRequestType;
	protected ClientRegistration clientRegistrationType;
	protected ClientAuthType clientAuthType;

	protected boolean receivedDiscoveryRequest;
	protected boolean receivedJwksRequest;
	protected boolean receivedRegistrationRequest;
	protected boolean receivedAuthorizationRequest;
	protected boolean receivedTokenRequest;
	protected boolean receivedUserinfoRequest;

	/**
	 * for how long the test will wait for negative tests
	 */
	protected int waitTimeoutSeconds = 5;

	protected Class<? extends Condition> addTokenEndpointAuthMethodSupported;
	protected Class<? extends ConditionSequence> validateClientAuthenticationSteps;
	protected Class<? extends ConditionSequence> authorizationCodeGrantTypeProfileSteps;
	protected Class<? extends ConditionSequence> authorizationEndpointProfileSteps;
	protected Class<? extends ConditionSequence> clientRegistrationSteps;

	protected ResponseType getEffectiveResponseTypeVariant() {
		return getVariant(ResponseType.class);
	}
	protected ResponseMode getEffectiveResponseModeVariant() {
		return getVariant(ResponseMode.class);
	}
	protected ClientRequestType getEffectiveClientRequestTypeVariant() {
		return getVariant(ClientRequestType.class);
	}
	protected ClientRegistration getEffectiveClientRegistrationVariant() {
		return getVariant(ClientRegistration.class);
	}
	protected ClientAuthType getEffectiveClientAuthTypeVariant() {
		return getVariant(ClientAuthType.class);
	}

	@Override
	public void configure(JsonObject config, String baseUrl, String externalUrlOverride) {
		env.putString("base_url", baseUrl);
		env.putObject("config", config);

		if(config.has("waitTimeoutSeconds")) {
			waitTimeoutSeconds = OIDFJSON.getInt(config.get("waitTimeoutSeconds"));
		}
		responseType = getEffectiveResponseTypeVariant();
		env.putString("response_type", responseType.toString());

		responseMode = getEffectiveResponseModeVariant();

		clientRequestType = getEffectiveClientRequestTypeVariant();

		clientRegistrationType = getEffectiveClientRegistrationVariant();

		clientAuthType = getEffectiveClientAuthTypeVariant();

		configureServerConfiguration();

		exposeEnvString("discoveryUrl");
		exposeEnvString("issuer");

		onServerConfigurationCompleted();

		configureServerJWKS();

		validateConfiguredServerJWKS();

		setServerSigningAlgorithm();

		configureUserInfo();

		configureClientConfiguration();

		onBeforeFireSetupDone();
		setStatus(Status.CONFIGURED);
		fireSetupDone();
	}

	/**
	 * override if necessary
	 */
	protected void endTestIfRequiredAuthorizationRequestParametersAreMissing() {

	}

	/**
	 * override if necessary
	 */
	protected void addCustomValuesToIdToken() {

	}

	/**
	 * override if necessary
	 */
	protected void customizeIdTokenSignature() {

	}

	/**
	 * called right before fireSetupDone is called
	 */
	protected void onBeforeFireSetupDone() {

	}

	protected void validateTokenEndpointRequest() {

	}

	protected void validateConfiguredServerJWKS() {
		callAndStopOnFailure(ValidateServerJWKs.class, "RFC7517-1.1");
		callAndContinueOnFailure(CheckDistinctKeyIdValueInServerJWKs.class, Condition.ConditionResult.FAILURE, "RFC7517-4.5");
	}

	/**
	 * expected to add discoveryUrl and issuer to env
	 */
	protected void configureServerConfiguration() {
		callAndStopOnFailure(OIDCCGenerateServerConfiguration.class);
	}

	protected void onServerConfigurationCompleted() {
		//fapi would call callAndStopOnFailure(CheckServerConfiguration.class); here
		switch(clientRequestType) {
			case REQUEST_OBJECT:
				callAndStopOnFailure(SetRequestParameterSupportedToTrueInServerConfiguration.class, "OIDCC-6.1");
				break;
			case REQUEST_URI:
				callAndStopOnFailure(SetRequestUriParameterSupportedToTrueInServerConfiguration.class, "OIDCC-6.2");
				break;
		}
	}

	/**
	 * override to modify the generated jwks
	 */
	protected void configureServerJWKS() {
		callAndStopOnFailure(OIDCCGenerateServerJWKs.class);
	}

	protected void configureUserInfo() {
		callAndStopOnFailure(LoadUserInfo.class);
	}

	protected void configureClientConfiguration() {
		if(clientRegistrationType == ClientRegistration.STATIC_CLIENT) {
			callAndStopOnFailure(OIDCCGetStaticClientConfigurationForRPTests.class);
			processAndValidateClientJwks();
		} else if(clientRegistrationType == ClientRegistration.DYNAMIC_CLIENT) {
			callAndContinueOnFailure(GetDynamicClientConfiguration.class);
			//for dynamic clients, jwks_uri retrieval and jwks validation will be performed after registration
		}

	}

	//TODO may be incomplete or excessive
	protected boolean isClientJwksNeeded() {
		if(clientRequestType == ClientRequestType.REQUEST_OBJECT) {
			return true;
		}
		//or clientAuthType == ClientAuthType.self_signed_tls_client_auth
		if( clientAuthType == ClientAuthType.PRIVATE_KEY_JWT ) {
			return true;
		}

		JsonObject client = env.getObject("client");
		if(client.has("request_object_signing_alg")) {
			String requestObjectSigningAlg = OIDFJSON.getString(client.get("request_object_signing_alg"));
			if (requestObjectSigningAlg != null && requestObjectSigningAlg.matches("^((P|E|R)S\\d{3}|EdDSA)$")) {
				return true;
			}
		}

		if(client.has("id_token_encrypted_response_alg")) {
			String idTokenEncRespAlg = OIDFJSON.getString(client.get("id_token_encrypted_response_alg"));
			if (idTokenEncRespAlg != null && idTokenEncRespAlg.matches("^(RSA|ECDH)")) {
				return true;
			}
		}

		if(client.has("userinfo_encrypted_response_alg")) {
			String userinfoEncRespAlg = OIDFJSON.getString(client.get("userinfo_encrypted_response_alg"));
			if (userinfoEncRespAlg != null && userinfoEncRespAlg.matches("^(RSA|ECDH)")) {
				return true;
			}
		}

		if(client.has("introspection_encrypted_response_alg")) {
			String introspectionEncRespAlg = OIDFJSON.getString(client.get("introspection_encrypted_response_alg"));
			if (introspectionEncRespAlg != null && introspectionEncRespAlg.matches("^(RSA|ECDH)")) {
				return true;
			}
		}

		if(client.has("authorization_encrypted_response_alg")) {
			String authzEncRespAlg = OIDFJSON.getString(client.get("authorization_encrypted_response_alg"));
			if (authzEncRespAlg != null && authzEncRespAlg.matches("^(RSA|ECDH)")) {
				return true;
			}
		}

		return false;
	}


	@Override
	public void start() {
		setStatus(Status.RUNNING);
		// nothing to do here
		setStatus(Status.WAITING);
	}

	@Override
	public Object handleHttp(String path, HttpServletRequest req, HttpServletResponse res, HttpSession session, JsonObject requestParts) {

		setStatus(Status.RUNNING);

		String requestId = "incoming_request_" + RandomStringUtils.randomAlphanumeric(37);

		env.putObject(requestId, requestParts);

		call(exec().mapKey("client_request", requestId));

		validateTlsForIncomingHttpRequest();

		call(exec().unmapKey("client_request"));

		Object responseObject = handleClientRequestForPath(requestId, path);

		if(!finishTestIfAllRequestsAreReceived()) {
			setStatus(Status.WAITING);
		}

		return responseObject;
	}

	protected void validateTlsForIncomingHttpRequest() {
	}

	protected Object handleClientRequestForPath(String requestId, String path){

		if (path.equals("authorize")) {

			receivedAuthorizationRequest = true;
			return handleAuthorizationEndpointRequest(requestId);

		} else if (path.equals("token")) {

			receivedTokenRequest = true;
			return handleTokenEndpointRequest(requestId);

		} else if (path.equals("jwks")) {

			receivedJwksRequest = true;
			return handleJwksEndpointRequest();

		} else if (path.equals("userinfo")) {
			receivedUserinfoRequest = true;
			return handleUserinfoEndpointRequest(requestId);

		} else if (path.equals("register") && clientRegistrationType == ClientRegistration.DYNAMIC_CLIENT) {
			receivedRegistrationRequest = true;
			return handleRegistrationEndpointRequest(requestId);

		} else if (path.equals(".well-known/openid-configuration")) {

			receivedDiscoveryRequest = true;
			return handleDiscoveryEndpointRequest();

		} else {

			throw new TestFailureException(getId(), "Got unexpected HTTP call to " + path);

		}
	}

	protected Object handleDiscoveryEndpointRequest() {
		JsonObject serverConfiguration = env.getObject("server");

		return new ResponseEntity<Object>(serverConfiguration, HttpStatus.OK);
	}


	protected Object handleUserinfoEndpointRequest(String requestId) {

		call(exec().startBlock("Userinfo endpoint").mapKey("incoming_request", requestId));

		validateUserinfoRequest();

		JsonObject user = prepareUserinfoResponse();

		callAndStopOnFailure(ClearAccessTokenFromRequest.class);

		call(exec().unmapKey("incoming_request").endBlock());

		return new ResponseEntity<Object>(user, HttpStatus.OK);

	}

	/**
	 * returns true if fireTestFinished is called
	 *
	 * @return
	 */
	protected boolean finishTestIfAllRequestsAreReceived() {
		boolean fireTestFinishedCalled = false;
		switch (responseType) {
			case CODE:
				if(receivedUserinfoRequest) {
					fireTestFinished();
					fireTestFinishedCalled = true;
				}
				break;
			case CODE_ID_TOKEN:
				if(receivedUserinfoRequest) {
					fireTestFinished();
					fireTestFinishedCalled = true;
				}
				break;
			case ID_TOKEN:
				//TODO test may never end if the client caches the jwks
				if(receivedAuthorizationRequest && receivedJwksRequest) {
					fireTestFinished();
					fireTestFinishedCalled = true;
				}
				break;
			case CODE_TOKEN:
				if(receivedUserinfoRequest) {
					fireTestFinished();
					fireTestFinishedCalled = true;
				}
				break;
			case CODE_ID_TOKEN_TOKEN:
				if(receivedUserinfoRequest) {
					fireTestFinished();
					fireTestFinishedCalled = true;
				}
				break;
			case ID_TOKEN_TOKEN:
				if(receivedUserinfoRequest) {
					fireTestFinished();
					fireTestFinishedCalled = true;
				}
				break;
		}
		return fireTestFinishedCalled;
	}

	protected JsonObject prepareUserinfoResponse() {
		callAndStopOnFailure(FilterUserInfoForScopes.class);
		JsonObject user = env.getObject("user_info_endpoint_response");
		return user;
	}

	protected void validateUserinfoRequest() {
		extractBearerTokenFromUserinfoRequest();
		callAndStopOnFailure(RequireBearerAccessToken.class);
		//TODO is this necessary? (left over from the FAPI test)
		callAndStopOnFailure(RequireOpenIDScope.class);
	}

	/**
	 * Support any of
	 * - Authorization Request Header Field
	 * - Form-Encoded Body Parameter
	 * - URI Query Parameter
	 */
	protected void extractBearerTokenFromUserinfoRequest() {
		callAndStopOnFailure(OIDCCExtractBearerAccessTokenFromRequest.class, "RFC6750-2", "OIDCC-5.3.1");
	}

	protected Object handleJwksEndpointRequest() {

		JsonObject jwks = env.getObject("server_public_jwks");

		return new ResponseEntity<Object>(jwks, HttpStatus.OK);
	}

	private Object handleTokenEndpointRequest(String requestId) {

		call(exec().startBlock("Token endpoint").mapKey("token_endpoint_request", requestId));

		validateTokenEndpointRequest();

		if(validateClientAuthenticationSteps!=null) {
			call(sequence(validateClientAuthenticationSteps));
		}

		return handleTokenEndpointGrantType(requestId);

	}

	protected Object handleTokenEndpointGrantType(String requestId){

		// dispatch based on grant type
		String grantType = env.getString("token_endpoint_request", "body_form_params.grant_type");

		if (grantType.equals("authorization_code")) {
			// we're doing the authorization code grant for user access
			return authorizationCodeGrantType(requestId);
		} else {
			throw new TestFailureException(getId(), "Got a grant type on the token endpoint we didn't understand: " + grantType);
		}
	}

	/**
	 * http request is mapped to "dynamic_registration_request" before this call
	 */
	protected void validateRegistrationRequest() {
		callAndStopOnFailure(OIDCCValidateDynamicRegistrationRedirectUri.class);
	}

	protected Object handleRegistrationEndpointRequest(String requestId) {

		call(exec().startBlock("Registration endpoint").mapKey("incoming_request", requestId));

		callAndStopOnFailure(OIDCCExtractDynamicRegistrationRequest.class);

		validateRegistrationRequest();

		JsonObject registeredClient = registerClient();

		call(exec().unmapKey("incoming_request").endBlock());

		return new ResponseEntity<Object>(registeredClient, HttpStatus.CREATED);

	}

	/**
	 * Override to add additional steps to be executed after the variant (client authentication)
	 * steps are executed
	 * @return
	 */
	protected Class<? extends ConditionSequence> getAdditionalClientRegistrationSteps() {
		return null;
	}

	/**
	 * clients are not persisted anywhere
	 * they are only valid for the duration of the test
	 * @return
	 */
	protected JsonObject registerClient() {
		callAndStopOnFailure(OIDCCRegisterClient.class);

		if(clientRegistrationSteps!=null) {
			call(sequence(clientRegistrationSteps));
		}
		Class<? extends ConditionSequence> additionalSteps = getAdditionalClientRegistrationSteps();
		if(additionalSteps!=null) {
			call(sequence(additionalSteps));
		}
		processAndValidateClientJwks();

		JsonObject client = env.getObject("client");
		return client;
	}

	/**
	 * - runs basic checks
	 * - fetches jwks_uri if provided
	 * - calls validateClientJwks()
	 */
	protected void processAndValidateClientJwks() {
		boolean clientJwksNeeded = isClientJwksNeeded();
		if(clientJwksNeeded) {
			//initial validation
			callAndStopOnFailure(EnsureClientHasJwksOrJwksUri.class);
		}
		callAndStopOnFailure(EnsureClientDoesNotHaveBothJwksAndJwksUri.class);

		//TODO should jwks_uri download be skipped if jwks won't be needed?
		//fetch client jwks from jwks_uri, if a jwks_uri is found
		fetchClientJwksFromJwksUri();

		//at this point jwks has been downloaded from jwks_uri and added to client.jwks
		if(clientJwksNeeded) {
			callAndStopOnFailure(ExtractJWKsFromStaticClientConfiguration.class);
			validateClientJwks();
		}
	}

	protected void validateClientJwks() {
		callAndStopOnFailure(ValidateClientJWKsPublicPart.class, "RFC7517-1.1");
		callAndContinueOnFailure(CheckDistinctKeyIdValueInClientJWKs.class, Condition.ConditionResult.FAILURE, "RFC7517-4.5");
		//TODO add requirements
		callAndContinueOnFailure(EnsureClientJwksDoesNotContainPrivateOrSymmetricKeys.class, Condition.ConditionResult.FAILURE);
	}


	/**
	 * from this point on the client will contain both jwks and jwks_uri
	 */
	protected void fetchClientJwksFromJwksUri() {
		//TODO add requirements
		skipIfElementMissing("client", "jwks_uri", Condition.ConditionResult.INFO, FetchClientKeys.class,
			Condition.ConditionResult.FAILURE);
	}

	protected void validateAuthorizationCodeGrantType() {
		callAndStopOnFailure(ValidateAuthorizationCode.class);

		callAndContinueOnFailure(ValidateRedirectUriForTokenEndpointRequest.class, Condition.ConditionResult.FAILURE, "OIDCC-3.1.3.2");

		generateAccessToken();
	}

	protected void createIdToken(boolean isAuthorizationCodeGrantType) {
		generateIdTokenClaims();

		if(isAuthorizationCodeGrantType) {
			//token endpoint called with code
			if (authorizationCodeGrantTypeProfileSteps != null) {
				call(sequence(authorizationCodeGrantTypeProfileSteps));
			}
			addAtHashToIdToken();
		} else {
			//authorization endpoint
			if (authorizationEndpointProfileSteps != null) {
				call(sequence(authorizationEndpointProfileSteps));
			}
			addCHashToIdToken();
			addAtHashToIdToken();
			//s_hash is not applicable to core tests. Commenting out just in case it's needed in the future
			//addSHashToIdToken();
		}

		addCustomValuesToIdToken();

		signIdToken();

		customizeIdTokenSignature();
	}
/*
	//s_hash is not applicable to core tests. Commenting out just in case it's needed in the future
	protected void addSHashToIdToken() {
		skipIfElementMissing(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, CreateEffectiveAuthorizationRequestParameters.STATE, Condition.ConditionResult.INFO,
			CalculateSHash.class, Condition.ConditionResult.FAILURE);
		skipIfMissing(null, new String[] { "s_hash" }, Condition.ConditionResult.INFO,
			AddSHashToIdTokenClaims.class, Condition.ConditionResult.FAILURE);
	}
*/
	protected void addAtHashToIdToken() {
		skipIfMissing(null, new String[] { "at_hash" }, Condition.ConditionResult.INFO,
			AddAtHashToIdTokenClaims.class, Condition.ConditionResult.FAILURE, "OIDCC-3.3.2.11");
	}

	protected void addCHashToIdToken() {
		skipIfMissing(null, new String[] { "c_hash" }, Condition.ConditionResult.INFO,
			AddCHashToIdTokenClaims.class, Condition.ConditionResult.FAILURE, "OIDCC-3.3.2.11");
	}

	protected Object authorizationCodeGrantType(String requestId) {

		validateAuthorizationCodeGrantType();

		createIdToken(true);

		callAndStopOnFailure(CreateTokenEndpointResponse.class);

		call(exec().unmapKey("token_endpoint_request").endBlock());

		return new ResponseEntity<Object>(env.getObject("token_endpoint_response"), HttpStatus.OK);
	}

	protected void generateIdTokenClaims() {
		callAndStopOnFailure(GenerateIdTokenClaims.class);
	}

	protected void signIdToken() {
		callAndStopOnFailure(SignIdToken.class);
	}

	protected void fetchAndProcessRequestUri() {
		callAndStopOnFailure(FetchRequestUriAndExtractRequestObject.class, "OIDCC-6.2");
		callAndStopOnFailure(EnsureRequestUriIsHttpsOrRequestObjectIsSigned.class, "OIDCC-6.2");
	}

	protected void extractAuthorizationEndpointRequestParameters() {
		if(clientRequestType == ClientRequestType.REQUEST_URI) {
			fetchAndProcessRequestUri();
		} else if(clientRequestType == ClientRequestType.REQUEST_OBJECT) {
			callAndStopOnFailure(ExtractRequestObject.class, "OIDCC-6.1");
		} else {
			//handle plain http request case
			callAndStopOnFailure(EnsureRequestDoesNotContainRequestObject.class, "OIDCC-6.1");
		}

		callAndStopOnFailure(OIDCCEnsureAuthorizationHttpRequestContainsOpenIDScope.class, "OIDCC-6.1", "OIDCC-6.2");

		if(clientRequestType == ClientRequestType.REQUEST_OBJECT || clientRequestType == ClientRequestType.REQUEST_URI) {
			validateRequestObject();
			callAndStopOnFailure(OIDCCEnsureRequiredAuthorizationRequestParametersMatchRequestObject.class, "OIDCC-6.1", "OIDCC-6.2");
			callAndContinueOnFailure(OIDCCEnsureOptionalAuthorizationRequestParametersMatchRequestObject.class,
										Condition.ConditionResult.WARNING, "OIDCC-6.1", "OIDCC-6.2");
		}

		callAndStopOnFailure(CreateEffectiveAuthorizationRequestParameters.class, "OIDCC-6.1", "OIDCC-6.2");

		callAndStopOnFailure(ExtractRequestedScopes.class);

		extractNonceFromAuthorizationEndpointRequestParameters();
	}

	protected void extractNonceFromAuthorizationEndpointRequestParameters() {
		callAndContinueOnFailure(ExtractNonceFromAuthorizationRequest.class, Condition.ConditionResult.INFO, "OIDCC-3.1.2.1");
	}

	/**
	 * end the test here if required parameters are missing
	 */
	protected void validateAuthorizationEndpointRequestParameters() {

		validateResponseTypeAuthorizationRequestParameter();

		callAndStopOnFailure(EnsureMatchingClientId.class, "OIDCC-3.1.2.1");

		callAndStopOnFailure(EnsureValidRedirectUriForAuthorizationEndpointRequest.class, "OIDCC-3.1.2.1");

		endTestIfRequiredAuthorizationRequestParametersAreMissing();

		callAndStopOnFailure(EnsureOpenIDInScopeRequest.class, "OIDCC-3.1.2.1");

	}

	protected void validateRequestObject() {
		callAndStopOnFailure(ValidateRequestObjectExp.class, "RFC7519-4.1.4");
		callAndContinueOnFailure(EnsureNumericRequestObjectClaimsAreNotNull.class, Condition.ConditionResult.WARNING, "OIDCC-13.3");
		callAndStopOnFailure(ValidateRequestObjectClaims.class);
		String alg = env.getString("authorization_request_object", "header.alg");
		if(allowUnsignedRequestObjects() && "none".equals(alg)) {
			//TODO TBD if we should do something
		} else {
			callAndStopOnFailure(ValidateRequestObjectSignature.class, "OIDCC-6.1");
		}
	}

	//TODO implement checks and allow unsigned request objects when appropriate
	protected boolean allowUnsignedRequestObjects() {
		return true;
	}

	protected void validateResponseTypeAuthorizationRequestParameter() {
		switch (responseType) {
			case CODE:
				callAndStopOnFailure(EnsureResponseTypeIsCode.class);
				break;
			case ID_TOKEN:
				callAndStopOnFailure(EnsureResponseTypeIsIdToken.class);
				break;
			case CODE_ID_TOKEN:
				callAndStopOnFailure(EnsureResponseTypeIsCodeIdToken.class);
				break;
			case CODE_ID_TOKEN_TOKEN:
				callAndStopOnFailure(EnsureResponseTypeIsCodeIdTokenToken.class);
				break;
			case CODE_TOKEN:
				callAndStopOnFailure(EnsureResponseTypeIsCodeToken.class);
				break;
			case ID_TOKEN_TOKEN:
				callAndStopOnFailure(EnsureResponseTypeIsIdTokenToken.class);
				break;
			default:
				throw new TestFailureException(getId(), "Unexpected response_type" + responseType.toString());
		}
	}

	protected void setServerSigningAlgorithm() {
		callAndStopOnFailure(ExtractServerSigningAlg.class);
	}

	protected void createAuthorizationCode() {
		callAndStopOnFailure(CreateAuthorizationCode.class);

		//c_hash, s_hash won't work when id_token_signed_response_alg is none

		if(!"none".equals(env.getString("signing_algorithm"))) {
			callAndStopOnFailure(CalculateCHash.class, "OIDCC-3.3.2.11");
		}
	}


	protected void generateAccessToken() {
		callAndStopOnFailure(GenerateBearerAccessToken.class);
		if(!"none".equals(env.getString("signing_algorithm"))) {
			callAndStopOnFailure(CalculateAtHash.class, "OIDCC-3.3.2.11");
		}
	}

	protected void setAuthorizationEndpointRequestParamsForHttpMethod() {
		String httpMethod = env.getString("authorization_endpoint_http_request", "method");
		JsonObject httpRequestObj = env.getObject("authorization_endpoint_http_request");
		if("POST".equals(httpMethod)) {
			env.putObject("authorization_endpoint_http_request_params", httpRequestObj.getAsJsonObject("body_form_params"));
		} else if("GET".equals(httpMethod)) {
			env.putObject("authorization_endpoint_http_request_params", httpRequestObj.getAsJsonObject("query_string_params"));
		} else {
			//this should not happen?
			throw new TestFailureException(getId(), "Got unexpected HTTP method to authorization endpoint");
		}
	}

	@UserFacing
	protected Object handleAuthorizationEndpointRequest(String requestId) {

		call(exec().startBlock("Authorization endpoint").mapKey("authorization_endpoint_http_request", requestId));
		setAuthorizationEndpointRequestParamsForHttpMethod();

		extractAuthorizationEndpointRequestParameters();

		validateAuthorizationEndpointRequestParameters();

		if(responseType.includesCode()) {
			createAuthorizationCode();
		}

		if(responseType.includesToken()) {
			generateAccessToken();
		}

		if(responseType.includesIdToken()) {
			createIdToken(false);
		}

		callAndStopOnFailure(CreateAuthorizationEndpointResponseParams.class);

		if(responseType.includesCode()) {
			callAndStopOnFailure(AddCodeToAuthorizationEndpointResponseParams.class, "OIDCC-3.3.2.5");
		}
		if(responseType.includesIdToken()) {
			callAndStopOnFailure(AddIdTokenToAuthorizationEndpointResponseParams.class, "OIDCC-3.3.2.5");
		}
		if(responseType.includesToken()) {
			callAndStopOnFailure(AddTokenToAuthorizationEndpointResponseParams.class, "OIDCC-3.3.2.5");
		}


		Object viewToReturn = null;
		if(responseMode.isFormPost()) {

			viewToReturn = generateFormPostResponse();

		} else {

			redirectFromAuthorizationEndpoint();

			exposeEnvString("authorization_endpoint_response_redirect");

			String redirectTo = env.getString("authorization_endpoint_response_redirect");

			viewToReturn = new RedirectView(redirectTo, false, false, false);
		}

		call(exec().unmapKey("authorization_endpoint_http_request").endBlock());
		return viewToReturn;
	}

	protected Object generateFormPostResponse() {
		JsonObject responseParams = env.getObject("authorization_endpoint_response_params");
		String formActionUrl = OIDFJSON.getString(responseParams.remove("redirect_uri"));

		return new ModelAndView("formPostResponseMode",
			ImmutableMap.of(
				"formAction", formActionUrl,
				"formParameters", responseParams
			));
	}

	protected void redirectFromAuthorizationEndpoint() {
		if(responseType.includesIdToken() || responseType.includesToken()) {
			callAndStopOnFailure(SendAuthorizationResponseWithResponseModeFragment.class, "OIDCC-3.3.2.5");
		} else if(responseType.includesCode()) {
			callAndStopOnFailure(SendAuthorizationResponseWithResponseModeQuery.class, "OIDCC-3.3.2.5");
		} else {
			throw new TestFailureException(getId(), "Unexpected response_type" + responseType.toString());
		}
	}

	@VariantSetup(parameter = ClientAuthType.class, value = "none")
	public void setupClientAuthNone() {
		addTokenEndpointAuthMethodSupported = null;
		validateClientAuthenticationSteps = OIDCCValidateClientAuthenticationWithNone.class;
		clientRegistrationSteps = OIDCCRegisterClientWithNone.class;
	}

	@VariantSetup(parameter = ClientAuthType.class, value = "private_key_jwt")
	public void setupPrivateKeyJwt() {
		addTokenEndpointAuthMethodSupported = SetTokenEndpointAuthMethodsSupportedToPrivateKeyJWTOnly.class;
		validateClientAuthenticationSteps = ValidateClientAuthenticationWithPrivateKeyJWT.class;
		clientRegistrationSteps = OIDCCRegisterClientWithPrivateKeyJwt.class;
	}

	@VariantSetup(parameter = ClientAuthType.class, value = "client_secret_basic")
	public void setupClientSecretBasic() {
		addTokenEndpointAuthMethodSupported = SetTokenEndpointAuthMethodsSupportedToClientSecretBasicOnly.class;
		validateClientAuthenticationSteps = OIDCCValidateClientAuthenticationWithClientSecretBasic.class;
		clientRegistrationSteps = OIDCCRegisterClientWithClientSecret.class;
	}

	@VariantSetup(parameter = ClientAuthType.class, value = "client_secret_jwt")
	public void setupClientSecretJWT() {
		addTokenEndpointAuthMethodSupported = SetTokenEndpointAuthMethodsSupportedToClientSecretJWTOnly.class;
		validateClientAuthenticationSteps = OIDCCValidateClientAuthenticationWithClientSecretJWT.class;
		clientRegistrationSteps = OIDCCRegisterClientWithClientSecret.class;
	}

	@VariantSetup(parameter = ClientAuthType.class, value = "client_secret_post")
	public void setupClientSecretPost() {
		addTokenEndpointAuthMethodSupported = SetTokenEndpointAuthMethodsSupportedToClientSecretPostOnly.class;
		validateClientAuthenticationSteps = OIDCCValidateClientAuthenticationWithClientSecretPost.class;
		clientRegistrationSteps = OIDCCRegisterClientWithClientSecret.class;
	}

	/**
	 * Only use in tests that need to wait for a timeout
	 * As the client hasn't called an endpoint after waitTimeoutSeconds (from configuration) seconds,
	 * assume it has correctly detected the error and aborted.
	 */
	protected void startWaitingForTimeout() {
		getTestExecutionManager().runInBackground(() -> {
			Thread.sleep(waitTimeoutSeconds * 1000);
			if (getStatus().equals(Status.WAITING)) {
				setStatus(Status.RUNNING);
				fireTestFinished();
			}
			return "done";
		});
	}

}
