package net.openid.conformance.fapi1advancedfinal;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.as.AddACRClaimToIdTokenClaims;
import net.openid.conformance.condition.as.AddAtHashToIdTokenClaims;
import net.openid.conformance.condition.as.AddCHashToIdTokenClaims;
import net.openid.conformance.condition.as.AddClaimsParameterSupportedTrueToServerConfiguration;
import net.openid.conformance.condition.as.AddCodeToAuthorizationEndpointResponseParams;
import net.openid.conformance.condition.as.AddIdTokenToAuthorizationEndpointResponseParams;
import net.openid.conformance.condition.as.AddSHashToIdTokenClaims;
import net.openid.conformance.condition.as.AddTLSClientAuthToServerConfiguration;
import net.openid.conformance.condition.as.FAPIAddTokenEndpointAuthSigningAlgValuesSupportedToServer;
import net.openid.conformance.condition.as.CalculateAtHash;
import net.openid.conformance.condition.as.CalculateCHash;
import net.openid.conformance.condition.as.CalculateSHash;
import net.openid.conformance.condition.as.CheckForClientCertificate;
import net.openid.conformance.condition.as.CopyAccessTokenToClientCredentialsField;
import net.openid.conformance.condition.as.CreateAuthorizationCode;
import net.openid.conformance.condition.as.CreateAuthorizationEndpointResponseParams;
import net.openid.conformance.condition.as.CreateEffectiveAuthorizationRequestParameters;
import net.openid.conformance.condition.as.CreateFapiInteractionIdIfNeeded;
import net.openid.conformance.condition.as.CreateRefreshToken;
import net.openid.conformance.condition.as.EncryptJARMResponse;
import net.openid.conformance.condition.as.EnsureAuthorizationHttpRequestContainsOpenIDScope;
import net.openid.conformance.condition.as.EnsureClientIdInAuthorizationRequestParametersMatchRequestObject;
import net.openid.conformance.condition.as.EnsureAuthorizationRequestContainsStateParameter;
import net.openid.conformance.condition.as.EnsureRequestObjectWasEncrypted;
import net.openid.conformance.condition.as.EnsureRequestedScopeIsEqualToConfiguredScope;
import net.openid.conformance.condition.as.EnsureResponseTypeIsCode;
import net.openid.conformance.condition.as.EnsureOptionalAuthorizationRequestParametersMatchRequestObject;
import net.openid.conformance.condition.as.EnsureRequiredAuthorizationRequestParametersMatchRequestObject;
import net.openid.conformance.condition.as.FAPIBrazilAddACRClaimToIdTokenClaims;
import net.openid.conformance.condition.as.FAPIBrazilAddBrazilSpecificSettingsToServerConfiguration;
import net.openid.conformance.condition.as.FAPIBrazilAddCPFAndCPNJToIdTokenClaims;
import net.openid.conformance.condition.as.FAPIBrazilAddCPFAndCPNJToUserInfoClaims;
import net.openid.conformance.condition.as.FAPIBrazilAddTokenEndpointAuthSigningAlgValuesSupportedToServer;
import net.openid.conformance.condition.as.FAPIBrazilChangeConsentStatusToAuthorized;
import net.openid.conformance.condition.as.FAPIBrazilEnsureRequestObjectEncryptedUsingRSAOAEPA256GCM;
import net.openid.conformance.condition.as.FAPIBrazilEnsureRequestedScopeContainsConsents;
import net.openid.conformance.condition.as.FAPIBrazilExtractConsentRequest;
import net.openid.conformance.condition.as.FAPIBrazilSetGrantTypesSupportedInServerConfiguration;
import net.openid.conformance.condition.as.FAPIBrazilValidateConsentScope;
import net.openid.conformance.condition.as.SetServerSigningAlgToPS256;
import net.openid.conformance.condition.as.ValidateCodeVerifierWithS256;
import net.openid.conformance.condition.as.ValidateRefreshToken;
import net.openid.conformance.condition.as.jarm.GenerateJARMResponseClaims;
import net.openid.conformance.condition.as.jarm.SendJARMResponseWitResponseModeQuery;
import net.openid.conformance.condition.as.jarm.SignJARMResponse;
import net.openid.conformance.condition.as.par.CreatePAREndpointResponse;
import net.openid.conformance.condition.as.CreateTokenEndpointResponse;
import net.openid.conformance.condition.as.par.EnsureAuthorizationRequestDoesNotContainRequestWhenUsingPAR;
import net.openid.conformance.condition.as.EnsureClientCertificateMatches;
import net.openid.conformance.condition.as.EnsureClientJwksDoesNotContainPrivateOrSymmetricKeys;
import net.openid.conformance.condition.as.EnsureMatchingClientId;
import net.openid.conformance.condition.as.EnsureMatchingRedirectUriInRequestObject;
import net.openid.conformance.condition.as.EnsureNumericRequestObjectClaimsAreNotNull;
import net.openid.conformance.condition.as.EnsureOpenIDInScopeRequest;
import net.openid.conformance.condition.as.EnsurePAREndpointRequestDoesNotContainRequestUriParameter;
import net.openid.conformance.condition.as.EnsureRequestObjectDoesNotContainRequestOrRequestUri;
import net.openid.conformance.condition.as.EnsureRequestObjectDoesNotContainSubWithClientId;
import net.openid.conformance.condition.as.EnsureResponseTypeIsCodeIdToken;
import net.openid.conformance.condition.as.ExtractClientCertificateFromTokenEndpointRequestHeaders;
import net.openid.conformance.condition.as.ExtractNonceFromAuthorizationRequest;
import net.openid.conformance.condition.as.ExtractRequestObject;
import net.openid.conformance.condition.as.par.EnsureRequestObjectContainsCodeChallengeWhenUsingPAR;
import net.openid.conformance.condition.as.par.ExtractRequestObjectFromPAREndpointRequest;
import net.openid.conformance.condition.as.ExtractRequestedScopes;
import net.openid.conformance.condition.as.ExtractServerSigningAlg;
import net.openid.conformance.condition.as.FAPIEnsureMinimumClientKeyLength;
import net.openid.conformance.condition.as.FAPIEnsureMinimumServerKeyLength;
import net.openid.conformance.condition.as.FAPIValidateRequestObjectExp;
import net.openid.conformance.condition.as.FAPIValidateRequestObjectSigningAlg;
import net.openid.conformance.condition.as.FilterUserInfoForScopes;
import net.openid.conformance.condition.as.GenerateBearerAccessToken;
import net.openid.conformance.condition.as.GenerateIdTokenClaims;
import net.openid.conformance.condition.as.GenerateServerConfigurationMTLS;
import net.openid.conformance.condition.as.LoadServerJWKs;
import net.openid.conformance.condition.as.SendAuthorizationResponseWithResponseModeFragment;
import net.openid.conformance.condition.as.SetTokenEndpointAuthMethodsSupportedToPrivateKeyJWTOnly;
import net.openid.conformance.condition.as.SignIdToken;
import net.openid.conformance.condition.as.ValidateAuthorizationCode;
import net.openid.conformance.condition.as.ValidateRedirectUri;
import net.openid.conformance.condition.as.ValidateRequestObjectClaims;
import net.openid.conformance.condition.as.FAPI1AdvancedValidateRequestObjectNBFClaim;
import net.openid.conformance.condition.as.ValidateRequestObjectSignature;
import net.openid.conformance.condition.client.ExtractJWKsFromStaticClientConfiguration;
import net.openid.conformance.condition.client.FAPIBrazilValidateRequestObjectIdTokenACRClaims;
import net.openid.conformance.condition.client.FAPIValidateRequestObjectIdTokenACRClaims;
import net.openid.conformance.condition.client.GetStaticClient2Configuration;
import net.openid.conformance.condition.client.GetStaticClientConfiguration;
import net.openid.conformance.condition.client.ValidateClientJWKsPublicPart;
import net.openid.conformance.condition.client.ValidateServerJWKs;
import net.openid.conformance.condition.common.CheckDistinctKeyIdValueInClientJWKs;
import net.openid.conformance.condition.common.CheckServerConfiguration;
import net.openid.conformance.condition.common.EnsureIncomingTls12WithSecureCipherOrTls13;
import net.openid.conformance.condition.rs.ClearAccessTokenFromRequest;
import net.openid.conformance.condition.rs.CreateFAPIAccountEndpointResponse;
import net.openid.conformance.condition.rs.CreateOpenBankingAccountRequestResponse;
import net.openid.conformance.condition.rs.EnsureBearerAccessTokenNotInParams;
import net.openid.conformance.condition.rs.ExtractBearerAccessTokenFromHeader;
import net.openid.conformance.condition.rs.ExtractFapiDateHeader;
import net.openid.conformance.condition.rs.ExtractFapiIpAddressHeader;
import net.openid.conformance.condition.rs.FAPIBrazilGenerateGetConsentResponse;
import net.openid.conformance.condition.rs.FAPIBrazilGenerateNewConsentResponse;
import net.openid.conformance.condition.rs.GenerateAccountRequestId;
import net.openid.conformance.condition.rs.LoadUserInfo;
import net.openid.conformance.condition.rs.RequireBearerAccessToken;
import net.openid.conformance.condition.rs.RequireBearerClientCredentialsAccessToken;
import net.openid.conformance.condition.rs.RequireOpenIDScope;
import net.openid.conformance.runner.TestDispatcher;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.sequence.as.AddJARMToServerConfiguration;
import net.openid.conformance.sequence.as.AddOpenBankingUkClaimsToAuthorizationCodeGrant;
import net.openid.conformance.sequence.as.AddOpenBankingUkClaimsToAuthorizationEndpointResponse;
import net.openid.conformance.sequence.as.AddPARToServerConfiguration;
import net.openid.conformance.sequence.as.AddPlainFAPIToServerConfiguration;
import net.openid.conformance.sequence.as.GenerateOpenBankingUkAccountsEndpointResponse;
import net.openid.conformance.sequence.as.ValidateClientAuthenticationWithMTLS;
import net.openid.conformance.sequence.as.ValidateClientAuthenticationWithPrivateKeyJWT;
import net.openid.conformance.testmodule.AbstractTestModule;
import net.openid.conformance.testmodule.TestFailureException;
import net.openid.conformance.testmodule.UserFacing;
import net.openid.conformance.variant.ClientAuthType;
import net.openid.conformance.variant.FAPI1FinalOPProfile;
import net.openid.conformance.variant.FAPIAuthRequestMethod;
import net.openid.conformance.variant.FAPIJARMType;
import net.openid.conformance.variant.FAPIResponseMode;
import net.openid.conformance.variant.VariantHidesConfigurationFields;
import net.openid.conformance.variant.VariantNotApplicable;
import net.openid.conformance.variant.VariantParameters;
import net.openid.conformance.variant.VariantSetup;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@VariantParameters({
	ClientAuthType.class,
	FAPI1FinalOPProfile.class,
	FAPIAuthRequestMethod.class,
	FAPIResponseMode.class,
	FAPIJARMType.class
})
@VariantNotApplicable(parameter = ClientAuthType.class, values = {
	"none", "client_secret_basic", "client_secret_post", "client_secret_jwt"
})
@VariantHidesConfigurationFields(parameter = FAPIResponseMode.class, value = "jarm", configurationFields = {
	"client2.client_id",
	"client2.scope",
	"client2.redirect_uri",
	"client2.certificate",
	"client2.jwks",
	"client2.id_token_encrypted_response_alg",
	"client2.id_token_encrypted_response_enc",
})
@VariantHidesConfigurationFields(parameter = FAPI1FinalOPProfile.class, value = "openbanking_brazil", configurationFields = {
	"client.scope",
	"client2.scope"
})
public abstract class AbstractFAPI1AdvancedFinalClientTest extends AbstractTestModule {

