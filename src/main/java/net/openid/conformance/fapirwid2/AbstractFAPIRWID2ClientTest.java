package net.openid.conformance.fapirwid2;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.as.AddACRClaimToIdTokenClaims;
import net.openid.conformance.condition.as.AddAtHashToIdTokenClaims;
import net.openid.conformance.condition.as.AddCHashToIdTokenClaims;
import net.openid.conformance.condition.as.AddCodeToAuthorizationEndpointResponseParams;
import net.openid.conformance.condition.as.AddIdTokenToAuthorizationEndpointResponseParams;
import net.openid.conformance.condition.as.AddResponseTypeCodeIdTokenToServerConfiguration;
import net.openid.conformance.condition.as.AddSHashToIdTokenClaims;
import net.openid.conformance.condition.as.AddTLSClientAuthToServerConfiguration;
import net.openid.conformance.condition.as.CalculateAtHash;
import net.openid.conformance.condition.as.CalculateCHash;
import net.openid.conformance.condition.as.CalculateSHash;
import net.openid.conformance.condition.as.CheckClientIdMatchesOnTokenRequestIfPresent;
import net.openid.conformance.condition.as.CheckForClientCertificate;
import net.openid.conformance.condition.as.CheckPkceCodeVerifier;
import net.openid.conformance.condition.as.CopyAccessTokenToClientCredentialsField;
import net.openid.conformance.condition.as.CreateAuthorizationCode;
import net.openid.conformance.condition.as.CreateAuthorizationEndpointResponseParams;
import net.openid.conformance.condition.as.CreateEffectiveAuthorizationRequestParameters;
import net.openid.conformance.condition.as.CreateFapiInteractionIdIfNeeded;
import net.openid.conformance.condition.as.CreateTokenEndpointResponse;
import net.openid.conformance.condition.as.EnsureAuthorizationParametersMatchRequestObject;
import net.openid.conformance.condition.as.EnsureAuthorizationRequestContainsPkceCodeChallenge;
import net.openid.conformance.condition.as.EnsureClientCertificateMatches;
import net.openid.conformance.condition.as.EnsureClientJwksDoesNotContainPrivateOrSymmetricKeys;
import net.openid.conformance.condition.as.EnsureMatchingClientId;
import net.openid.conformance.condition.as.EnsureMatchingRedirectUriInRequestObject;
import net.openid.conformance.condition.as.EnsureNumericRequestObjectClaimsAreNotNull;
import net.openid.conformance.condition.as.EnsureOpenIDInScopeRequest;
import net.openid.conformance.condition.as.EnsureRequestObjectDoesNotContainRequestOrRequestUri;
import net.openid.conformance.condition.as.EnsureRequestObjectDoesNotContainSubWithClientId;
import net.openid.conformance.condition.as.EnsureResponseTypeIsCodeIdToken;
import net.openid.conformance.condition.as.ExtractClientCertificateFromRequestHeaders;
import net.openid.conformance.condition.as.ExtractNonceFromAuthorizationRequest;
import net.openid.conformance.condition.as.ExtractRequestObject;
import net.openid.conformance.condition.as.ExtractRequestedScopes;
import net.openid.conformance.condition.as.ExtractServerSigningAlg;
import net.openid.conformance.condition.as.FAPIAddTokenEndpointAuthSigningAlgValuesSupportedToServer;
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
import net.openid.conformance.condition.as.ValidateEncryptedRequestObjectHasKid;
import net.openid.conformance.condition.as.ValidateFAPIInteractionIdInResourceRequest;
import net.openid.conformance.condition.as.ValidateRedirectUri;
import net.openid.conformance.condition.as.ValidateRequestObjectClaims;
import net.openid.conformance.condition.as.ValidateRequestObjectSignature;
import net.openid.conformance.condition.client.ConfigurationRequestsTestIsSkipped;
import net.openid.conformance.condition.client.ExtractJWKsFromStaticClientConfiguration;
import net.openid.conformance.condition.client.FAPIValidateRequestObjectIdTokenACRClaims;
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
import net.openid.conformance.condition.rs.ExtractFapiInteractionIdHeader;
import net.openid.conformance.condition.rs.ExtractFapiIpAddressHeader;
import net.openid.conformance.condition.rs.GenerateAccountRequestId;
import net.openid.conformance.condition.rs.LoadUserInfo;
import net.openid.conformance.condition.rs.RequireBearerAccessToken;
import net.openid.conformance.condition.rs.RequireBearerClientCredentialsAccessToken;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.sequence.as.AddOpenBankingUkClaimsToAuthorizationCodeGrant;
import net.openid.conformance.sequence.as.AddOpenBankingUkClaimsToAuthorizationEndpointResponse;
import net.openid.conformance.sequence.as.GenerateOpenBankingUkAccountsEndpointResponse;
import net.openid.conformance.sequence.as.ValidateClientAuthenticationWithMTLS;
import net.openid.conformance.sequence.as.ValidateClientAuthenticationWithPrivateKeyJWT;
import net.openid.conformance.testmodule.AbstractTestModule;
import net.openid.conformance.testmodule.TestFailureException;
import net.openid.conformance.testmodule.UserFacing;
import net.openid.conformance.variant.ClientAuthType;
import net.openid.conformance.variant.FAPIProfile;
import net.openid.conformance.variant.VariantNotApplicable;
import net.openid.conformance.variant.VariantParameters;
import net.openid.conformance.variant.VariantSetup;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.view.RedirectView;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@VariantParameters({
	ClientAuthType.class,
	FAPIProfile.class
})
@VariantNotApplicable(parameter = ClientAuthType.class, values = {
	"none", "client_secret_basic", "client_secret_post", "client_secret_jwt"
})
public abstract class AbstractFAPIRWID2ClientTest extends AbstractTestModule {

