package net.openid.conformance.openid.client;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.as.AddAtHashToIdTokenClaims;
import net.openid.conformance.condition.as.AddAuthTimeToIdTokenClaims;
import net.openid.conformance.condition.as.AddCHashToIdTokenClaims;
import net.openid.conformance.condition.as.AddCodeToAuthorizationEndpointResponseParams;
import net.openid.conformance.condition.as.AddIdTokenToAuthorizationEndpointResponseParams;
import net.openid.conformance.condition.as.AddIssAndAudToUserInfoResponse;
import net.openid.conformance.condition.as.AddTokenToAuthorizationEndpointResponseParams;
import net.openid.conformance.condition.as.CalculateAtHash;
import net.openid.conformance.condition.as.CalculateCHash;
import net.openid.conformance.condition.as.ChangeTokenEndpointInServerConfigurationToMtls;
import net.openid.conformance.condition.as.CheckClientIdMatchesOnTokenRequestIfPresent;
import net.openid.conformance.condition.as.CheckForUnexpectedClaimsInClaimsParameter;
import net.openid.conformance.condition.as.CheckForUnexpectedClaimsInRequestObject;
import net.openid.conformance.condition.as.CheckForUnexpectedOpenIdClaims;
import net.openid.conformance.condition.as.CheckPkceCodeVerifier;
import net.openid.conformance.condition.as.CheckRequestObjectClaimsParameterMemberValues;
import net.openid.conformance.condition.as.CheckRequestObjectClaimsParameterValues;
import net.openid.conformance.condition.as.CreateAuthorizationCode;
import net.openid.conformance.condition.as.CreateAuthorizationEndpointResponseParams;
import net.openid.conformance.condition.as.CreateEffectiveAuthorizationRequestParameters;
import net.openid.conformance.condition.as.CreateTokenEndpointResponse;
import net.openid.conformance.condition.as.CreateWebfingerResponse;
import net.openid.conformance.condition.as.DisallowMaxAgeEqualsZeroAndPromptNone;
import net.openid.conformance.condition.as.EncryptIdToken;
import net.openid.conformance.condition.as.EncryptUserInfoResponse;
import net.openid.conformance.condition.as.EnsureAuthorizationHttpRequestContainsOpenIDScope;
import net.openid.conformance.condition.as.EnsureAuthorizationRequestContainsPkceCodeChallenge;
import net.openid.conformance.condition.as.EnsureClientDoesNotHaveBothJwksAndJwksUri;
import net.openid.conformance.condition.as.EnsureClientHasJwksOrJwksUri;
import net.openid.conformance.condition.as.EnsureClientJwksDoesNotContainPrivateOrSymmetricKeys;
import net.openid.conformance.condition.as.EnsureMatchingClientId;
import net.openid.conformance.condition.as.EnsureNumericRequestObjectClaimsAreNotNull;
import net.openid.conformance.condition.as.EnsureOpenIDInScopeRequest;
import net.openid.conformance.condition.as.EnsureOptionalAuthorizationRequestParametersMatchRequestObject;
import net.openid.conformance.condition.as.EnsureRequestDoesNotContainRequestObject;
import net.openid.conformance.condition.as.EnsureRequestObjectDoesNotContainRequestOrRequestUri;
import net.openid.conformance.condition.as.EnsureRequestObjectDoesNotContainSubWithClientId;
import net.openid.conformance.condition.as.EnsureRequestUriIsHttpsOrRequestObjectIsSigned;
import net.openid.conformance.condition.as.EnsureRequiredAuthorizationRequestParametersMatchRequestObject;
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
import net.openid.conformance.condition.as.FetchClientKeys;
import net.openid.conformance.condition.as.FetchRequestUriAndExtractRequestObject;
import net.openid.conformance.condition.as.FilterUserInfoForScopes;
import net.openid.conformance.condition.as.GenerateBearerAccessToken;
import net.openid.conformance.condition.as.GenerateIdTokenClaims;
import net.openid.conformance.condition.as.OIDCCAddRequestObjectSigningAlgValuesSupportedToServerConfiguration;
import net.openid.conformance.condition.as.OIDCCExtractServerSigningAlg;
import net.openid.conformance.condition.as.OIDCCGenerateServerConfiguration;
import net.openid.conformance.condition.as.OIDCCGenerateServerJWKs;
import net.openid.conformance.condition.as.OIDCCGetStaticClientConfigurationForRPTests;
import net.openid.conformance.condition.as.OIDCCSignIdToken;
import net.openid.conformance.condition.as.OIDCCValidateRequestObjectExp;
import net.openid.conformance.condition.as.SendAuthorizationResponseWithResponseModeFragment;
import net.openid.conformance.condition.as.SendAuthorizationResponseWithResponseModeQuery;
import net.openid.conformance.condition.as.SetRequestParameterSupportedToTrueInServerConfiguration;
import net.openid.conformance.condition.as.SetRequestUriParameterSupportedToTrueInServerConfiguration;
import net.openid.conformance.condition.as.SetTokenEndpointAuthMethodsSupportedToClientSecretBasicOnly;
import net.openid.conformance.condition.as.SetTokenEndpointAuthMethodsSupportedToClientSecretJWTOnly;
import net.openid.conformance.condition.as.SetTokenEndpointAuthMethodsSupportedToClientSecretPostOnly;
import net.openid.conformance.condition.as.SetTokenEndpointAuthMethodsSupportedToPrivateKeyJWTOnly;
import net.openid.conformance.condition.as.SetTokenEndpointAuthMethodsSupportedToSelfSignedTlsClientAuthOnly;
import net.openid.conformance.condition.as.SetTokenEndpointAuthMethodsSupportedToTlsClientAuthOnly;
import net.openid.conformance.condition.as.SignUserInfoResponse;
import net.openid.conformance.condition.as.ValidateAuthorizationCode;
import net.openid.conformance.condition.as.ValidateEncryptedRequestObjectHasKid;
import net.openid.conformance.condition.as.ValidateRedirectUriForTokenEndpointRequest;
import net.openid.conformance.condition.as.ValidateRequestObjectAud;
import net.openid.conformance.condition.as.ValidateRequestObjectIat;
import net.openid.conformance.condition.as.ValidateRequestObjectIss;
import net.openid.conformance.condition.as.ValidateRequestObjectMaxAge;
import net.openid.conformance.condition.as.ValidateRequestObjectSignature;
import net.openid.conformance.condition.as.dynregistration.EnsureIdTokenEncryptedResponseAlgIsSetIfEncIsSet;
import net.openid.conformance.condition.as.dynregistration.EnsureRegistrationRequestContainsAtLeastOneContact;
import net.openid.conformance.condition.as.dynregistration.EnsureRequestObjectEncryptionAlgIsSetIfEncIsSet;
import net.openid.conformance.condition.as.dynregistration.EnsureUserinfoEncryptedResponseAlgIsSetIfEncIsSet;
import net.openid.conformance.condition.as.dynregistration.OIDCCExtractDynamicRegistrationRequest;
import net.openid.conformance.condition.as.dynregistration.OIDCCRegisterClient;
import net.openid.conformance.condition.as.dynregistration.OIDCCValidateClientRedirectUris;
import net.openid.conformance.condition.as.dynregistration.SetClientIdTokenSignedResponseAlgToServerSigningAlg;
import net.openid.conformance.condition.as.dynregistration.ValidateClientGrantTypes;
import net.openid.conformance.condition.as.dynregistration.ValidateClientLogoUris;
import net.openid.conformance.condition.as.dynregistration.ValidateClientPolicyUris;
import net.openid.conformance.condition.as.dynregistration.ValidateClientRegistrationRequestSectorIdentifierUri;
import net.openid.conformance.condition.as.dynregistration.ValidateClientSubjectType;
import net.openid.conformance.condition.as.dynregistration.ValidateClientTosUris;
import net.openid.conformance.condition.as.dynregistration.ValidateClientUris;
import net.openid.conformance.condition.as.dynregistration.ValidateDefaultAcrValues;
import net.openid.conformance.condition.as.dynregistration.ValidateDefaultMaxAge;
import net.openid.conformance.condition.as.dynregistration.ValidateIdTokenSignedResponseAlg;
import net.openid.conformance.condition.as.dynregistration.ValidateInitiateLoginUri;
import net.openid.conformance.condition.as.dynregistration.ValidateRequestObjectSigningAlg;
import net.openid.conformance.condition.as.dynregistration.ValidateRequestUris;
import net.openid.conformance.condition.as.dynregistration.ValidateRequireAuthTime;
import net.openid.conformance.condition.as.dynregistration.ValidateTokenEndpointAuthSigningAlg;
import net.openid.conformance.condition.as.dynregistration.ValidateUserinfoSignedResponseAlg;
import net.openid.conformance.condition.client.ConfigurationRequestsTestIsSkipped;
import net.openid.conformance.condition.client.ExtractClientNameFromStoredConfig;
import net.openid.conformance.condition.client.ExtractJWKsFromStaticClientConfiguration;
import net.openid.conformance.condition.client.StoreOriginalClientConfiguration;
import net.openid.conformance.condition.client.ValidateClientJWKsPublicPart;
import net.openid.conformance.condition.client.ValidateServerJWKs;
import net.openid.conformance.condition.common.CheckDistinctKeyIdValueInClientJWKs;
import net.openid.conformance.condition.common.CheckDistinctKeyIdValueInServerJWKs;
import net.openid.conformance.condition.rs.ClearAccessTokenFromRequest;
import net.openid.conformance.condition.rs.OIDCCExtractBearerAccessTokenFromRequest;
import net.openid.conformance.condition.rs.OIDCCLoadUserInfo;
import net.openid.conformance.condition.rs.RequireBearerAccessToken;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.sequence.as.OIDCCRegisterClientWithClientSecretBasic;
import net.openid.conformance.sequence.as.OIDCCRegisterClientWithClientSecretJwt;
import net.openid.conformance.sequence.as.OIDCCRegisterClientWithClientSecretPost;
import net.openid.conformance.sequence.as.OIDCCRegisterClientWithNone;
import net.openid.conformance.sequence.as.OIDCCRegisterClientWithPrivateKeyJwt;
import net.openid.conformance.sequence.as.OIDCCRegisterClientWithSelfSignedTlsClientAuth;
import net.openid.conformance.sequence.as.OIDCCRegisterClientWithTlsClientAuth;
import net.openid.conformance.sequence.as.OIDCCValidateClientAuthenticationWithClientSecretBasic;
import net.openid.conformance.sequence.as.OIDCCValidateClientAuthenticationWithClientSecretJWT;
import net.openid.conformance.sequence.as.OIDCCValidateClientAuthenticationWithClientSecretPost;
import net.openid.conformance.sequence.as.OIDCCValidateClientAuthenticationWithNone;
import net.openid.conformance.sequence.as.OIDCCValidateClientAuthenticationWithSelfSignedTlsClientAuth;
import net.openid.conformance.sequence.as.OIDCCValidateClientAuthenticationWithTlsClientAuth;
import net.openid.conformance.sequence.as.ValidateClientAuthenticationWithPrivateKeyJWT;
import net.openid.conformance.testmodule.AbstractTestModule;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.testmodule.TestFailureException;
import net.openid.conformance.testmodule.UserFacing;
import net.openid.conformance.util.JWEUtil;
import net.openid.conformance.util.JWSUtil;
import net.openid.conformance.variant.ClientRegistration;
import net.openid.conformance.variant.ClientRequestType;
import net.openid.conformance.variant.OIDCCClientAuthType;
import net.openid.conformance.variant.ResponseMode;
import net.openid.conformance.variant.ResponseType;
import net.openid.conformance.variant.VariantConfigurationFields;
import net.openid.conformance.variant.VariantHidesConfigurationFields;
import net.openid.conformance.variant.VariantParameters;
import net.openid.conformance.variant.VariantSetup;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import java.time.Instant;