	public static final String ACCOUNT_REQUESTS_PATH = "open-banking/v1.1/account-requests";
	public static final String ACCOUNTS_PATH = "open-banking/v1.1/accounts";
	public static final String BRAZIL_CONSENTS_PATH = "consents/v1/consents";
	private Class<? extends Condition> addTokenEndpointAuthMethodSupported;
	private Class<? extends ConditionSequence> validateClientAuthenticationSteps;
	private Class<? extends ConditionSequence> configureAuthRequestMethodSteps;
	private Class<? extends ConditionSequence> configureResponseModeSteps;
	private Class<? extends ConditionSequence> authorizationCodeGrantTypeProfileSteps;
	private Class<? extends ConditionSequence> authorizationEndpointProfileSteps;
	private Class<? extends ConditionSequence> accountsEndpointProfileSteps;

	// Controls which endpoints we should expose to the client
	protected FAPI1FinalOPProfile profile;

	protected FAPIAuthRequestMethod authRequestMethod;

	protected FAPIResponseMode responseMode;

	protected ClientAuthType clientAuthType;

	protected FAPIJARMType jarmType;

	/**
	 * Exposes, in the web frontend, a path that the user needs to know
	 *
	 * @param name Name to use in the frontend
	 * @param path Path, relative to baseUrl
	 */
	private void exposePath(String name, String path) {
		env.putString(name, env.getString("base_url") + "/" + path);
		exposeEnvString(name);
	}

