package net.openid.conformance.openid.client;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.as.AddAtHashToIdTokenClaims;
import net.openid.conformance.condition.as.AddCHashToIdTokenClaims;
import net.openid.conformance.condition.as.AddClientSecretBasicAuthnMethodToServerConfiguration;
import net.openid.conformance.condition.as.AddClientSecretJWTAuthnMethodToServerConfiguration;
import net.openid.conformance.condition.as.AddClientSecretPostAuthnMethodToServerConfiguration;
import net.openid.conformance.condition.as.AddCodeToAuthorizationEndpointResponseParams;
import net.openid.conformance.condition.as.AddIdTokenToAuthorizationEndpointResponseParams;
import net.openid.conformance.condition.as.AddPrivateKeyJWTAuthnMethodToServerConfiguration;
import net.openid.conformance.condition.as.AddTokenToAuthorizationEndpointResponseParams;
import net.openid.conformance.condition.as.CalculateAtHash;
import net.openid.conformance.condition.as.CalculateCHash;
import net.openid.conformance.condition.as.CreateAuthorizationCode;
import net.openid.conformance.condition.as.CreateAuthorizationEndpointResponseParams;
import net.openid.conformance.condition.as.CreateTokenEndpointResponse;
import net.openid.conformance.condition.as.EnsureAuthorizationParametersMatchRequestObject;
import net.openid.conformance.condition.as.EnsureMatchingClientId;
import net.openid.conformance.condition.as.EnsureOpenIDInScopeRequest;
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
import net.openid.conformance.condition.as.FilterUserInfoForScopes;
import net.openid.conformance.condition.as.GenerateBearerAccessToken;
import net.openid.conformance.condition.as.GenerateIdTokenClaims;
import net.openid.conformance.condition.as.LoadServerJWKs;
import net.openid.conformance.condition.as.OIDCCGenerateServerConfiguration;
import net.openid.conformance.condition.as.OIDCCGetStaticClientConfigurationForRPTests;
import net.openid.conformance.condition.as.SendAuthorizationResponseWithResponseModeFragment;
import net.openid.conformance.condition.as.SendAuthorizationResponseWithResponseModeQuery;
import net.openid.conformance.condition.as.SignIdToken;
import net.openid.conformance.condition.as.ValidateAuthorizationCode;
import net.openid.conformance.condition.as.ValidateRedirectUri;
import net.openid.conformance.condition.as.ValidateRedirectUriForTokenEndpointRequest;
import net.openid.conformance.condition.as.ValidateRequestObjectClaims;
import net.openid.conformance.condition.as.ValidateRequestObjectExp;
import net.openid.conformance.condition.as.ValidateRequestObjectSignature;
import net.openid.conformance.condition.as.dynregistration.OIDCCExtractDynamicRegistrationRequest;
import net.openid.conformance.condition.as.dynregistration.OIDCCValidateDynamicRegistrationRedirectUri;
import net.openid.conformance.condition.client.ExtractJWKsFromStaticClientConfiguration;
import net.openid.conformance.condition.client.GetDynamicClientConfiguration;
import net.openid.conformance.condition.client.ValidateClientJWKsPublicPart;
import net.openid.conformance.condition.client.ValidateServerJWKs;
import net.openid.conformance.condition.rs.ClearAccessTokenFromRequest;
import net.openid.conformance.condition.rs.EnsureBearerAccessTokenNotInParams;
import net.openid.conformance.condition.rs.ExtractBearerAccessTokenFromHeader;
import net.openid.conformance.condition.rs.LoadUserInfo;
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
import net.openid.conformance.sequence.as.OIDCCValidateClientAuthenticationWithPrivateKeyJWT;
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
	"client.jwks"
})
@VariantConfigurationFields(parameter = ClientRegistration.class, value = "static_client", configurationFields = {
	"client.client_id",
	"client.redirect_uri",
	"client.request_type"
})
@VariantConfigurationFields(parameter = ClientRegistration.class, value = "dynamic_client", configurationFields = {
	"client.client_name"
})
@VariantHidesConfigurationFields(parameter = ClientRegistration.class, value = "dynamic_client", configurationFields = {
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

	@Override
	public void configure(JsonObject config, String baseUrl, String externalUrlOverride) {
		env.putString("base_url", baseUrl);
		env.putObject("config", config);

		if(config.has("waitTimeoutSeconds")) {
			waitTimeoutSeconds = OIDFJSON.getInt(config.get("waitTimeoutSeconds"));
		}
		responseType = getVariant(ResponseType.class);
		env.putString("response_type", responseType.toString());

		responseMode = getVariant(ResponseMode.class);

		clientRequestType = getVariant(ClientRequestType.class);

		clientRegistrationType = getVariant(ClientRegistration.class);

		configureServerConfiguration();

		exposeEnvString("discoveryUrl");
		exposeEnvString("issuer");

		onServerConfigurationCompleted();

		configureServerJWKS();

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

	/**
	 * expected to add discoveryUrl and issuer to env
	 */
	protected void configureServerConfiguration() {
		callAndStopOnFailure(OIDCCGenerateServerConfiguration.class);
	}

	protected void onServerConfigurationCompleted() {
		//fapi would call callAndStopOnFailure(CheckServerConfiguration.class); here
	}

	/**
	 * TODO we should have a constant JWKS instead of asking the user to enter the JWKS in configuration
	 */
	protected void configureServerJWKS() {
		callAndStopOnFailure(LoadServerJWKs.class);

		callAndStopOnFailure(ValidateServerJWKs.class, "RFC7517-1.1");
	}

	protected void configureUserInfo() {
		callAndStopOnFailure(LoadUserInfo.class);
	}

	protected void configureClientConfiguration() {
		if(clientRegistrationType == ClientRegistration.STATIC_CLIENT) {
			callAndStopOnFailure(OIDCCGetStaticClientConfigurationForRPTests.class);
		} else if(clientRegistrationType == ClientRegistration.DYNAMIC_CLIENT) {
			callAndContinueOnFailure(GetDynamicClientConfiguration.class);
		}

		if(clientRequestType == ClientRequestType.REQUEST_OBJECT) {
			callAndStopOnFailure(ValidateClientJWKsPublicPart.class, "RFC7517-1.1");
			// for signing request objects
			callAndStopOnFailure(ExtractJWKsFromStaticClientConfiguration.class);
		}
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

		setStatus(Status.WAITING);

		Object responseObject = handleClientRequestForPath(requestId, path);

		finishTestIfAllRequestsAreReceived();

		return responseObject;
	}

	protected void validateTlsForIncomingHttpRequest() {
		//callAndContinueOnFailure(EnsureIncomingTls12.class, "FAPI-R-7.1-1");
		//callAndContinueOnFailure(EnsureIncomingTlsSecureCipher.class, Condition.ConditionResult.FAILURE, "FAPI-R-7.1-1");
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
		setStatus(Status.RUNNING);
		JsonObject serverConfiguration = env.getObject("server");

		setStatus(Status.WAITING);
		return new ResponseEntity<Object>(serverConfiguration, HttpStatus.OK);
	}


	protected Object handleUserinfoEndpointRequest(String requestId) {

		setStatus(Status.RUNNING);

		call(exec().startBlock("Userinfo endpoint").mapKey("incoming_request", requestId));

		validateUserinfoRequest();

		JsonObject user = prepareUserinfoResponse();

		callAndStopOnFailure(ClearAccessTokenFromRequest.class);

		call(exec().unmapKey("incoming_request").endBlock());

		setStatus(Status.WAITING);

		return new ResponseEntity<Object>(user, HttpStatus.OK);

	}

	protected void finishTestIfAllRequestsAreReceived() {
		switch (responseType) {
			case CODE:
				if(receivedUserinfoRequest) {
					fireTestFinished();
				}
				break;
			case CODE_ID_TOKEN:
				if(receivedUserinfoRequest) {
					fireTestFinished();
				}
				break;
			case ID_TOKEN:
				//TODO test may never end if the client caches the jwks
				if(receivedAuthorizationRequest && receivedJwksRequest) {
					fireTestFinished();
				}
				break;
			case CODE_TOKEN:
				if(receivedUserinfoRequest) {
					fireTestFinished();
				}
				break;
			case CODE_ID_TOKEN_TOKEN:
				if(receivedUserinfoRequest) {
					fireTestFinished();
				}
				break;
			case ID_TOKEN_TOKEN:
				if(receivedUserinfoRequest) {
					fireTestFinished();
				}
				break;
		}

	}

	protected JsonObject prepareUserinfoResponse() {
		callAndStopOnFailure(FilterUserInfoForScopes.class);
		JsonObject user = env.getObject("user_info_endpoint_response");
		return user;
	}

	protected void validateUserinfoRequest() {
		callAndStopOnFailure(EnsureBearerAccessTokenNotInParams.class, "FAPI-R-6.2.2-1");
		callAndStopOnFailure(ExtractBearerAccessTokenFromHeader.class, "FAPI-R-6.2.2-1");

		callAndStopOnFailure(RequireBearerAccessToken.class);

		callAndStopOnFailure(RequireOpenIDScope.class, "FAPI-R-5.2.3-7");

	}

	protected Object handleJwksEndpointRequest() {

		setStatus(Status.RUNNING);
		JsonObject jwks = env.getObject("server_public_jwks");

		setStatus(Status.WAITING);

		return new ResponseEntity<Object>(jwks, HttpStatus.OK);
	}

	private Object handleTokenEndpointRequest(String requestId) {

		setStatus(Status.RUNNING);

		call(exec().startBlock("Token endpoint").mapKey("token_endpoint_request", requestId));

		validateTokenEndpointRequest();

		if(validateClientAuthenticationSteps!=null) {
			call(sequence(validateClientAuthenticationSteps));
		}

		return handleTokenEndpointGrantType(requestId);

	}

	protected Object handleTokenEndpointGrantType(String requestId){

		// dispatch based on grant type
		String grantType = env.getString("token_endpoint_request", "params.grant_type");

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

		setStatus(Status.RUNNING);

		call(exec().startBlock("Registration endpoint").mapKey("incoming_request", requestId));

		callAndStopOnFailure(OIDCCExtractDynamicRegistrationRequest.class);

		validateRegistrationRequest();

		JsonObject registeredClient = registerClient();

		call(exec().unmapKey("incoming_request").endBlock());

		setStatus(Status.WAITING);

		return new ResponseEntity<Object>(registeredClient, HttpStatus.CREATED);

	}

	/**
	 * clients are not persisted anywhere
	 * they are only valid for the duration of the test
	 * @return
	 */
	protected JsonObject registerClient() {
		if(clientRegistrationSteps!=null) {
			call(sequence(clientRegistrationSteps));
		}
		JsonObject client = env.getObject("client");
		return client;
	}

	protected void validateAuthorizationCodeGrantType() {
		callAndStopOnFailure(ValidateAuthorizationCode.class);

		//TODO this is not good. ValidateRedirectUri and ValidateRedirectUriForTokenEndpointRequest could be merged
		if(clientRegistrationType == ClientRegistration.DYNAMIC_CLIENT) {
			callAndStopOnFailure(ValidateRedirectUriForTokenEndpointRequest.class);
		} else {
			callAndStopOnFailure(ValidateRedirectUri.class);
		}

		callAndStopOnFailure(GenerateBearerAccessToken.class);

	}

	protected void createIdToken(boolean isAuthorizationCodeGrantType) {
		generateIdTokenClaims();

		if(isAuthorizationCodeGrantType) {
			//token endpoint called with code
			if (authorizationCodeGrantTypeProfileSteps != null) {
				call(sequence(authorizationCodeGrantTypeProfileSteps));
			}
		} else {
			//authorization endpoint
			if (authorizationEndpointProfileSteps != null) {
				call(sequence(authorizationEndpointProfileSteps));
			}
			skipIfMissing(null, new String[] { "c_hash" }, Condition.ConditionResult.INFO,
				AddCHashToIdTokenClaims.class, Condition.ConditionResult.FAILURE, "OIDCC-3.3.2.11");

			skipIfMissing(null, new String[] { "at_hash" }, Condition.ConditionResult.INFO,
				AddAtHashToIdTokenClaims.class, Condition.ConditionResult.FAILURE, "OIDCC-3.3.2.11");
		}

		addCustomValuesToIdToken();

		signIdToken();

		customizeIdTokenSignature();
	}

	protected Object authorizationCodeGrantType(String requestId) {

		validateAuthorizationCodeGrantType();

		createIdToken(true);

		callAndStopOnFailure(CreateTokenEndpointResponse.class);

		call(exec().unmapKey("token_endpoint_request").endBlock());

		setStatus(Status.WAITING);

		return new ResponseEntity<Object>(env.getObject("token_endpoint_response"), HttpStatus.OK);

	}

	protected void generateIdTokenClaims() {
		callAndStopOnFailure(GenerateIdTokenClaims.class);
	}

	protected void signIdToken() {
		callAndStopOnFailure(SignIdToken.class);
	}

	protected void fetchAndProcessRequestUri() {
		//TODO implement request_uri support
	}

	/**
	 * TODO we need to extract variables from request objects and refactor all existing code
	 * to support both request objects and plain http requests
	 * for example CalculateSHash only works with
	 * String state = env.getString("authorization_request_object", "claims.state");
	 */
	protected void extractAuthorizationEndpointRequestParameters() {
		if(clientRequestType == ClientRequestType.REQUEST_URI) {

			fetchAndProcessRequestUri();

		} else if(clientRequestType == ClientRequestType.REQUEST_OBJECT) {

			callAndStopOnFailure(ExtractRequestObject.class, "FAPI-RW-5.2.2-10");
			callAndStopOnFailure(EnsureAuthorizationParametersMatchRequestObject.class);

		} else {
			//handle plain http request case
		}

		callAndStopOnFailure(ExtractRequestedScopes.class);

		callAndContinueOnFailure(ExtractNonceFromAuthorizationRequest.class, Condition.ConditionResult.INFO, "OIDCC-3.1.2.1");

		//TODO what to do with this?
		//callAndStopOnFailure(ExtractServerSigningAlg.class);

	}

	/**
	 * end the test here if required parameters are missing
	 */
	protected void validateAuthorizationEndpointRequestParameters() {
		if(clientRequestType == ClientRequestType.REQUEST_OBJECT || clientRequestType == ClientRequestType.REQUEST_URI) {
			extractAndValidateRequestObject();
		}

		validateResponseTypeAuthorizationRequestParameter();

		callAndStopOnFailure(EnsureMatchingClientId.class, "OIDCC-3.1.2.1");

		callAndStopOnFailure(EnsureValidRedirectUriForAuthorizationEndpointRequest.class);

		endTestIfRequiredAuthorizationRequestParametersAreMissing();

		callAndStopOnFailure(EnsureOpenIDInScopeRequest.class, "FAPI-R-5.2.3-7");

	}

	protected void extractAndValidateRequestObject() {
		callAndStopOnFailure(EnsureAuthorizationParametersMatchRequestObject.class);
		callAndStopOnFailure(ValidateRequestObjectExp.class, "RFC7519-4.1.4");
		callAndStopOnFailure(ValidateRequestObjectClaims.class);
		callAndStopOnFailure(ValidateRequestObjectSignature.class, "FAPI-RW-5.2.2.1");
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

	/**
	 * TODO always returns RS256 for now
	 */
	protected void setServerSigningAlgorithm() {
		env.putString("signing_algorithm", "RS256");
	}

	protected void createAuthorizationCode() {
		callAndStopOnFailure(CreateAuthorizationCode.class);

		callAndStopOnFailure(CalculateCHash.class, "OIDCC-3.3.2.11");

		/*
		skipIfElementMissing("authorization_request_object", "claims.state", Condition.ConditionResult.INFO,
			CalculateSHash.class, Condition.ConditionResult.FAILURE, "FAPI-RW-5.2.2-4");
		*/
	}

	protected void generateAccessToken() {
		callAndStopOnFailure(GenerateBearerAccessToken.class);

		callAndStopOnFailure(CalculateAtHash.class, "OIDCC-3.3.2.11");
	}

	@UserFacing
	protected Object handleAuthorizationEndpointRequest(String requestId) {

		setStatus(Status.RUNNING);

		call(exec().startBlock("Authorization endpoint").mapKey("authorization_endpoint_request", requestId));

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

		call(exec().unmapKey("authorization_endpoint_request").endBlock());
		setStatus(Status.WAITING);
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
		addTokenEndpointAuthMethodSupported = AddPrivateKeyJWTAuthnMethodToServerConfiguration.class;
		validateClientAuthenticationSteps = OIDCCValidateClientAuthenticationWithPrivateKeyJWT.class;
		clientRegistrationSteps = OIDCCRegisterClientWithPrivateKeyJwt.class;
	}

	@VariantSetup(parameter = ClientAuthType.class, value = "client_secret_basic")
	public void setupClientSecretBasic() {
		addTokenEndpointAuthMethodSupported = AddClientSecretBasicAuthnMethodToServerConfiguration.class;
		validateClientAuthenticationSteps = OIDCCValidateClientAuthenticationWithClientSecretBasic.class;
		clientRegistrationSteps = OIDCCRegisterClientWithClientSecret.class;
	}

	@VariantSetup(parameter = ClientAuthType.class, value = "client_secret_jwt")
	public void setupClientSecretJWT() {
		addTokenEndpointAuthMethodSupported = AddClientSecretJWTAuthnMethodToServerConfiguration.class;
		validateClientAuthenticationSteps = OIDCCValidateClientAuthenticationWithClientSecretJWT.class;
		clientRegistrationSteps = OIDCCRegisterClientWithClientSecret.class;
	}

	@VariantSetup(parameter = ClientAuthType.class, value = "client_secret_post")
	public void setupClientSecretPost() {
		addTokenEndpointAuthMethodSupported = AddClientSecretPostAuthnMethodToServerConfiguration.class;
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