	public static final String ACCOUNT_REQUESTS_PATH = "open-banking/v1.1/account-requests";
	public static final String ACCOUNTS_PATH = "open-banking/v1.1/accounts";

	private Class<? extends Condition> addTokenEndpointAuthMethodSupported;
	private Class<? extends ConditionSequence> validateClientAuthenticationSteps;
	private Class<? extends ConditionSequence> authorizationCodeGrantTypeProfileSteps;
	private Class<? extends ConditionSequence> authorizationEndpointProfileSteps;
	private Class<? extends ConditionSequence> accountsEndpointProfileSteps;

	// Controls which endpoints we should expose to the client
	protected FAPIProfile profile;

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
		String baseUrlMtls = env.getString("base_mtls_url");
		env.putString(name, baseUrlMtls + "/" + path);
		exposeEnvString(name);
	}

	protected abstract void addCustomValuesToIdToken();

	protected void addCustomSignatureOfIdToken(){}

	protected void endTestIfRequiredParametersAreMissing(){}

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

		profile = getVariant(FAPIProfile.class);

		callAndStopOnFailure(GenerateServerConfigurationMTLS.class);

		callAndStopOnFailure(addTokenEndpointAuthMethodSupported);

		callAndStopOnFailure(AddResponseTypeCodeIdTokenToServerConfiguration.class);
		callAndStopOnFailure(FAPIAddTokenEndpointAuthSigningAlgValuesSupportedToServer.class);
		exposeEnvString("discoveryUrl");
		exposeEnvString("issuer");

		exposeMtlsPath("accounts_endpoint", ACCOUNTS_PATH);
		exposePath("account_requests_endpoint", ACCOUNT_REQUESTS_PATH);

		callAndStopOnFailure(CheckServerConfiguration.class);

		callAndStopOnFailure(LoadServerJWKs.class);

		callAndStopOnFailure(ValidateServerJWKs.class, "RFC7517-1.1");

		callAndStopOnFailure(FAPIEnsureMinimumServerKeyLength.class, "FAPI-R-5.2.2-5", "FAPI-R-5.2.2-6");

		callAndStopOnFailure(LoadUserInfo.class);

		callAndStopOnFailure(GetStaticClientConfiguration.class);

		callAndStopOnFailure(ValidateClientJWKsPublicPart.class, "RFC7517-1.1");

		// for signing request objects
		callAndStopOnFailure(ExtractJWKsFromStaticClientConfiguration.class);
		callAndContinueOnFailure(CheckDistinctKeyIdValueInClientJWKs.class, Condition.ConditionResult.FAILURE, "RFC7517-4.5");
		callAndContinueOnFailure(EnsureClientJwksDoesNotContainPrivateOrSymmetricKeys.class, Condition.ConditionResult.FAILURE);
		callAndStopOnFailure(FAPIEnsureMinimumClientKeyLength.class,"FAPI-R-5.2.2.5");

		setStatus(Status.CONFIGURED);
		fireSetupDone();
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

		String requestId = "incoming_request_" + RandomStringUtils.secure().nextAlphanumeric(37);

		env.putObject(requestId, requestParts);

		call(exec().mapKey("client_request", requestId));

		callAndContinueOnFailure(EnsureIncomingTls12WithSecureCipherOrTls13.class, ConditionResult.WARNING, "FAPI1-BASE-7.1", "FAPI1-ADV-8.5");

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
		} else if (path.equals(ACCOUNT_REQUESTS_PATH) && profile == FAPIProfile.OPENBANKING_UK) {
			return accountRequestsEndpoint(requestId);
		} else {
			throw new TestFailureException(getId(), "Got unexpected HTTP call to " + path);
		}
	}

	@Override
	public Object handleHttpMtls(String path, HttpServletRequest req, HttpServletResponse res, HttpSession session, JsonObject requestParts) {

		setStatus(Status.RUNNING);

		String requestId = "incoming_request_" + RandomStringUtils.secure().nextAlphanumeric(37);

		env.putObject(requestId, requestParts);

		call(exec().mapKey("client_request", requestId));

		callAndContinueOnFailure(EnsureIncomingTls12WithSecureCipherOrTls13.class, ConditionResult.WARNING, "FAPI1-BASE-7.1", "FAPI1-ADV-8.5");

		call(exec().unmapKey("client_request"));

		setStatus(Status.WAITING);

		if (path.equals("token")) {
			return tokenEndpoint(requestId);
		} else if (path.equals(ACCOUNTS_PATH)) {
			return accountsEndpoint(requestId);
		} else {
			throw new TestFailureException(getId(), "Got unexpected HTTP call to " + path);
		}
	}

	private Object discoveryEndpoint() {
		setStatus(Status.RUNNING);
		JsonObject serverConfiguration = env.getObject("server");

		setStatus(Status.WAITING);
		return new ResponseEntity<Object>(serverConfiguration, HttpStatus.OK);
	}

	private Object userinfoEndpoint(String requestId) {

		setStatus(Status.RUNNING);

		call(exec().startBlock("Userinfo endpoint")
			.mapKey("incoming_request", requestId));

		callAndStopOnFailure(EnsureBearerAccessTokenNotInParams.class, "FAPI-R-6.2.2-1");
		callAndStopOnFailure(ExtractBearerAccessTokenFromHeader.class, "FAPI-R-6.2.2-1");

		callAndStopOnFailure(RequireBearerAccessToken.class);

		callAndStopOnFailure(FilterUserInfoForScopes.class);

		JsonObject user = env.getObject("user_info_endpoint_response");

		callAndStopOnFailure(ClearAccessTokenFromRequest.class);

		call(exec().unmapKey("incoming_request").endBlock());

		setStatus(Status.WAITING);

		return new ResponseEntity<Object>(user, HttpStatus.OK);

	}

	private Object jwksEndpoint() {

		setStatus(Status.RUNNING);
		JsonObject jwks = env.getObject("server_public_jwks");

		setStatus(Status.WAITING);

		return new ResponseEntity<Object>(jwks, HttpStatus.OK);
	}

	private Object tokenEndpoint(String requestId) {

		setStatus(Status.RUNNING);

		call(exec().startBlock("Token endpoint")
			.mapKey("token_endpoint_request", requestId));

		if (env.getObject("client") == null) {
			throw new TestFailureException(getId(), "The token endpoint has been called before the client has been registered.");
		}

		callAndContinueOnFailure(CheckClientIdMatchesOnTokenRequestIfPresent.class, ConditionResult.FAILURE, "RFC6749-3.2.1");

		callAndContinueOnFailure(ExtractClientCertificateFromRequestHeaders.class, ConditionResult.INFO);

		callAndStopOnFailure(CheckForClientCertificate.class, "FAPI-RW-5.2.2-5");

		callAndStopOnFailure(EnsureClientCertificateMatches.class);

		call(sequence(validateClientAuthenticationSteps));

		return handleTokenEndpointGrantType(requestId);

	}

	protected Object handleTokenEndpointGrantType(String requestId){

		// dispatch based on grant type
		String grantType = env.getString("token_endpoint_request", "body_form_params.grant_type");
		if (grantType == null) {
			throw new TestFailureException(getId(), "Token endpoint body does not contain the mandatory 'grant_type' parameter");
		}

		if (grantType.equals("authorization_code")) {
			// we're doing the authorization code grant for user access
			return authorizationCodeGrantType(requestId);
		} else if (grantType.equals("client_credentials") && profile == FAPIProfile.OPENBANKING_UK) {
			// we're doing the client credentials grant for initial token access
			return clientCredentialsGrantType(requestId);
		} else {
			throw new TestFailureException(getId(), "Got a grant type on the token endpoint we didn't understand: " + grantType);
		}
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

		if(env.containsObject("code_challenge")) {
			call(sequence(CheckPkceCodeVerifier.class));
		}

		callAndStopOnFailure(GenerateBearerAccessToken.class);

		callAndStopOnFailure(GenerateIdTokenClaims.class);

		if (authorizationCodeGrantTypeProfileSteps != null) {
			call(sequence(authorizationCodeGrantTypeProfileSteps));
		}

		callAndStopOnFailure(SignIdToken.class);

		callAndStopOnFailure(CreateTokenEndpointResponse.class);

		call(exec().unmapKey("token_endpoint_request").endBlock());

		setStatus(Status.WAITING);

		return new ResponseEntity<Object>(env.getObject("token_endpoint_response"), HttpStatus.OK);

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

		call(exec().startBlock("Authorization endpoint")
			.mapKey("authorization_endpoint_http_request", requestId));
		setAuthorizationEndpointRequestParamsForHttpMethod();

		callAndStopOnFailure(ExtractRequestObject.class, "FAPI-RW-5.2.2-10");
		skipIfElementMissing("authorization_request_object", "jwe_header", ConditionResult.INFO, ValidateEncryptedRequestObjectHasKid.class, ConditionResult.FAILURE, "OIDCC-10.2", "OIDCC-10.2.1");

		//CreateEffectiveAuthorizationRequestParameters call must be before endTestIfRequiredParametersAreMissing
		callAndStopOnFailure(CreateEffectiveAuthorizationRequestParameters.class);

		endTestIfRequiredParametersAreMissing();

		callAndStopOnFailure(EnsureAuthorizationParametersMatchRequestObject.class);

		callAndStopOnFailure(FAPIValidateRequestObjectSigningAlg.class, "FAPI-RW-8.6");

		callAndStopOnFailure(FAPIValidateRequestObjectIdTokenACRClaims.class, "FAPI-RW-5.2.3-5", "OIDCC-5.5.1.1");

		callAndStopOnFailure(FAPIValidateRequestObjectExp.class, "RFC7519-4.1.4", "FAPI-RW-5.2.2-13");

		callAndStopOnFailure(ValidateRequestObjectClaims.class);
		callAndContinueOnFailure(EnsureNumericRequestObjectClaimsAreNotNull.class, Condition.ConditionResult.WARNING, "OIDCC-13.3");

		callAndStopOnFailure(EnsureMatchingRedirectUriInRequestObject.class);

		callAndContinueOnFailure(EnsureRequestObjectDoesNotContainRequestOrRequestUri.class, ConditionResult.WARNING, "OIDCC-6.1");

		callAndContinueOnFailure(EnsureRequestObjectDoesNotContainSubWithClientId.class, ConditionResult.WARNING, "JAR-10.8");

		callAndStopOnFailure(ValidateRequestObjectSignature.class, "FAPI-RW-5.2.2-1");

		callAndStopOnFailure(EnsureResponseTypeIsCodeIdToken.class, "OIDCC-6.1");

		callAndStopOnFailure(EnsureMatchingClientId.class, "OIDCC-3.1.2.1");

		callAndStopOnFailure(ExtractRequestedScopes.class);

		callAndStopOnFailure(EnsureOpenIDInScopeRequest.class, "FAPI-R-5.2.3-7");

		callAndStopOnFailure(ExtractNonceFromAuthorizationRequest.class, "FAPI-R-5.2.3-8");

		skipIfElementMissing(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, CreateEffectiveAuthorizationRequestParameters.CODE_CHALLENGE, Condition.ConditionResult.INFO, EnsureAuthorizationRequestContainsPkceCodeChallenge.class, Condition.ConditionResult.FAILURE, "RFC7636-4.3");

		callAndStopOnFailure(CreateAuthorizationCode.class);

		callAndStopOnFailure(ExtractServerSigningAlg.class);

		callAndStopOnFailure(CalculateCHash.class, "OIDCC-3.3.2.11");

		skipIfElementMissing(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, CreateEffectiveAuthorizationRequestParameters.STATE,
			ConditionResult.INFO, CalculateSHash.class, ConditionResult.FAILURE, "FAPI-RW-5.2.2-4");

		callAndStopOnFailure(GenerateBearerAccessToken.class);

		callAndStopOnFailure(CalculateAtHash.class, "OIDCC-3.3.2.11");

		callAndStopOnFailure(GenerateIdTokenClaims.class);

		if (authorizationEndpointProfileSteps != null) {
			call(sequence(authorizationEndpointProfileSteps));
		}

		callAndStopOnFailure(AddCHashToIdTokenClaims.class, "OIDCC-3.3.2.11");

		skipIfMissing(null, new String[] {"s_hash"}, ConditionResult.INFO,
			AddSHashToIdTokenClaims.class, ConditionResult.FAILURE, "FAPI-RW-5.2.2-4");

		callAndStopOnFailure(AddAtHashToIdTokenClaims.class, "OIDCC-3.3.2.11");

		addCustomValuesToIdToken();

		callAndStopOnFailure(AddACRClaimToIdTokenClaims.class,  "OIDCC-3.1.3.7-12");

		callAndStopOnFailure(SignIdToken.class);

		addCustomSignatureOfIdToken();

		callAndStopOnFailure(CreateAuthorizationEndpointResponseParams.class);

		callAndStopOnFailure(AddCodeToAuthorizationEndpointResponseParams.class, "OIDCC-3.3.2.5");

		callAndStopOnFailure(AddIdTokenToAuthorizationEndpointResponseParams.class, "OIDCC-3.3.2.5");

		callAndStopOnFailure(SendAuthorizationResponseWithResponseModeFragment.class, "OIDCC-3.3.2.5");

		exposeEnvString("authorization_endpoint_response_redirect");

		String redirectTo = env.getString("authorization_endpoint_response_redirect");

		setStatus(Status.WAITING);

		call(exec().unmapKey("authorization_endpoint_http_request").endBlock());

		return new RedirectView(redirectTo, false, false, false);

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

		callAndStopOnFailure(EnsureBearerAccessTokenNotInParams.class, "FAPI-R-6.2.2-1");
		callAndStopOnFailure(ExtractBearerAccessTokenFromHeader.class, "FAPI-R-6.2.2-1");

		callAndStopOnFailure(RequireBearerClientCredentialsAccessToken.class);

		// TODO: should we clear the old headers?
		callAndContinueOnFailure(ExtractFapiDateHeader.class, ConditionResult.INFO, "FAPI-R-6.2.2-3");
		callAndContinueOnFailure(ExtractFapiIpAddressHeader.class, ConditionResult.INFO, "FAPI-R-6.2.2-4");
		callAndContinueOnFailure(ExtractFapiInteractionIdHeader.class, ConditionResult.INFO, "FAPI-R-6.2.2-4");
		callAndContinueOnFailure(ValidateFAPIInteractionIdInResourceRequest.class, ConditionResult.FAILURE, "FAPI1-BASE-6.2.2-5");

		callAndStopOnFailure(GenerateAccountRequestId.class);
		exposeEnvString("account_request_id");

		callAndStopOnFailure(CreateFapiInteractionIdIfNeeded.class, "FAPI-R-6.2.1-11");

		callAndStopOnFailure(CreateOpenBankingAccountRequestResponse.class);

		JsonObject accountRequestResponse = env.getObject("account_request_response");
		JsonObject headerJson = env.getObject("account_request_response_headers");

		callAndStopOnFailure(ClearAccessTokenFromRequest.class);

		call(exec().unmapKey("incoming_request").endBlock());

		setStatus(Status.WAITING);

		return new ResponseEntity<Object>(accountRequestResponse, headersFromJson(headerJson), HttpStatus.OK);
	}

	private Object accountsEndpoint(String requestId) {
		setStatus(Status.RUNNING);

		call(exec().startBlock("Accounts endpoint"));

		call(exec().mapKey("token_endpoint_request", requestId));

		callAndContinueOnFailure(ExtractClientCertificateFromRequestHeaders.class, ConditionResult.INFO);
		callAndStopOnFailure(CheckForClientCertificate.class, "FAPI-RW-5.2.2-5");
		callAndStopOnFailure(EnsureClientCertificateMatches.class);

		call(exec().unmapKey("token_endpoint_request"));

		call(exec().mapKey("incoming_request", requestId));

		callAndStopOnFailure(EnsureBearerAccessTokenNotInParams.class, "FAPI-R-6.2.2-1");
		callAndStopOnFailure(ExtractBearerAccessTokenFromHeader.class, "FAPI-R-6.2.2-1");

		callAndStopOnFailure(RequireBearerAccessToken.class);

		// TODO: should we clear the old headers?
		callAndContinueOnFailure(ExtractFapiDateHeader.class, ConditionResult.INFO, "FAPI-R-6.2.2-3");
		callAndContinueOnFailure(ExtractFapiIpAddressHeader.class, ConditionResult.INFO, "FAPI-R-6.2.2-4");
		callAndContinueOnFailure(ExtractFapiInteractionIdHeader.class, ConditionResult.INFO, "FAPI-R-6.2.2-4");
		callAndContinueOnFailure(ValidateFAPIInteractionIdInResourceRequest.class, ConditionResult.FAILURE, "FAPI1-BASE-6.2.2-5");

		callAndStopOnFailure(CreateFapiInteractionIdIfNeeded.class, "FAPI-R-6.2.1-11");

		callAndStopOnFailure(CreateFAPIAccountEndpointResponse.class);

		if (accountsEndpointProfileSteps != null) {
			call(sequence(accountsEndpointProfileSteps));
		}

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

	@VariantSetup(parameter = FAPIProfile.class, value = "plain_fapi")
	public void setupPlainFapi() {
		authorizationCodeGrantTypeProfileSteps = null;
		authorizationEndpointProfileSteps = null;
		accountsEndpointProfileSteps = null;
	}

	@VariantSetup(parameter = FAPIProfile.class, value = "openbanking_uk")
	public void setupOpenBankingUk() {
		authorizationCodeGrantTypeProfileSteps = AddOpenBankingUkClaimsToAuthorizationCodeGrant.class;
		authorizationEndpointProfileSteps = AddOpenBankingUkClaimsToAuthorizationEndpointResponse.class;
		accountsEndpointProfileSteps = GenerateOpenBankingUkAccountsEndpointResponse.class;
	}
}