	private void exposeMtlsPath(String name, String path) {
		String baseUrlMtls = env.getString("base_url").replaceFirst(TestDispatcher.TEST_PATH, TestDispatcher.TEST_MTLS_PATH);
		env.putString(name, baseUrlMtls + "/" + path);
		exposeEnvString(name);
	}

	protected abstract void addCustomValuesToIdToken();

	protected void addCustomSignatureOfIdToken(){}

	protected void endTestIfRequiredParametersAreMissing(){}

	@Override
	public void configure(JsonObject config, String baseUrl, String externalUrlOverride) {
		env.putString("base_url", baseUrl);
		env.putObject("config", config);

		profile = getVariant(FAPI1FinalOPProfile.class);
		authRequestMethod = getVariant(FAPIAuthRequestMethod.class);
		responseMode = getVariant(FAPIResponseMode.class);
		clientAuthType = getVariant(ClientAuthType.class);
		jarmType = getVariant(FAPIJARMType.class);
		callAndStopOnFailure(GenerateServerConfigurationMTLS.class);

		//this must come before configureResponseModeSteps due to JARM signing_algorithm dependency
		callAndStopOnFailure(LoadServerJWKs.class);
		callAndStopOnFailure(ValidateServerJWKs.class, "RFC7517-1.1");

		if(profile == FAPI1FinalOPProfile.OPENBANKING_BRAZIL) {
			callAndStopOnFailure(SetServerSigningAlgToPS256.class, "BrazilOB-6.1-1");
			callAndStopOnFailure(FAPIBrazilSetGrantTypesSupportedInServerConfiguration.class, "BrazilOB-5.2.3-5");
			callAndStopOnFailure(AddClaimsParameterSupportedTrueToServerConfiguration.class, "BrazilOB-5.2.2-3");
			callAndStopOnFailure(FAPIBrazilAddBrazilSpecificSettingsToServerConfiguration.class, "BrazilOB-5.2.2");
		} else {
			callAndStopOnFailure(ExtractServerSigningAlg.class);
		}

		callAndStopOnFailure(addTokenEndpointAuthMethodSupported);

		if(configureAuthRequestMethodSteps!=null) {
			call(sequence(configureAuthRequestMethodSteps));
		}

		if(configureResponseModeSteps!=null) {
			call(sequence(configureResponseModeSteps));
		}
		if(profile == FAPI1FinalOPProfile.OPENBANKING_BRAZIL) {
			callAndStopOnFailure(FAPIBrazilAddTokenEndpointAuthSigningAlgValuesSupportedToServer.class);
		} else {
			callAndStopOnFailure(FAPIAddTokenEndpointAuthSigningAlgValuesSupportedToServer.class);
		}

		exposeEnvString("discoveryUrl");
		exposeEnvString("issuer");

		exposeMtlsPath("accounts_endpoint", ACCOUNTS_PATH);

		if(profile == FAPI1FinalOPProfile.OPENBANKING_BRAZIL) {
			exposeMtlsPath("consents_endpoint", BRAZIL_CONSENTS_PATH);
		} else {
			exposePath("account_requests_endpoint", ACCOUNT_REQUESTS_PATH);
		}

		if(authRequestMethod == FAPIAuthRequestMethod.PUSHED) {
			exposeMtlsPath("par_endpoint", "par");
		}

		callAndStopOnFailure(CheckServerConfiguration.class);

		callAndStopOnFailure(FAPIEnsureMinimumServerKeyLength.class, "FAPI1-BASE-5.2.2-5", "FAPI1-BASE-5.2.2-6");

		callAndStopOnFailure(LoadUserInfo.class);

		configureClients();

		onConfigurationCompleted();
		setStatus(Status.CONFIGURED);
		fireSetupDone();
	}

	/**
	 * will be called at the end of configure
	 */
	protected void onConfigurationCompleted() {

	}

	protected void configureClients()
	{
		eventLog.startBlock("Verify configuration of first client");
		callAndStopOnFailure(GetStaticClientConfiguration.class);

		validateClientJwks(false);
		validateClientConfiguration();

		eventLog.startBlock("Verify configuration of second client");
		// extract second client
		switchToSecondClient();
		callAndStopOnFailure(GetStaticClient2Configuration.class);

		validateClientJwks(true);
		validateClientConfiguration();

		//switch back to the first client
		unmapClient();
		eventLog.endBlock();
	}

	protected void validateClientConfiguration() {
	}


	protected void switchToSecondClient() {
		env.mapKey("client", "client2");
		env.mapKey("client_jwks", "client_jwks2");
	}

	protected void unmapClient() {
		env.unmapKey("client");
		env.unmapKey("client_jwks");
	}

	protected void validateClientJwks(boolean isSecondClient)
	{
		callAndStopOnFailure(ValidateClientJWKsPublicPart.class, "RFC7517-1.1");

		callAndStopOnFailure(ExtractJWKsFromStaticClientConfiguration.class);
		callAndContinueOnFailure(CheckDistinctKeyIdValueInClientJWKs.class, Condition.ConditionResult.FAILURE, "RFC7517-4.5");
		callAndContinueOnFailure(EnsureClientJwksDoesNotContainPrivateOrSymmetricKeys.class, Condition.ConditionResult.FAILURE);

		callAndStopOnFailure(FAPIEnsureMinimumClientKeyLength.class,"FAPI1-BASE-5.2.4-2", "FAPI1-BASE-5.2.4-3");
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

		callAndContinueOnFailure(EnsureIncomingTls12WithSecureCipherOrTls13.class, ConditionResult.FAILURE, "FAPI1-BASE-7.1", "FAPI1-ADV-8.5");

		call(exec().unmapKey("client_request"));

		setStatus(Status.WAITING);

		return handleClientRequestForPath(requestId, path);

	}