@VariantParameters({
	OIDCCClientAuthType.class,
	ResponseType.class,
	ResponseMode.class,
	ClientRegistration.class,
	ClientRequestType.class
})
@VariantConfigurationFields(parameter = OIDCCClientAuthType.class, value = "client_secret_basic", configurationFields = {
	"client.client_secret"
})
@VariantConfigurationFields(parameter = OIDCCClientAuthType.class, value = "client_secret_post", configurationFields = {
	"client.client_secret"
})
@VariantConfigurationFields(parameter = OIDCCClientAuthType.class, value = "client_secret_jwt", configurationFields = {
	"client.client_secret",
	"client.client_secret_jwt_alg"
})
@VariantConfigurationFields(parameter = OIDCCClientAuthType.class, value = "private_key_jwt", configurationFields = {
	"client.jwks",
	"client.jwks_uri"
})
@VariantConfigurationFields(parameter = OIDCCClientAuthType.class, value = "tls_client_auth", configurationFields = {
	"client.tls_client_auth_subject_dn",
	"client.tls_client_auth_san_dns",
	"client.tls_client_auth_san_uri",
	"client.tls_client_auth_san_ip",
	"client.tls_client_auth_san_email"
})
@VariantConfigurationFields(parameter = OIDCCClientAuthType.class, value = "self_signed_tls_client_auth", configurationFields = {
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
	"client.jwks",
	"client.jwks_uri",
	"client.tls_client_auth_subject_dn",
	"client.tls_client_auth_san_dns",
	"client.tls_client_auth_san_uri",
	"client.tls_client_auth_san_ip",
	"client.tls_client_auth_san_email"
})
@VariantHidesConfigurationFields(parameter = OIDCCClientAuthType.class, value = "none", configurationFields = {
	"client.client_secret"
})
public abstract class AbstractOIDCCClientTest extends AbstractTestModule {
	protected ResponseType responseType;
	protected ResponseMode responseMode;
	protected ClientRequestType clientRequestType;
	protected ClientRegistration clientRegistrationType;
	protected OIDCCClientAuthType clientAuthType;

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
	protected OIDCCClientAuthType getEffectiveClientAuthTypeVariant() {
		return getVariant(OIDCCClientAuthType.class);
	}