	protected Object handleClientRequestForPath(String requestId, String path){
		if (path.equals("authorize")) {
			return authorizationEndpoint(requestId);
		} else if (path.equals("token")) {
			return tokenEndpoint(requestId);
		} else if (path.equals("jwks")) {
			return jwksEndpoint();
		} else if (path.equals("userinfo")) {
			return userinfoEndpoint(requestId);
		} else if (path.equals(".well-known/openid-configuration")) {
			return discoveryEndpoint();
		} else if (path.equals("par") && authRequestMethod == FAPIAuthRequestMethod.PUSHED) {
			return parEndpoint(requestId);
		} else if (path.equals(ACCOUNT_REQUESTS_PATH) && profile == FAPI1FinalOPProfile.OPENBANKING_UK) {
			return accountRequestsEndpoint(requestId);
		}
		throw new TestFailureException(getId(), "Got unexpected HTTP call to " + path);
	}

	@Override
	public Object handleHttpMtls(String path, HttpServletRequest req, HttpServletResponse res, HttpSession session, JsonObject requestParts) {

		setStatus(Status.RUNNING);

		String requestId = "incoming_request_" + RandomStringUtils.randomAlphanumeric(37);

		env.putObject(requestId, requestParts);

		call(exec().mapKey("client_request", requestId));

		callAndContinueOnFailure(EnsureIncomingTls12WithSecureCipherOrTls13.class, ConditionResult.FAILURE, "FAPI1-BASE-7.1", "FAPI1-ADV-8.5-1");

		call(exec().unmapKey("client_request"));

		setStatus(Status.WAITING);

		if (path.equals("token")) {
			return tokenEndpoint(requestId);
		} else if (path.equals(ACCOUNTS_PATH)) {
			return accountsEndpoint(requestId);
		} else if (path.equals("par") && authRequestMethod == FAPIAuthRequestMethod.PUSHED) {
			return parEndpoint(requestId);
		}
		if (profile == FAPI1FinalOPProfile.OPENBANKING_BRAZIL) {
			if(BRAZIL_CONSENTS_PATH.equals(path)) {
				return brazilHandleNewConsentRequest(requestId);
			} else if(path.startsWith(BRAZIL_CONSENTS_PATH + "/")) {
				return brazilHandleGetConsentRequest(requestId, path);
			}
		}

		throw new TestFailureException(getId(), "Got unexpected HTTP call to " + path);
	}

	protected void validateResourceEndpointHeaders() {
		skipIfElementMissing("incoming_request", "headers.x-fapi-auth-date", ConditionResult.INFO,
			ExtractFapiDateHeader.class, ConditionResult.FAILURE, "FAPI1-BASE-6.2.2-3");

		skipIfElementMissing("incoming_request", "headers.x-fapi-customer-ip-address", ConditionResult.INFO,
			ExtractFapiIpAddressHeader.class, ConditionResult.FAILURE, "FAPI1-BASE-6.2.2-4");

		skipIfElementMissing("incoming_request", "headers.x-fapi-interaction-id", ConditionResult.INFO,
			ExtractFapiDateHeader.class, ConditionResult.FAILURE, "FAPI1-BASE-6.2.2-5");

	}
	protected Object brazilHandleNewConsentRequest(String requestId) {
		setStatus(Status.RUNNING);

		call(exec().mapKey("token_endpoint_request", requestId));
		checkMtlsCertificate();
		call(exec().unmapKey("token_endpoint_request"));

		//Requires method=POST
		call(exec().startBlock("New consent endpoint").mapKey("incoming_request", requestId));

		callAndStopOnFailure(EnsureBearerAccessTokenNotInParams.class, "FAPI1-BASE-6.2.2-1");
		callAndStopOnFailure(ExtractBearerAccessTokenFromHeader.class, "FAPI1-BASE-6.2.2-1");
		callAndStopOnFailure(RequireBearerClientCredentialsAccessToken.class);

		validateResourceEndpointHeaders();

		callAndStopOnFailure(FAPIBrazilExtractConsentRequest.class, "BrazilOB-5.2.2.2");

		callAndStopOnFailure(CreateFapiInteractionIdIfNeeded.class, "FAPI1-BASE-6.2.1-11");

		callAndStopOnFailure(FAPIBrazilGenerateNewConsentResponse.class, "BrazilOB-5.2.2.2");
		JsonObject response = env.getObject("consent_response");
		JsonObject headerJson = env.getObject("consent_response_headers");

		callAndStopOnFailure(ClearAccessTokenFromRequest.class);

		call(exec().unmapKey("incoming_request").endBlock());

		setStatus(Status.WAITING);

		return new ResponseEntity<Object>(response, headersFromJson(headerJson), HttpStatus.CREATED);
	}



	protected Object brazilHandleGetConsentRequest(String requestId, String path) {
		setStatus(Status.RUNNING);
		call(exec().mapKey("token_endpoint_request", requestId));
		checkMtlsCertificate();
		call(exec().unmapKey("token_endpoint_request"));

		call(exec().startBlock("Get consent endpoint").mapKey("incoming_request", requestId));

		callAndStopOnFailure(EnsureBearerAccessTokenNotInParams.class, "FAPI1-BASE-6.2.2-1");
		callAndStopOnFailure(ExtractBearerAccessTokenFromHeader.class, "FAPI1-BASE-6.2.2-1");
		callAndStopOnFailure(RequireBearerClientCredentialsAccessToken.class);

		validateResourceEndpointHeaders();
		callAndStopOnFailure(CreateFapiInteractionIdIfNeeded.class, "FAPI1-BASE-6.2.1-11");

		String requestedConsentId = path.substring(path.lastIndexOf('/')+1);
		env.putString("requested_consent_id", requestedConsentId);
		callAndStopOnFailure(FAPIBrazilGenerateGetConsentResponse.class, "BrazilOB-5.2.2.2");

		JsonObject response = env.getObject("get_consent_response");
		JsonObject headerJson = env.getObject("get_consent_response_headers");

		callAndStopOnFailure(ClearAccessTokenFromRequest.class);

		call(exec().unmapKey("incoming_request").endBlock());

		setStatus(Status.WAITING);

		return new ResponseEntity<Object>(response, headersFromJson(headerJson), HttpStatus.OK);
	}

	protected Object discoveryEndpoint() {
		setStatus(Status.RUNNING);
		JsonObject serverConfiguration = env.getObject("server");

		setStatus(Status.WAITING);
		return new ResponseEntity<Object>(serverConfiguration, HttpStatus.OK);
	}

	protected void checkMtlsCertificate() {
		callAndContinueOnFailure(ExtractClientCertificateFromTokenEndpointRequestHeaders.class);
		callAndStopOnFailure(CheckForClientCertificate.class, "FAPI1-ADV-5.2.2-5");
		callAndStopOnFailure(EnsureClientCertificateMatches.class);
	}
	protected void authenticateParEndpointRequest(String requestId) {
		call(exec().mapKey("token_endpoint_request", requestId));

		checkMtlsCertificate();
		call(sequence(validateClientAuthenticationSteps));
//TODO Due to historical reasons there is potential ambiguity regarding the
//   appropriate audience value to use when employing JWT client assertion
//   based authentication (defined in Section 2.2 of [RFC7523] with
//   "private_key_jwt" or "client_secret_jwt" authentication method names
//   per Section 9 of [OIDC]).  To address that ambiguity the issuer
//   identifier URL of the authorization server according to [RFC8414]
//   SHOULD be used as the value of the audience.  In order to facilitate
//   interoperability the authorization server MUST accept its issuer
//   identifier, token endpoint URL, or pushed authorization request
//   endpoint URL as values that identify it as an intended audience.
		call(exec().unmapKey("token_endpoint_request"));
	}

	protected void extractParEndpointRequest() {
		callAndStopOnFailure(ExtractRequestObjectFromPAREndpointRequest.class, "PAR-2.1");
		callAndStopOnFailure(EnsurePAREndpointRequestDoesNotContainRequestUriParameter.class, "PAR-2.1");
	}

	protected Object parEndpoint(String requestId) {
		setStatus(Status.RUNNING);
		call(exec().startBlock("PAR endpoint").mapKey("par_endpoint_http_request", requestId));

		authenticateParEndpointRequest(requestId);
		extractParEndpointRequest();
		validateRequestObjectForPAREndpointRequest();

		JsonObject parResponse = createPAREndpointResponse();
		setStatus(Status.WAITING);
		return new ResponseEntity<Object>(parResponse, HttpStatus.CREATED);
	}

	protected JsonObject createPAREndpointResponse() {
		callAndStopOnFailure(CreatePAREndpointResponse.class, "PAR-2.2");
		JsonObject parResponse = env.getObject("par_endpoint_response");
		return parResponse;
	}

	protected Object userinfoEndpoint(String requestId) {

		setStatus(Status.RUNNING);

		call(exec().startBlock("Userinfo endpoint")
			.mapKey("incoming_request", requestId));

		callAndStopOnFailure(EnsureBearerAccessTokenNotInParams.class, "FAPI1-BASE-6.2.2-1");
		callAndStopOnFailure(ExtractBearerAccessTokenFromHeader.class, "FAPI1-BASE-6.2.2-1");

		callAndStopOnFailure(RequireBearerAccessToken.class);

		callAndStopOnFailure(RequireOpenIDScope.class, "FAPI1-BASE-5.2.3.1-1");

		callAndStopOnFailure(FilterUserInfoForScopes.class);
		if(profile == FAPI1FinalOPProfile.OPENBANKING_BRAZIL) {
			callAndStopOnFailure(FAPIBrazilAddCPFAndCPNJToUserInfoClaims.class, "BrazilOB-5.2.2.2", "BrazilOB-5.2.2.3");
		}

		JsonObject user = env.getObject("user_info_endpoint_response");

		callAndStopOnFailure(ClearAccessTokenFromRequest.class);

		call(exec().unmapKey("incoming_request").endBlock());

		setStatus(Status.WAITING);

		return new ResponseEntity<Object>(user, HttpStatus.OK);

	}

	protected Object jwksEndpoint() {

		setStatus(Status.RUNNING);
		JsonObject jwks = env.getObject("server_public_jwks");

		setStatus(Status.WAITING);

		return new ResponseEntity<Object>(jwks, HttpStatus.OK);
	}

	protected Object tokenEndpoint(String requestId) {

		setStatus(Status.RUNNING);

		call(exec().startBlock("Token endpoint")
			.mapKey("token_endpoint_request", requestId));

		checkMtlsCertificate();

		call(sequence(validateClientAuthenticationSteps));

		return handleTokenEndpointGrantType(requestId);

	}

	protected Object handleTokenEndpointGrantType(String requestId){

		// dispatch based on grant type
		String grantType = env.getString("token_endpoint_request", "body_form_params.grant_type");

		if (grantType.equals("authorization_code")) {
			// we're doing the authorization code grant for user access
			return authorizationCodeGrantType(requestId);
		} else if (grantType.equals("client_credentials")) {
			if( profile == FAPI1FinalOPProfile.OPENBANKING_UK) {
				// we're doing the client credentials grant for initial token access
				return clientCredentialsGrantType(requestId);
			} else if(profile == FAPI1FinalOPProfile.OPENBANKING_BRAZIL) {
				callAndStopOnFailure(FAPIBrazilEnsureRequestedScopeContainsConsents.class);
				return clientCredentialsGrantType(requestId);
			}
		} else if (grantType.equals("refresh_token")) {
			return refreshTokenGrantType(requestId);
		}
		throw new TestFailureException(getId(), "Got an unexpected grant type on the token endpoint: " + grantType);
	}

	protected Object refreshTokenGrantType(String requestId) {

		callAndStopOnFailure(ValidateRefreshToken.class);

		issueAccessToken();

		callAndStopOnFailure(CreateTokenEndpointResponse.class);

		call(exec().unmapKey("token_endpoint_request").endBlock());

		setStatus(Status.WAITING);

		return new ResponseEntity<Object>(env.getObject("token_endpoint_response"), HttpStatus.OK);

	}

	protected Object clientCredentialsGrantType(String requestId) {

		callAndStopOnFailure(GenerateBearerAccessToken.class);

		callAndStopOnFailure(CreateTokenEndpointResponse.class);

		// this puts the client credentials specific token into its own box for later
		callAndStopOnFailure(CopyAccessTokenToClientCredentialsField.class);

		call(exec().unmapKey("token_endpoint_request").endBlock());

		setStatus(Status.WAITING);

		return new ResponseEntity<Object>(env.getObject("token_endpoint_response"), HttpStatus.OK);

	}

	protected Object authorizationCodeGrantType(String requestId) {

		callAndStopOnFailure(ValidateAuthorizationCode.class);

		callAndStopOnFailure(ValidateRedirectUri.class);

		if(authRequestMethod==FAPIAuthRequestMethod.PUSHED) {
			callAndStopOnFailure(ValidateCodeVerifierWithS256.class, "RFC7636-4.6", "FAPI1-ADV-5.2.3-15");
		}

		issueAccessToken();

		issueRefreshToken();

		String isOpenIdScopeRequested = env.getString("request_scopes_contain_openid");
		if("yes".equals(isOpenIdScopeRequested)) {
			issueIdToken(false);
		}

		createTokenEndpointResponse();

		call(exec().unmapKey("token_endpoint_request").endBlock());

		setStatus(Status.WAITING);

		return new ResponseEntity<Object>(env.getObject("token_endpoint_response"), HttpStatus.OK);

	}