	@Override
	public void configure(JsonObject config, String baseUrl, String externalUrlOverride, String baseMtlsUrl) {
		env.putString("base_url", baseUrl);
		env.putString("base_mtls_url", baseMtlsUrl);
		env.putObject("config", config);

		Boolean skip = env.getBoolean("config", "skip_test");
		if (skip != null && skip) {
			// This is intended for use in our CI where we insist all tests run to completion
			// It would be used as a temporary measure in an 'override' where one of the environments we are testing
			// against is not able to run the test to completion due to an issue in that environments.
			callAndContinueOnFailure(ConfigurationRequestsTestIsSkipped.class, Condition.ConditionResult.FAILURE);
			fireTestFinished();
			return;
		}

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

		if(addTokenEndpointAuthMethodSupported!=null) {
			callAndStopOnFailure(addTokenEndpointAuthMethodSupported);
		}
		adjustTokenEndpointInServerConfigurationIfUsingMtls();

		exposeEnvString("discoveryUrl");
		exposeEnvString("issuer");

		onServerConfigurationCompleted();

		configureServerJWKS();

		validateConfiguredServerJWKS();

		configureUserInfo();

		configureClientConfiguration();

		onBeforeFireSetupDone();

		if(clientRegistrationType==ClientRegistration.STATIC_CLIENT) {
			setServerSigningAlgorithm();
			callAndStopOnFailure(SetClientIdTokenSignedResponseAlgToServerSigningAlg.class);
		}

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
				callAndStopOnFailure(OIDCCAddRequestObjectSigningAlgValuesSupportedToServerConfiguration.class, "OIDCC-6.1");
				break;
			case REQUEST_URI:
				callAndStopOnFailure(SetRequestUriParameterSupportedToTrueInServerConfiguration.class, "OIDCC-6.2");
				break;
			case PLAIN_HTTP_REQUEST:
				// nothing to do
				break;
		}
	}

	/**
	 * no-op unless client auth type is SELF_SIGNED_TLS_CLIENT_AUTH or TLS_CLIENT_AUTH
	 */
	protected void adjustTokenEndpointInServerConfigurationIfUsingMtls() {
		if(clientAuthType==OIDCCClientAuthType.SELF_SIGNED_TLS_CLIENT_AUTH || clientAuthType==OIDCCClientAuthType.TLS_CLIENT_AUTH) {
			callAndStopOnFailure(ChangeTokenEndpointInServerConfigurationToMtls.class);
		}
	}

	/**
	 * override to modify the generated jwks
	 */
	protected void configureServerJWKS() {
		callAndStopOnFailure(OIDCCGenerateServerJWKs.class);
	}

	protected void configureUserInfo() {
		callAndStopOnFailure(OIDCCLoadUserInfo.class);
	}

	protected void configureClientConfiguration() {
		if(clientRegistrationType == ClientRegistration.STATIC_CLIENT) {
			callAndStopOnFailure(OIDCCGetStaticClientConfigurationForRPTests.class);
			processAndValidateClientJwks();
			validateClientMetadata();
		} else if(clientRegistrationType == ClientRegistration.DYNAMIC_CLIENT) {
			// I am not sure the result of either of these condition calls is used
			callAndContinueOnFailure(StoreOriginalClientConfiguration.class, ConditionResult.INFO);
			callAndStopOnFailure(ExtractClientNameFromStoredConfig.class);
			//for dynamic clients, jwks_uri retrieval and jwks validation will be performed after registration
			//signing_algorithm will be also set after registration
		}

	}

	protected boolean isClientJwksNeeded() {
		//or clientAuthType == ClientAuthType.self_signed_tls_client_auth
		if( clientAuthType == OIDCCClientAuthType.PRIVATE_KEY_JWT || clientAuthType == OIDCCClientAuthType.SELF_SIGNED_TLS_CLIENT_AUTH) {
			return true;
		}

		JsonObject client = env.getObject("client");

		if(clientRequestType == ClientRequestType.REQUEST_OBJECT || clientRequestType == ClientRequestType.REQUEST_URI) {
			if(client.has("request_object_signing_alg")) {
				String requestObjectSigningAlg = OIDFJSON.getString(client.get("request_object_signing_alg"));
				if(requestObjectSigningAlg!=null
					&& !"none".equals(requestObjectSigningAlg)
					&& JWSUtil.isAsymmetricJWSAlgorithm(requestObjectSigningAlg))
				{
					return true;
				}
			} else {
				/*
					request_object_signing_alg
					OPTIONAL. JWS [JWS] alg algorithm [JWA] that MUST be used for signing Request Objects sent to the OP.
					...The default, if omitted, is that any algorithm supported by the OP and the RP MAY be used...
				 */
				//as per the above, jwks may or may not be needed, we can't know this until we process a request_object
				//this may lead to a failure later in the test due to missing client_public_jwks
			}
		}

		if(client.has("id_token_encrypted_response_alg")) {
			String idTokenEncRespAlg = OIDFJSON.getString(client.get("id_token_encrypted_response_alg"));
			if (idTokenEncRespAlg != null && JWEUtil.isAsymmetricJWEAlgorithm(idTokenEncRespAlg)) {
				return true;
			}
		}

		if(client.has("userinfo_encrypted_response_alg")) {
			String userinfoEncRespAlg = OIDFJSON.getString(client.get("userinfo_encrypted_response_alg"));
			if (userinfoEncRespAlg != null && JWEUtil.isAsymmetricJWEAlgorithm(userinfoEncRespAlg)) {
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
	public Object handleHttpMtls(String path, HttpServletRequest req, HttpServletResponse res, HttpSession session, JsonObject requestParts) {

		setStatus(Status.RUNNING);

		String requestId = "incoming_request_" + RandomStringUtils.secure().nextAlphanumeric(37);

		env.putObject(requestId, requestParts);

		call(exec().mapKey("client_request", requestId));

		validateTlsForIncomingHttpRequest();

		call(exec().unmapKey("client_request"));

		Object responseObject = null;
		if (path.equals("token")) {
			responseObject = handleTokenEndpointRequest(requestId);
		} else {
			throw new TestFailureException(getId(), "Got unexpected MTLS HTTP call to " + path);
		}
		if (!finishTestIfAllRequestsAreReceived()) {
			setStatus(Status.WAITING);
		}
		return responseObject;
	}

	/**
	 * Override to randomize jwks path
	 * @return
	 */
	protected String getJwksPath() {
		return "jwks";
	}


	@Override
	public Object handleHttp(String path, HttpServletRequest req, HttpServletResponse servletResponse, HttpSession session, JsonObject requestParts) {

		if(getStatus()==Status.FINISHED && path.equals("jwks")) {
			//TODO temporary fix, until a finish-test endpoint is added
			//don't change state. we finish the test after userinfo but clients may send
			//a request to jwks endpoint when userinfo response is signed
		} else {
			setStatus(Status.RUNNING);
		}

		String requestId = "incoming_request_" + RandomStringUtils.secure().nextAlphanumeric(37);

		env.putObject(requestId, requestParts);

		call(exec().mapKey("client_request", requestId));

		validateTlsForIncomingHttpRequest();

		call(exec().unmapKey("client_request"));

		Object responseObject = handleClientRequestForPath(requestId, path, servletResponse);

		if(getStatus()==Status.FINISHED && path.equals(getJwksPath())) {
			//TODO temporary fix, until a finish-test endpoint is added
			//we want to allow jwks calls after the test is finished
		} else {
			if (!finishTestIfAllRequestsAreReceived()) {
				setStatus(Status.WAITING);
			}
		}

		return responseObject;
	}

	protected void validateTlsForIncomingHttpRequest() {
	}

	protected Object handleClientRequestForPath(String requestId, String path, HttpServletResponse servletResponse){

		if (path.equals("authorize")) {
			checkIfDiscoveryCalled(path);
			receivedAuthorizationRequest = true;
			return handleAuthorizationEndpointRequest(requestId);

		} else if (path.equals("token")) {
			checkIfDiscoveryCalled(path);
			receivedTokenRequest = true;
			return handleTokenEndpointRequest(requestId);

		} else if (path.equals(getJwksPath())) {
			checkIfDiscoveryCalled(path);
			receivedJwksRequest = true;
			return handleJwksEndpointRequest();

		} else if (path.equals("userinfo")) {
			checkIfDiscoveryCalled(path);
			checkIfJWKCalled(path);
			receivedUserinfoRequest = true;
			return handleUserinfoEndpointRequest(requestId);

		} else if (path.equals("register") && clientRegistrationType == ClientRegistration.DYNAMIC_CLIENT) {
			checkIfDiscoveryCalled(path);
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
		call(exec().startBlock("Discovery endpoint"));
		JsonObject serverConfiguration = env.getObject("server");
		call(exec().endBlock());
		return new ResponseEntity<Object>(serverConfiguration, HttpStatus.OK);
	}


	protected Object handleUserinfoEndpointRequest(String requestId) {

		call(exec().startBlock("Userinfo endpoint").mapKey("incoming_request", requestId));

		validateUserinfoRequest();

		JsonObject user = prepareUserinfoResponse();

		callAndStopOnFailure(ClearAccessTokenFromRequest.class);

		signUserInfoResponseIfNecessary();

		encryptUserInfoResponseIfNecessary();

		call(exec().unmapKey("incoming_request").endBlock());

		String encryptedUserinfoResponse = env.getString("encrypted_user_info_endpoint_response");
		//If the UserInfo Response is signed and/or encrypted, then the Claims are returned in a
		//JWT and the content-type MUST be application/jwt.
		if(encryptedUserinfoResponse!=null) {
			return createApplicationJwtResponse(encryptedUserinfoResponse);
		} else {
			String signedUserinfoResponse = env.getString("signed_user_info_endpoint_response");
			if(signedUserinfoResponse!=null) {
				return createApplicationJwtResponse(signedUserinfoResponse);
			}
		}
		//neither signed nor encrypted
		return new ResponseEntity<Object>(user, HttpStatus.OK);
	}

	protected void signUserInfoResponseIfNecessary() {
		//If signed, the UserInfo Response SHOULD contain the Claims iss (issuer) and aud (audience) as members.
		skipIfElementMissing("client", "userinfo_signed_response_alg", Condition.ConditionResult.INFO,
			AddIssAndAudToUserInfoResponse.class, Condition.ConditionResult.FAILURE, "OIDCC-5.3.2");

		skipIfElementMissing("client", "userinfo_signed_response_alg", Condition.ConditionResult.INFO,
			SignUserInfoResponse.class, Condition.ConditionResult.FAILURE, "OIDCC-5.3.2");
	}

	protected void encryptUserInfoResponseIfNecessary() {
		skipIfElementMissing("client", "userinfo_encrypted_response_alg", Condition.ConditionResult.INFO,
			EncryptUserInfoResponse.class, Condition.ConditionResult.FAILURE, "OIDCC-5.3.2");
	}

	protected ResponseEntity<Object> createApplicationJwtResponse(String body) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(DATAUTILS_MEDIATYPE_APPLICATION_JWT_UTF8);
		return new ResponseEntity<Object>(body, headers, HttpStatus.OK);
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
		callAndStopOnFailure(FilterUserInfoForScopes.class, "OIDCC-5.4");
		JsonObject user = env.getObject("user_info_endpoint_response");
		return user;
	}

	protected void validateUserinfoRequest() {
		extractBearerTokenFromUserinfoRequest();
		callAndStopOnFailure(RequireBearerAccessToken.class, "OIDCC-5.3.1");
	}


	protected void checkIfDiscoveryCalled(String path) {

	}

	protected void checkIfJWKCalled(String path) {

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
		call(exec().startBlock("Jwks endpoint"));
		JsonObject jwks = env.getObject("server_public_jwks");
		call(exec().endBlock());
		return new ResponseEntity<Object>(jwks, HttpStatus.OK);
	}

	protected Object handleTokenEndpointRequest(String requestId) {
		call(exec().mapKey("token_endpoint_request", requestId));

		String grantType = env.getString("token_endpoint_request", "body_form_params.grant_type");
		if (grantType == null) {
			throw new TestFailureException(getId(), "Token endpoint body does not contain the mandatory 'grant_type' parameter");
		}

		if ("refresh_token".equals(grantType)) {
			call(exec().startBlock("Token endpoint - Refresh Request"));
		} else {
			call(exec().startBlock("Token endpoint"));
		}

		validateTokenEndpointRequest();

		callAndContinueOnFailure(CheckClientIdMatchesOnTokenRequestIfPresent.class, Condition.ConditionResult.FAILURE, "RFC6749-3.2.1");

		if(validateClientAuthenticationSteps!=null) {
			call(sequence(validateClientAuthenticationSteps));
		}

		if ("authorization_code".equals(grantType)) {
			// we're doing the authorization code grant for user access
			return authorizationCodeGrantType(requestId);
		} else if ("refresh_token".equals(grantType)) {
			return refreshTokenGrantType(requestId);
		} else {
			throw new TestFailureException(getId(), "Got a grant type on the token endpoint we didn't understand: " + grantType);
		}
	}

	protected Object refreshTokenGrantType(String requestId) {
		throw new TestFailureException(getId(), "refresh_token grant type is not implemented for this test");
	}

	/**
	 * http request is mapped to "dynamic_registration_request" before this call
	 */
	protected void validateRegistrationRequest() {
		//because the python suite requires this
		callAndContinueOnFailure(EnsureRegistrationRequestContainsAtLeastOneContact.class, ConditionResult.INFO);

		//the following conditions are used for both static client validation and dynamic registration validation
		//so they require "client" env entry
		env.mapKey("client", "dynamic_registration_request");
		validateClientMetadata();
		env.unmapKey("client");
		callAndContinueOnFailure(ValidateClientRegistrationRequestSectorIdentifierUri.class,
									Condition.ConditionResult.FAILURE,"OIDCR-2","OIDCR-5");
	}

	/**
	 * jwks and jwks_uri will be validated in validateClientJwks
	 */
	protected void validateClientMetadata() {
		callAndContinueOnFailure(ValidateClientGrantTypes.class, Condition.ConditionResult.FAILURE, "OIDCR-2");
		callAndContinueOnFailure(OIDCCValidateClientRedirectUris.class, Condition.ConditionResult.FAILURE,
			"OIDCR-2");

		callAndContinueOnFailure(ValidateClientLogoUris.class, Condition.ConditionResult.FAILURE,"OIDCR-2");
		callAndContinueOnFailure(ValidateClientUris.class, Condition.ConditionResult.FAILURE,"OIDCR-2");
		callAndContinueOnFailure(ValidateClientPolicyUris.class, Condition.ConditionResult.FAILURE,"OIDCR-2");
		callAndContinueOnFailure(ValidateClientTosUris.class, Condition.ConditionResult.FAILURE,"OIDCR-2");

		callAndContinueOnFailure(ValidateClientSubjectType.class, Condition.ConditionResult.FAILURE,"OIDCR-2");
		skipIfElementMissing("client", "id_token_signed_response_alg", Condition.ConditionResult.INFO,
			ValidateIdTokenSignedResponseAlg.class, Condition.ConditionResult.FAILURE, "OIDCR-2");

		callAndContinueOnFailure(EnsureIdTokenEncryptedResponseAlgIsSetIfEncIsSet.class, Condition.ConditionResult.FAILURE,"OIDCR-2");

		//userinfo
		skipIfElementMissing("client", "userinfo_signed_response_alg", Condition.ConditionResult.INFO,
			ValidateUserinfoSignedResponseAlg.class, Condition.ConditionResult.FAILURE, "OIDCR-2");
		callAndContinueOnFailure(EnsureUserinfoEncryptedResponseAlgIsSetIfEncIsSet.class, Condition.ConditionResult.FAILURE,"OIDCR-2");

		//request object
		skipIfElementMissing("client", "request_object_signing_alg", Condition.ConditionResult.INFO,
			ValidateRequestObjectSigningAlg.class, Condition.ConditionResult.FAILURE, "OIDCR-2");
		callAndContinueOnFailure(EnsureRequestObjectEncryptionAlgIsSetIfEncIsSet.class, Condition.ConditionResult.FAILURE,"OIDCR-2");

		//not validating token_endpoint_auth_method as we will override it anyway

		skipIfElementMissing("client", "token_endpoint_auth_signing_alg", Condition.ConditionResult.INFO,
			ValidateTokenEndpointAuthSigningAlg.class, Condition.ConditionResult.FAILURE, "OIDCR-2");

		callAndContinueOnFailure(ValidateDefaultMaxAge.class, ConditionResult.WARNING,"OIDCR-2");

		skipIfElementMissing("client", "require_auth_time", Condition.ConditionResult.INFO,
			ValidateRequireAuthTime.class, Condition.ConditionResult.FAILURE, "OIDCR-2");

		skipIfElementMissing("client", "default_acr_values", Condition.ConditionResult.INFO,
			ValidateDefaultAcrValues.class, Condition.ConditionResult.FAILURE, "OIDCR-2");

		skipIfElementMissing("client", "initiate_login_uri", Condition.ConditionResult.INFO,
			ValidateInitiateLoginUri.class, Condition.ConditionResult.FAILURE, "OIDCR-2");

		skipIfElementMissing("client", "request_uris", Condition.ConditionResult.INFO,
			ValidateRequestUris.class, Condition.ConditionResult.FAILURE, "OIDCR-2");
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

		//set signing_algorithm after registration
		setServerSigningAlgorithm();
		//set id_token_signed_response_alg to the actual server signing algorithm
		callAndStopOnFailure(SetClientIdTokenSignedResponseAlgToServerSigningAlg.class);

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
		callAndStopOnFailure(EnsureClientDoesNotHaveBothJwksAndJwksUri.class, "OIDCR-2");

		//fetch client jwks from jwks_uri, if a jwks_uri is found
		fetchClientJwksFromJwksUri();

		//at this point jwks has been downloaded from jwks_uri and added to client.jwks
		JsonObject client = env.getObject("client");
		if(client.has("jwks")) {
			callAndStopOnFailure(ExtractJWKsFromStaticClientConfiguration.class);
			validateClientJwks();
		}
	}

	protected void validateClientJwks() {
		callAndStopOnFailure(ValidateClientJWKsPublicPart.class, "RFC7517-1.1");
		callAndContinueOnFailure(CheckDistinctKeyIdValueInClientJWKs.class, Condition.ConditionResult.FAILURE, "RFC7517-4.5");
		callAndContinueOnFailure(EnsureClientJwksDoesNotContainPrivateOrSymmetricKeys.class, Condition.ConditionResult.FAILURE, "RFC7517-9.2");
	}


	/**
	 * from this point on the client will contain both jwks and jwks_uri
	 */
	protected void fetchClientJwksFromJwksUri() {
		skipIfElementMissing("client", "jwks_uri", Condition.ConditionResult.INFO, FetchClientKeys.class,
			Condition.ConditionResult.FAILURE, "OIDCC-10.1.1", "OIDCC-10.2.1");
	}

	protected void validateAuthorizationCodeGrantType() {
		callAndStopOnFailure(ValidateAuthorizationCode.class, "OIDCC-3.1.3.2");

		callAndContinueOnFailure(ValidateRedirectUriForTokenEndpointRequest.class, Condition.ConditionResult.FAILURE, "OIDCC-3.1.3.2");

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

		if(isAuthorizationCodeGrantType || responseType.includesIdToken()) {
			addAuthTimeToIdToken();
		}

		addCustomValuesToIdToken();

		signIdToken();

		customizeIdTokenSignature();

		encryptIdTokenIfNecessary();
	}

	protected void encryptIdTokenIfNecessary() {
		skipIfElementMissing("client", "id_token_encrypted_response_alg", Condition.ConditionResult.INFO,
			EncryptIdToken.class, Condition.ConditionResult.FAILURE, "OIDCC-10.2");
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

	protected void addAuthTimeToIdToken() {
		skipIfElementMissing("effective_authorization_endpoint_request", "max_age", Condition.ConditionResult.INFO,
			AddAuthTimeToIdTokenClaims.class, Condition.ConditionResult.FAILURE, "OIDCC-3.1.2.1");
	}

	protected Object authorizationCodeGrantType(String requestId) {

		validateAuthorizationCodeGrantType();

		if(env.containsObject("code_challenge")) {
			call(sequence(CheckPkceCodeVerifier.class));
		}

		generateAccessToken();

		createIdToken(true);

		createRefreshToken(false);

		callAndStopOnFailure(CreateTokenEndpointResponse.class, "OIDCC-3.1.3.3");

		call(exec().unmapKey("token_endpoint_request").endBlock());

		return new ResponseEntity<Object>(env.getObject("token_endpoint_response"), HttpStatus.OK);
	}

	/**
	 * does nothing by default. to be overridden in refresh token tests
	 * @param isRefreshTokenGrant
	 */
	protected void createRefreshToken(boolean isRefreshTokenGrant) {
	}

	protected void generateIdTokenClaims() {
		callAndStopOnFailure(GenerateIdTokenClaims.class);
	}

	protected void signIdToken() {
		callAndStopOnFailure(OIDCCSignIdToken.class, "OIDCC-2");
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

		callAndStopOnFailure(EnsureAuthorizationHttpRequestContainsOpenIDScope.class, "OIDCC-6.1", "OIDCC-6.2");

		if(clientRequestType == ClientRequestType.REQUEST_OBJECT || clientRequestType == ClientRequestType.REQUEST_URI) {
			validateRequestObject();
			callAndStopOnFailure(EnsureRequiredAuthorizationRequestParametersMatchRequestObject.class, "OIDCC-6.1", "OIDCC-6.2");
			skipIfElementMissing("authorization_request_object", "jwe_header", Condition.ConditionResult.INFO, ValidateEncryptedRequestObjectHasKid.class, Condition.ConditionResult.FAILURE, "OIDCC-10.2", "OIDCC-10.2.1");
			callAndContinueOnFailure(EnsureOptionalAuthorizationRequestParametersMatchRequestObject.class,
										Condition.ConditionResult.WARNING, "OIDCC-6.1", "OIDCC-6.2");
		}

		callAndStopOnFailure(CreateEffectiveAuthorizationRequestParameters.class, "OIDCC-6.1", "OIDCC-6.2");

		callAndStopOnFailure(ExtractRequestedScopes.class);

		extractNonceFromAuthorizationEndpointRequestParameters();

		skipIfElementMissing(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, CreateEffectiveAuthorizationRequestParameters.CODE_CHALLENGE, Condition.ConditionResult.INFO, EnsureAuthorizationRequestContainsPkceCodeChallenge.class, Condition.ConditionResult.FAILURE, "RFC7636-4.3");
	}

	protected void extractNonceFromAuthorizationEndpointRequestParameters() {
		String responseType = env.getString(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, CreateEffectiveAuthorizationRequestParameters.RESPONSE_TYPE);
		if (responseType != null && responseType.contains("id_token")) {
			callAndStopOnFailure(ExtractNonceFromAuthorizationRequest.class, ConditionResult.FAILURE, "OIDCC-3.1.2.1", "OIDCC-3.2.2.1");
		} else {
			callAndContinueOnFailure(ExtractNonceFromAuthorizationRequest.class, Condition.ConditionResult.INFO, "OIDCC-3.1.2.1");
		}
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

		disallowMaxAge0AndPromptNone();
	}

	/**
	 * To be overridden in OIDCCClientTestFormPostError
	 * or any similar classes that want to trigger an error
	 * by allowing max_age=0 and prompt=none
	 */
	protected void disallowMaxAge0AndPromptNone() {
		callAndStopOnFailure(DisallowMaxAgeEqualsZeroAndPromptNone.class, "OIDCC-3.1.2.3");
	}

	protected void validateRequestObject() {
		skipIfElementMissing("authorization_request_object", "claims.exp", Condition.ConditionResult.INFO,
			OIDCCValidateRequestObjectExp.class, Condition.ConditionResult.FAILURE, "RFC7519-4.1.4");
		callAndContinueOnFailure(ValidateRequestObjectIat.class, Condition.ConditionResult.WARNING, "OIDCC-6.1");
		callAndContinueOnFailure(EnsureNumericRequestObjectClaimsAreNotNull.class, Condition.ConditionResult.WARNING, "OIDCC-13.3");
		callAndContinueOnFailure(ValidateRequestObjectMaxAge.class, Condition.ConditionResult.FAILURE, "OIDCC-13.3");
		callAndContinueOnFailure(EnsureRequestObjectDoesNotContainRequestOrRequestUri.class, ConditionResult.WARNING, "OIDCC-6.1");
		callAndContinueOnFailure(EnsureRequestObjectDoesNotContainSubWithClientId.class, ConditionResult.WARNING, "JAR-10.8");

		String alg = env.getString("authorization_request_object", "header.alg");

		if(allowUnsignedRequestObjects() && "none".equals(alg)) {
			//Nimbusds will throw an exception if a request object with alg:none contains a signature
		} else {
			//https://openid.net/specs/openid-connect-core-1_0.html#RequestObject
			// The Request Object MAY be signed or unsigned (plaintext).
			// When it is plaintext, this is indicated by use of the none algorithm [JWA] in the JOSE Header.
			// If signed, the Request Object SHOULD contain the Claims iss (issuer) and aud (audience) as members.
			// The iss value SHOULD be the Client ID of the RP, unless it was signed by a different party than the RP.
			// The aud value SHOULD be or include the OP's Issuer Identifier URL.
			callAndContinueOnFailure(ValidateRequestObjectIss.class, Condition.ConditionResult.WARNING, "OIDCC-6.1");
			callAndContinueOnFailure(ValidateRequestObjectAud.class, Condition.ConditionResult.WARNING, "OIDCC-6.1");

			//This may happen when the client does not contain both request_object_signing_alg and jwks/jwks_uri
			//and a signed request object is received. We can't validate the signature.
			//Using skipIfMissing to avoid an ugly missing required environment entry error thrown by the framework
			skipIfMissing(new String[]{"client_public_jwks"}, null, Condition.ConditionResult.FAILURE,
				ValidateRequestObjectSignature.class, Condition.ConditionResult.FAILURE, "OIDCC-6.1");
		}
	}

	/**
	 * Override to disallow unsigned request objects.
	 * By default they are allowed
	 * @return
	 */
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
		callAndStopOnFailure(OIDCCExtractServerSigningAlg.class);
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

	protected String getAuthorizationEndpointBlockText() {
		return "Authorization endpoint";
	}

	@UserFacing
	protected Object handleAuthorizationEndpointRequest(String requestId) {

		call(exec().startBlock(getAuthorizationEndpointBlockText()).mapKey("authorization_endpoint_http_request", requestId));
		setAuthorizationEndpointRequestParamsForHttpMethod();

		extractAuthorizationEndpointRequestParameters();

		validateAuthorizationEndpointRequestParameters();

		skipIfElementMissing("authorization_request_object", "claims", ConditionResult.INFO,
			CheckForUnexpectedClaimsInRequestObject.class, ConditionResult.WARNING, "RFC6749-4.1.1", "OIDCC-3.1.2.1", "RFC7636-4.3", "OAuth2-RT-2.1", "RFC7519-4.1", "DPOP-10", "RFC8485-4.1", "RFC8707-2.1", "RFC9396-2");

		skipIfElementMissing("authorization_request_object", "claims.claims", ConditionResult.INFO,
			CheckForUnexpectedClaimsInClaimsParameter.class, ConditionResult.WARNING, "OIDCC-5.5");
		skipIfElementMissing("authorization_request_object", "claims.claims", ConditionResult.INFO,
			CheckForUnexpectedOpenIdClaims.class, ConditionResult.WARNING, "OIDCC-5.1", "OIDCC-5.5.1.1", "BrazilOB-5.2.2.3", "BrazilOB-5.2.2.4", "OBSP-3.4");
		skipIfElementMissing("authorization_request_object", "claims.claims", ConditionResult.INFO,
			CheckRequestObjectClaimsParameterValues.class, ConditionResult.FAILURE, "OIDCC-5.5");
		skipIfElementMissing("authorization_request_object", "claims.claims", ConditionResult.INFO,
			CheckRequestObjectClaimsParameterMemberValues.class, ConditionResult.FAILURE, "OIDCC-5.5.1");

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


		customizeAuthorizationEndpointResponseParams();

		Object viewToReturn = null;
		if(responseMode.isFormPost()) {

			viewToReturn = generateFormPostResponse();

		} else {

			redirectFromAuthorizationEndpoint();

			exposeEnvString("authorization_endpoint_response_redirect");

			String redirectTo = env.getString("authorization_endpoint_response_redirect");

			viewToReturn = new RedirectView(redirectTo, false, false, false);
		}

		env.putString("auth_time", Long.toString(Instant.now().getEpochSecond()));

		call(exec().unmapKey("authorization_endpoint_http_request").endBlock());
		return viewToReturn;
	}

	/**
	 * Called right before the response is generated
	 * Override to customize response parameters
	 */
	protected void customizeAuthorizationEndpointResponseParams() {

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

	@VariantSetup(parameter = OIDCCClientAuthType.class, value = "none")
	public void setupClientAuthNone() {
		addTokenEndpointAuthMethodSupported = null;
		validateClientAuthenticationSteps = OIDCCValidateClientAuthenticationWithNone.class;
		clientRegistrationSteps = OIDCCRegisterClientWithNone.class;
	}

	@VariantSetup(parameter = OIDCCClientAuthType.class, value = "private_key_jwt")
	public void setupPrivateKeyJwt() {
		addTokenEndpointAuthMethodSupported = SetTokenEndpointAuthMethodsSupportedToPrivateKeyJWTOnly.class;
		validateClientAuthenticationSteps = ValidateClientAuthenticationWithPrivateKeyJWT.class;
		clientRegistrationSteps = OIDCCRegisterClientWithPrivateKeyJwt.class;
	}

	@VariantSetup(parameter = OIDCCClientAuthType.class, value = "client_secret_basic")
	public void setupClientSecretBasic() {
		addTokenEndpointAuthMethodSupported = SetTokenEndpointAuthMethodsSupportedToClientSecretBasicOnly.class;
		validateClientAuthenticationSteps = OIDCCValidateClientAuthenticationWithClientSecretBasic.class;
		clientRegistrationSteps = OIDCCRegisterClientWithClientSecretBasic.class;
	}

	@VariantSetup(parameter = OIDCCClientAuthType.class, value = "client_secret_jwt")
	public void setupClientSecretJWT() {
		addTokenEndpointAuthMethodSupported = SetTokenEndpointAuthMethodsSupportedToClientSecretJWTOnly.class;
		validateClientAuthenticationSteps = OIDCCValidateClientAuthenticationWithClientSecretJWT.class;
		clientRegistrationSteps = OIDCCRegisterClientWithClientSecretJwt.class;
	}

	@VariantSetup(parameter = OIDCCClientAuthType.class, value = "client_secret_post")
	public void setupClientSecretPost() {
		addTokenEndpointAuthMethodSupported = SetTokenEndpointAuthMethodsSupportedToClientSecretPostOnly.class;
		validateClientAuthenticationSteps = OIDCCValidateClientAuthenticationWithClientSecretPost.class;
		clientRegistrationSteps = OIDCCRegisterClientWithClientSecretPost.class;
	}

	@VariantSetup(parameter = OIDCCClientAuthType.class, value = "tls_client_auth")
	public void setupTlsClientAuth() {
		addTokenEndpointAuthMethodSupported = SetTokenEndpointAuthMethodsSupportedToTlsClientAuthOnly.class;
		validateClientAuthenticationSteps = OIDCCValidateClientAuthenticationWithTlsClientAuth.class;
		clientRegistrationSteps = OIDCCRegisterClientWithTlsClientAuth.class;
	}

	@VariantSetup(parameter = OIDCCClientAuthType.class, value = "self_signed_tls_client_auth")
	public void setupSelfSignedTlsClientAuth() {
		addTokenEndpointAuthMethodSupported = SetTokenEndpointAuthMethodsSupportedToSelfSignedTlsClientAuthOnly.class;
		validateClientAuthenticationSteps = OIDCCValidateClientAuthenticationWithSelfSignedTlsClientAuth.class;
		clientRegistrationSteps = OIDCCRegisterClientWithSelfSignedTlsClientAuth.class;
	}

	/**
	 * Only use in tests that need to wait for a timeout
	 * As the client hasn't called an endpoint after waitTimeoutSeconds (from configuration) seconds,
	 * assume it has correctly detected the error and aborted.
	 */
	protected void startWaitingForTimeout() {
		getTestExecutionManager().runInBackground(() -> {
			Thread.sleep(waitTimeoutSeconds * 1000L);
			if (getStatus().equals(Status.WAITING)) {
				setStatus(Status.RUNNING);
				fireTestFinished();
			}
			return "done";
		});
	}

	/**
	 * override to validate the webfinger resource
	 * @param resourcePrefix
	 */
	protected void validateWebfingerRequestResource(String resourcePrefix) {
	}
	/**
	 *
	 * @param resourcePrefix can be acct or https
	 * @return
	 */
	public Object handleWebfingerRequest(String requestedTestName, String resourcePrefix, String resource, JsonObject requestParts) {
		setStatus(Status.RUNNING);
		call(exec().startBlock("Webfinger Request"));
		//this should not happen but just in case
		if(!this.getName().equals(requestedTestName)) {
			throw new TestFailureException(getId(),
				"Test name in webfinger request does not match current test name. " +
					"Requested=" + requestedTestName+ " actual=" + this.getName());
		}
		validateWebfingerRequestResource(resourcePrefix);
		env.putObject("incoming_webfinger_request", requestParts);
		env.putString("incoming_webfinger_resource", resource);
		callAndStopOnFailure(CreateWebfingerResponse.class, "OIDCD-2");
		call(exec().endBlock());
		setStatus(Status.WAITING);
		return env.getObject("webfinger_response");
	}
}