	protected void createTokenEndpointResponse() {
		callAndStopOnFailure(CreateTokenEndpointResponse.class);
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
	protected Object authorizationEndpoint(String requestId) {

		setStatus(Status.RUNNING);

		call(exec().startBlock("Authorization endpoint").mapKey("authorization_endpoint_http_request", requestId));
		setAuthorizationEndpointRequestParamsForHttpMethod();
		if(authRequestMethod == FAPIAuthRequestMethod.PUSHED) {
			callAndStopOnFailure(EnsureAuthorizationRequestDoesNotContainRequestWhenUsingPAR.class);
		}

		if(authRequestMethod == FAPIAuthRequestMethod.BY_VALUE) {
			callAndStopOnFailure(ExtractRequestObject.class, "FAPI1-ADV-5.2.2-10");
			if(profile == FAPI1FinalOPProfile.OPENBANKING_BRAZIL) {
				callAndStopOnFailure(EnsureRequestObjectWasEncrypted.class, "BrazilOB-5.2.3-3");
				callAndStopOnFailure(FAPIBrazilEnsureRequestObjectEncryptedUsingRSAOAEPA256GCM.class, "BrazilOB-6.1.1-1");
			}
		}

		//CreateEffectiveAuthorizationRequestParameters call must be before endTestIfRequiredParametersAreMissing
		callAndStopOnFailure(CreateEffectiveAuthorizationRequestParameters.class);

		endTestIfRequiredParametersAreMissing();

		validateRequestObjectForAuthorizationEndpointRequest();

		callAndStopOnFailure(CreateAuthorizationCode.class);
		String isOpenIdScopeRequested = env.getString("request_scopes_contain_openid");
		if("yes".equals(isOpenIdScopeRequested)) {
			if(jarmType==FAPIJARMType.PLAIN_OAUTH) {
				throw new TestFailureException(getId(), "openid scope cannot be used with PLAIN_OAUTH");
			}
			callAndStopOnFailure(ExtractNonceFromAuthorizationRequest.class, "FAPI1-BASE-5.2.2.2");
		} else {
			if(jarmType==FAPIJARMType.OIDC) {
				throw new TestFailureException(getId(), "openid scope must be used with OIDC");
			}
			callAndStopOnFailure(EnsureAuthorizationRequestContainsStateParameter.class, "FAPI1-BASE-5.2.2.3-1");
		}

		if(responseMode!=FAPIResponseMode.JARM && "yes".equals(isOpenIdScopeRequested)) {
			//issueAccessToken();
			issueIdToken(true);
		}

		/*
		 	- Após o `POST` de criação do consentimento, o `STATUS` devolvido na resposta deverá ser `AWAITING_AUTHORISATION`.
			- O `STATUS` será alterado para `AUTHORISED` somente após autenticação e confirmação por parte do
				usuário na instituição transmissora dos dados.
		 */
		if(profile == FAPI1FinalOPProfile.OPENBANKING_BRAZIL) {
			callAndStopOnFailure(FAPIBrazilChangeConsentStatusToAuthorized.class);
		}

		createAuthorizationEndpointResponse();

		String redirectTo = env.getString("authorization_endpoint_response_redirect");

		setStatus(Status.WAITING);

		call(exec().unmapKey("authorization_endpoint_http_request").endBlock());

		return new RedirectView(redirectTo, false, false, false);

	}

	/**
	 * Common checks applicable to both PAR endpoint and authorization requests
	 */
	protected void validateRequestObjectCommonChecks() {
		callAndStopOnFailure(FAPIValidateRequestObjectSigningAlg.class, "FAPI1-ADV-8.6");
		if(jarmType==FAPIJARMType.OIDC) {
			if(profile == FAPI1FinalOPProfile.OPENBANKING_BRAZIL) {
				callAndContinueOnFailure(FAPIBrazilValidateRequestObjectIdTokenACRClaims.class, ConditionResult.FAILURE,
					"FAPI1-ADV-5.2.3-5", "OIDCC-5.5.1.1", "BrazilOB-5.2.2.4");
			} else {
				callAndContinueOnFailure(FAPIValidateRequestObjectIdTokenACRClaims.class, ConditionResult.INFO,
					"FAPI1-ADV-5.2.3-5", "OIDCC-5.5.1.1");
			}
		}
		callAndStopOnFailure(FAPIValidateRequestObjectExp.class, "RFC7519-4.1.4", "FAPI1-ADV-5.2.2-13");
		callAndContinueOnFailure(FAPI1AdvancedValidateRequestObjectNBFClaim.class, ConditionResult.FAILURE, "FAPI1-ADV-5.2.2-17");
		callAndStopOnFailure(ValidateRequestObjectClaims.class);
		callAndContinueOnFailure(EnsureNumericRequestObjectClaimsAreNotNull.class, Condition.ConditionResult.WARNING, "OIDCC-13.3");
		callAndContinueOnFailure(EnsureRequestObjectDoesNotContainRequestOrRequestUri.class, ConditionResult.FAILURE, "OIDCC-6.1");
		callAndContinueOnFailure(EnsureRequestObjectDoesNotContainSubWithClientId.class, ConditionResult.FAILURE, "JAR-10.8");
		callAndStopOnFailure(ValidateRequestObjectSignature.class, "FAPI1-ADV-5.2.2-1");
		callAndStopOnFailure(EnsureMatchingRedirectUriInRequestObject.class);
	}

	protected void validateRequestObjectForAuthorizationEndpointRequest() {
		if(authRequestMethod==FAPIAuthRequestMethod.PUSHED) {
			callAndContinueOnFailure(EnsureClientIdInAuthorizationRequestParametersMatchRequestObject.class, ConditionResult.FAILURE,
				"FAPI1-ADV-5.2.3-16");
		} else {
			validateRequestObjectCommonChecks();	//for PAR, these checks will be applied to the PAR endpoint request
			callAndContinueOnFailure(EnsureRequiredAuthorizationRequestParametersMatchRequestObject.class,  ConditionResult.FAILURE,
				"OIDCC-6.1", "FAPI1-ADV-5.2.3-9");
			callAndContinueOnFailure(EnsureOptionalAuthorizationRequestParametersMatchRequestObject.class, ConditionResult.WARNING,
				"OIDCC-6.1", "OIDCC-6.2");
			if(responseMode!=FAPIResponseMode.JARM) {
				callAndContinueOnFailure(EnsureAuthorizationHttpRequestContainsOpenIDScope.class, ConditionResult.FAILURE,
					"OIDCC-6.1", "OIDCC-6.2");
			}
		}
		callAndStopOnFailure(ExtractRequestedScopes.class);

		if(profile == FAPI1FinalOPProfile.OPENBANKING_BRAZIL) {
			callAndStopOnFailure(FAPIBrazilValidateConsentScope.class);
		} else {
			callAndStopOnFailure(EnsureRequestedScopeIsEqualToConfiguredScope.class);
		}

		if(responseMode==FAPIResponseMode.JARM) {
			callAndStopOnFailure(EnsureResponseTypeIsCode.class, "FAPI1-ADV-5.2.2-2");
		} else if(responseMode==FAPIResponseMode.PLAIN_RESPONSE) {
			callAndStopOnFailure(EnsureResponseTypeIsCodeIdToken.class, "OIDCC-6.1", "FAPI1-ADV-5.2.2-1");
			callAndStopOnFailure(EnsureOpenIDInScopeRequest.class, "FAPI1-BASE-5.2.3-7");
		}

		callAndStopOnFailure(EnsureMatchingClientId.class, "OIDCC-3.1.2.1");
	}

	protected void validateRequestObjectForPAREndpointRequest() {
		validateRequestObjectCommonChecks();
		callAndStopOnFailure(EnsureRequestObjectContainsCodeChallengeWhenUsingPAR.class, "FAPI1-ADV-5.2.3-15");
	}

	protected void issueIdToken(boolean isAuthorizationEndpoint) {
		prepareIdTokenClaims(isAuthorizationEndpoint);

		signIdToken();

		encryptIdToken(isAuthorizationEndpoint);
	}

	protected void issueAccessToken() {
		callAndStopOnFailure(GenerateBearerAccessToken.class);
		callAndStopOnFailure(CalculateAtHash.class, "OIDCC-3.3.2.11");
	}

	protected void issueRefreshToken() {
		callAndStopOnFailure(CreateRefreshToken.class);
	}

	protected void prepareIdTokenClaims(boolean isAuthorizationEndpoint) {

		//3.3.3.6 The at_hash and c_hash Claims MAY be omitted from the ID Token returned from the Token Endpoint even when these Claims are present in the ID Token returned from the Authorization Endpoint,
		//TODO skip or add?
		if(isAuthorizationEndpoint) {
			callAndStopOnFailure(CalculateCHash.class, "OIDCC-3.3.2.11");
			skipIfElementMissing(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, CreateEffectiveAuthorizationRequestParameters.STATE,
				ConditionResult.INFO, CalculateSHash.class, ConditionResult.FAILURE, "FAPI1-ADV-5.2.2.1-5");
		}

		callAndStopOnFailure(GenerateIdTokenClaims.class);
		if(profile == FAPI1FinalOPProfile.OPENBANKING_BRAZIL) {
			callAndStopOnFailure(FAPIBrazilAddCPFAndCPNJToIdTokenClaims.class, "BrazilOB-5.2.2.2", "BrazilOB-5.2.2.3");
		}

		if (!isAuthorizationEndpoint && authorizationCodeGrantTypeProfileSteps != null) {
			call(sequence(authorizationCodeGrantTypeProfileSteps));
		}

		if (isAuthorizationEndpoint && authorizationEndpointProfileSteps != null) {
			call(sequence(authorizationEndpointProfileSteps));
		}

		//TODO skip or add?
		if(isAuthorizationEndpoint) {
			callAndStopOnFailure(AddCHashToIdTokenClaims.class, "OIDCC-3.3.2.11");
			skipIfMissing(null, new String[] {"s_hash"}, ConditionResult.INFO,
				AddSHashToIdTokenClaims.class, ConditionResult.FAILURE, "FAPI1-ADV-5.2.2.1-5");
		}
		skipIfMissing(null, new String[] {"at_hash"}, ConditionResult.INFO,
			AddAtHashToIdTokenClaims.class, ConditionResult.FAILURE, "OIDCC-3.3.2.11");

		addCustomValuesToIdToken();

		if(profile == FAPI1FinalOPProfile.OPENBANKING_BRAZIL) {
			skipIfMissing(null, new String[]{"requested_id_token_acr_values"}, ConditionResult.INFO,
				FAPIBrazilAddACRClaimToIdTokenClaims.class, ConditionResult.FAILURE, "OIDCC-3.1.3.7-12");
		} else {
			skipIfMissing(null, new String[]{"requested_id_token_acr_values"}, ConditionResult.INFO,
				AddACRClaimToIdTokenClaims.class, ConditionResult.FAILURE, "OIDCC-3.1.3.7-12");
		}


	}
	protected void signIdToken() {
		callAndStopOnFailure(SignIdToken.class);

		addCustomSignatureOfIdToken();
	}

	/**
	 * This method does not actually encrypt id_tokens, even when id_token_encrypted_response_alg is set
	 * "5.2.3.1.  ID Token as detached signature" reads:
	 *  "5. shall support both signed and signed & encrypted ID Tokens."
	 *  So an implementation MUST support non-encrypted id_tokens too and we do NOT allow testers to run all tests with id_token
	 *  encryption enabled, encryption will be enabled only for certain tests and the rest will return non-encrypted id_tokens.
	 *  Second client will be used for encrypted id_token tests. First client does not need to have an encryption key
	 * @param isAuthorizationEndpoint
	 */
	protected void encryptIdToken(boolean isAuthorizationEndpoint) {
	}

	protected void createAuthorizationEndpointResponse() {
		callAndStopOnFailure(CreateAuthorizationEndpointResponseParams.class);

		callAndStopOnFailure(AddCodeToAuthorizationEndpointResponseParams.class, "OIDCC-3.3.2.5");

		if(responseMode==FAPIResponseMode.PLAIN_RESPONSE) {
			callAndStopOnFailure(AddIdTokenToAuthorizationEndpointResponseParams.class, "OIDCC-3.3.2.5");

			callAndStopOnFailure(SendAuthorizationResponseWithResponseModeFragment.class, "OIDCC-3.3.2.5");
		}
		if(responseMode==FAPIResponseMode.JARM) {
			createJARMResponse();
			//send via redirect
			callAndStopOnFailure(SendJARMResponseWitResponseModeQuery.class, "OIDCC-3.3.2.5", "JARM-4.3.1");
		}

		exposeEnvString("authorization_endpoint_response_redirect");
	}

	protected void createJARMResponse() {
		generateJARMResponseClaims();
		//authorization_signed_response_alg will not be taken into account. signing_algorithm will be used
		callAndStopOnFailure(SignJARMResponse.class,"JARM-4.2");
		encryptJARMResponse();
	}

	protected void encryptJARMResponse() {
		skipIfElementMissing("client", "authorization_encrypted_response_alg", ConditionResult.INFO,
			EncryptJARMResponse.class, ConditionResult.FAILURE, "JARM-5");

	}

	protected void generateJARMResponseClaims() {
		callAndStopOnFailure(GenerateJARMResponseClaims.class,"JARM-4.1.1");
	}

	/**
	 * OpenBanking account request API
	 *
	 * @param requestId
	 * @return
	 */
	protected Object accountRequestsEndpoint(String requestId) {

		setStatus(Status.RUNNING);

		call(exec().startBlock("Account request endpoint")
			.mapKey("incoming_request", requestId));

		callAndStopOnFailure(EnsureBearerAccessTokenNotInParams.class, "FAPI1-BASE-6.2.2-1");
		callAndStopOnFailure(ExtractBearerAccessTokenFromHeader.class, "FAPI1-BASE-6.2.2-1");

		callAndStopOnFailure(RequireBearerClientCredentialsAccessToken.class);

		// TODO: should we clear the old headers?
		validateResourceEndpointHeaders();

		callAndStopOnFailure(GenerateAccountRequestId.class);
		exposeEnvString("account_request_id");

		callAndStopOnFailure(CreateFapiInteractionIdIfNeeded.class, "FAPI1-BASE-6.2.1-11");

		callAndStopOnFailure(CreateOpenBankingAccountRequestResponse.class);

		JsonObject accountRequestResponse = env.getObject("account_request_response");
		JsonObject headerJson = env.getObject("account_request_response_headers");

		callAndStopOnFailure(ClearAccessTokenFromRequest.class);

		call(exec().unmapKey("incoming_request").endBlock());

		setStatus(Status.WAITING);

		return new ResponseEntity<Object>(accountRequestResponse, headersFromJson(headerJson), HttpStatus.OK);
	}

	protected Object accountsEndpoint(String requestId) {
		setStatus(Status.RUNNING);

		call(exec().startBlock("Accounts endpoint"));

		call(exec().mapKey("token_endpoint_request", requestId));

		checkMtlsCertificate();

		call(exec().unmapKey("token_endpoint_request"));

		call(exec().mapKey("incoming_request", requestId));

		callAndStopOnFailure(EnsureBearerAccessTokenNotInParams.class, "FAPI1-BASE-6.2.2-1");
		callAndStopOnFailure(ExtractBearerAccessTokenFromHeader.class, "FAPI1-BASE-6.2.2-1");

		callAndStopOnFailure(RequireBearerAccessToken.class);

		// TODO: should we clear the old headers?
		validateResourceEndpointHeaders();

		callAndStopOnFailure(CreateFapiInteractionIdIfNeeded.class, "FAPI1-BASE-6.2.1-11");

		callAndStopOnFailure(CreateFAPIAccountEndpointResponse.class);

		if (accountsEndpointProfileSteps != null)
			call(sequence(accountsEndpointProfileSteps));

		callAndStopOnFailure(ClearAccessTokenFromRequest.class);

		call(exec().unmapKey("incoming_request").endBlock());

		JsonObject accountsEndpointResponse = env.getObject("accounts_endpoint_response");
		JsonObject headerJson = env.getObject("accounts_endpoint_response_headers");

		// at this point we can assume the test is fully done
		fireTestFinished();

		return new ResponseEntity<>(accountsEndpointResponse, headersFromJson(headerJson), HttpStatus.OK);
	}

	@VariantSetup(parameter = ClientAuthType.class, value = "mtls")
	public void setupMTLS() {
		addTokenEndpointAuthMethodSupported = AddTLSClientAuthToServerConfiguration.class;
		validateClientAuthenticationSteps = ValidateClientAuthenticationWithMTLS.class;
	}

	@VariantSetup(parameter = ClientAuthType.class, value = "private_key_jwt")
	public void setupPrivateKeyJwt() {
		addTokenEndpointAuthMethodSupported = SetTokenEndpointAuthMethodsSupportedToPrivateKeyJWTOnly.class;
		validateClientAuthenticationSteps = ValidateClientAuthenticationWithPrivateKeyJWT.class;
	}

	@VariantSetup(parameter = FAPI1FinalOPProfile.class, value = "plain_fapi")
	public void setupPlainFapi() {
		authorizationCodeGrantTypeProfileSteps = null;
		authorizationEndpointProfileSteps = null;
		accountsEndpointProfileSteps = null;
	}

	@VariantSetup(parameter = FAPI1FinalOPProfile.class, value = "openbanking_uk")
	public void setupOpenBankingUk() {
		authorizationCodeGrantTypeProfileSteps = AddOpenBankingUkClaimsToAuthorizationCodeGrant.class;
		authorizationEndpointProfileSteps = AddOpenBankingUkClaimsToAuthorizationEndpointResponse.class;
		accountsEndpointProfileSteps = GenerateOpenBankingUkAccountsEndpointResponse.class;
	}

	@VariantSetup(parameter = FAPIAuthRequestMethod.class, value = "by_value")
	public void setupAuthRequestMethodByValue() {
		configureAuthRequestMethodSteps = null;
	}

	@VariantSetup(parameter = FAPIAuthRequestMethod.class, value = "pushed")
	public void setupAuthRequestMethodPushed() {
		configureAuthRequestMethodSteps = AddPARToServerConfiguration.class;
	}

	@VariantSetup(parameter = FAPIResponseMode.class, value = "plain_response")
	public void setupResponseModePlain() {
		configureResponseModeSteps = AddPlainFAPIToServerConfiguration.class;
	}

	@VariantSetup(parameter = FAPIResponseMode.class, value = "jarm")
	public void setupResponseModeJARM() {
		configureResponseModeSteps = AddJARMToServerConfiguration.class;
	}

	protected void startWaitingForTimeout() {
		getTestExecutionManager().runInBackground(() -> {
			Thread.sleep(5 * 1000);
			if (getStatus().equals(Status.WAITING)) {
				setStatus(Status.RUNNING);
				//As the client hasn't called the token endpoint after 5 seconds, assume it has correctly detected the error and aborted.
				fireTestFinished();
			}

			return "done";

		});
	}
}
