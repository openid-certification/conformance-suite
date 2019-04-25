package io.fintechlabs.testframework.fapi;

import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.condition.as.AddACRClaimToIdTokenClaims;
import io.fintechlabs.testframework.condition.as.AddAtHashToIdTokenClaims;
import io.fintechlabs.testframework.condition.as.AddCHashToIdTokenClaims;
import io.fintechlabs.testframework.condition.as.AddResponseTypeCodeIdTokenToServerConfiguration;
import io.fintechlabs.testframework.condition.as.AddSHashToIdTokenClaims;
import io.fintechlabs.testframework.condition.as.AddTokenEndpointSigningAlg;
import io.fintechlabs.testframework.condition.as.CalculateAtHash;
import io.fintechlabs.testframework.condition.as.CalculateCHash;
import io.fintechlabs.testframework.condition.as.CalculateSHash;
import io.fintechlabs.testframework.condition.as.CheckForClientCertificate;
import io.fintechlabs.testframework.condition.as.CopyAccessTokenToClientCredentialsField;
import io.fintechlabs.testframework.condition.as.CreateAuthorizationCode;
import io.fintechlabs.testframework.condition.as.CreateFapiInteractionIdIfNeeded;
import io.fintechlabs.testframework.condition.as.CreateTokenEndpointResponse;
import io.fintechlabs.testframework.condition.as.EnsureAuthorizationParametersMatchRequestObject;
import io.fintechlabs.testframework.condition.as.EnsureClientCertificateMatches;
import io.fintechlabs.testframework.condition.as.EnsureMatchingClientId;
import io.fintechlabs.testframework.condition.as.EnsureMatchingRedirectUri;
import io.fintechlabs.testframework.condition.as.EnsureMinimumKeyLength;
import io.fintechlabs.testframework.condition.as.EnsureOpenIDInScopeRequest;
import io.fintechlabs.testframework.condition.as.EnsureResponseTypeIsCodeIdToken;
import io.fintechlabs.testframework.condition.as.ExtractClientCertificateFromTokenEndpointRequestHeaders;
import io.fintechlabs.testframework.condition.as.ExtractNonceFromAuthorizationRequest;
import io.fintechlabs.testframework.condition.as.ExtractRequestObject;
import io.fintechlabs.testframework.condition.as.ExtractRequestedScopes;
import io.fintechlabs.testframework.condition.as.ExtractServerSigningAlg;
import io.fintechlabs.testframework.condition.as.FAPIValidateRequestObjectSigningAlg;
import io.fintechlabs.testframework.condition.as.FilterUserInfoForScopes;
import io.fintechlabs.testframework.condition.as.GenerateBearerAccessToken;
import io.fintechlabs.testframework.condition.as.GenerateIdTokenClaims;
import io.fintechlabs.testframework.condition.as.GenerateServerConfigurationMTLS;
import io.fintechlabs.testframework.condition.as.LoadServerJWKs;
import io.fintechlabs.testframework.condition.as.RedirectBackToClientWithAuthorizationCodeAndIdToken;
import io.fintechlabs.testframework.condition.as.SignIdToken;
import io.fintechlabs.testframework.condition.as.ValidateAuthorizationCode;
import io.fintechlabs.testframework.condition.as.ValidateRedirectUri;
import io.fintechlabs.testframework.condition.as.ValidateRequestObjectClaims;
import io.fintechlabs.testframework.condition.as.ValidateRequestObjectExp;
import io.fintechlabs.testframework.condition.as.ValidateRequestObjectSignature;
import io.fintechlabs.testframework.condition.client.ExtractJWKsFromClientConfiguration;
import io.fintechlabs.testframework.condition.client.FAPIValidateRequestObjectIdTokenACRClaims;
import io.fintechlabs.testframework.condition.client.GetStaticClientConfiguration;
import io.fintechlabs.testframework.condition.common.CheckServerConfiguration;
import io.fintechlabs.testframework.condition.common.EnsureIncomingTls12;
import io.fintechlabs.testframework.condition.common.EnsureIncomingTlsSecureCipher;
import io.fintechlabs.testframework.condition.common.EnsureMinimumClientSecretEntropy;
import io.fintechlabs.testframework.condition.rs.ClearAccessTokenFromRequest;
import io.fintechlabs.testframework.condition.rs.CreateFAPIAccountEndpointResponse;
import io.fintechlabs.testframework.condition.rs.CreateOpenBankingAccountRequestResponse;
import io.fintechlabs.testframework.condition.rs.EnsureBearerAccessTokenNotInParams;
import io.fintechlabs.testframework.condition.rs.ExtractBearerAccessTokenFromHeader;
import io.fintechlabs.testframework.condition.rs.ExtractFapiDateHeader;
import io.fintechlabs.testframework.condition.rs.ExtractFapiInteractionIdHeader;
import io.fintechlabs.testframework.condition.rs.ExtractFapiIpAddressHeader;
import io.fintechlabs.testframework.condition.rs.GenerateAccountRequestId;
import io.fintechlabs.testframework.condition.rs.LoadUserInfo;
import io.fintechlabs.testframework.condition.rs.RequireBearerAccessToken;
import io.fintechlabs.testframework.condition.rs.RequireBearerClientCredentialsAccessToken;
import io.fintechlabs.testframework.condition.rs.RequireOpenIDScope;
import io.fintechlabs.testframework.testmodule.AbstractTestModule;
import io.fintechlabs.testframework.testmodule.TestFailureException;
import io.fintechlabs.testframework.testmodule.UserFacing;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public abstract class AbstractFAPIRWID2ClientTest extends AbstractTestModule {

	public static final String ACCOUNT_REQUESTS_PATH = "open-banking/v1.1/account-requests";
	public static final String ACCOUNTS_PATH = "open-banking/v1.1/accounts";

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

	protected abstract void addTokenEndpointAuthMethodSupported();

	protected abstract void validateClientAuthentication();

	protected abstract void addCustomValuesToIdToken();

	protected void addCustomSignatureOfIdToken(){}

	protected void authorizationCodeGrantTypeProfile(){}

	protected void authorizationEndpointProfile(){}

	protected void accountsEndpointProfile(){}

	protected boolean endTestIfStateIsNotSupplied(){return false;}

	@Override
	public void configure(JsonObject config, String baseUrl) {
		env.putString("base_url", baseUrl);
		env.putObject("config", config);

		callAndStopOnFailure(GenerateServerConfigurationMTLS.class);

		addTokenEndpointAuthMethodSupported();

		callAndStopOnFailure(AddResponseTypeCodeIdTokenToServerConfiguration.class);
		callAndStopOnFailure(AddTokenEndpointSigningAlg.class);
		exposeEnvString("discoveryUrl");
		exposeEnvString("issuer");

		exposePath("accounts_endpoint", ACCOUNTS_PATH);
		exposePath("account_requests_endpoint", ACCOUNT_REQUESTS_PATH);

		callAndStopOnFailure(CheckServerConfiguration.class);

		callAndStopOnFailure(LoadServerJWKs.class);

		callAndStopOnFailure(EnsureMinimumKeyLength.class, "FAPI-R-5.2.2-5", "FAPI-R-5.2.2-6");

		callAndStopOnFailure(LoadUserInfo.class);

		callAndStopOnFailure(GetStaticClientConfiguration.class);

		callAndContinueOnFailure(EnsureMinimumClientSecretEntropy.class, ConditionResult.FAILURE, "RFC6819-5.1.4.2-2", "RFC6749-10.10");

		// for signing request objects
		callAndStopOnFailure(ExtractJWKsFromClientConfiguration.class);

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

		String requestId = "incoming_request_" + RandomStringUtils.randomAlphanumeric(37);

		env.putObject(requestId, requestParts);

		call(exec().mapKey("client_request", requestId));

		callAndContinueOnFailure(EnsureIncomingTls12.class, "FAPI-R-7.1-1");
		callAndContinueOnFailure(EnsureIncomingTlsSecureCipher.class, ConditionResult.FAILURE, "FAPI-R-7.1-1");

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
		} else if (path.equals(ACCOUNTS_PATH)) {
			return accountsEndpoint(requestId);
		} else {
			throw new TestFailureException(getId(), "Got unexpected HTTP call to " + path);
		}
	}

	@Override
	public Object handleHttpMtls(String path, HttpServletRequest req, HttpServletResponse res, HttpSession session, JsonObject requestParts) {

		setStatus(Status.RUNNING);

		String requestId = "incoming_request_" + RandomStringUtils.randomAlphanumeric(37);

		env.putObject(requestId, requestParts);

		call(exec().mapKey("client_request", requestId));

		callAndContinueOnFailure(EnsureIncomingTls12.class, ConditionResult.FAILURE, "FAPI-R-7.1-1");
		callAndContinueOnFailure(EnsureIncomingTlsSecureCipher.class, ConditionResult.FAILURE, "FAPI-R-7.1-1");

		call(exec().unmapKey("client_request"));

		setStatus(Status.WAITING);

		if (path.equals("token")) {
			return tokenEndpoint(requestId);
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

		callAndStopOnFailure(RequireOpenIDScope.class, "FAPI-R-5.2.3-7");

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

		callAndContinueOnFailure(ExtractClientCertificateFromTokenEndpointRequestHeaders.class);

		callAndStopOnFailure(CheckForClientCertificate.class, "FAPI-RW-5.2.2-5");

		callAndStopOnFailure(EnsureClientCertificateMatches.class);

		validateClientAuthentication();

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

		callAndStopOnFailure(GenerateBearerAccessToken.class);

		callAndStopOnFailure(GenerateIdTokenClaims.class);

		authorizationCodeGrantTypeProfile();

		callAndStopOnFailure(SignIdToken.class);

		callAndStopOnFailure(CreateTokenEndpointResponse.class);

		call(exec().unmapKey("token_endpoint_request").endBlock());

		setStatus(Status.WAITING);

		return new ResponseEntity<Object>(env.getObject("token_endpoint_response"), HttpStatus.OK);

	}

	@UserFacing
	protected Object authorizationEndpoint(String requestId) {

		setStatus(Status.RUNNING);

		call(exec().startBlock("Authorization endpoint")
			.mapKey("authorization_endpoint_request", requestId));

		callAndStopOnFailure(ExtractRequestObject.class, "FAPI-RW-5.2.2-10");

		if (endTestIfStateIsNotSupplied()){return true;}

		callAndStopOnFailure(EnsureAuthorizationParametersMatchRequestObject.class);

		callAndStopOnFailure(FAPIValidateRequestObjectSigningAlg.class, "FAPI-RW-8.6");

		callAndStopOnFailure(FAPIValidateRequestObjectIdTokenACRClaims.class, "FAPI-RW-5.2.3-5");

		callAndStopOnFailure(ValidateRequestObjectExp.class, "RFC7519-4.1.4", "FAPI-RW-5.2.2.13");

		callAndStopOnFailure(ValidateRequestObjectClaims.class);

		callAndStopOnFailure(ValidateRequestObjectSignature.class, "FAPI-RW-5.2.2.1");

		callAndStopOnFailure(EnsureResponseTypeIsCodeIdToken.class, "OIDCC-6.1");

		callAndStopOnFailure(EnsureMatchingClientId.class, "OIDCC-3.1.2.1");

		callAndStopOnFailure(EnsureMatchingRedirectUri.class);

		callAndStopOnFailure(ExtractRequestedScopes.class);

		callAndStopOnFailure(EnsureOpenIDInScopeRequest.class, "FAPI-R-5.2.3-7");

		callAndStopOnFailure(ExtractNonceFromAuthorizationRequest.class, "FAPI-R-5.2.3-8");

		callAndStopOnFailure(CreateAuthorizationCode.class);

		callAndStopOnFailure(ExtractServerSigningAlg.class);

		callAndStopOnFailure(CalculateCHash.class, "OIDCC-3.3.2.11");

		skipIfElementMissing("authorization_request_object", "claims.state", ConditionResult.INFO,
			CalculateSHash.class, ConditionResult.FAILURE, "FAPI-RW-5.2.2-4");

		callAndStopOnFailure(GenerateBearerAccessToken.class);

		callAndStopOnFailure(CalculateAtHash.class, "OIDCC-3.3.2.11");

		callAndStopOnFailure(GenerateIdTokenClaims.class);

		authorizationEndpointProfile();

		callAndStopOnFailure(AddCHashToIdTokenClaims.class, "OIDCC-3.3.2.11");

		skipIfMissing(null, new String[] {"s_hash"}, ConditionResult.INFO,
			AddSHashToIdTokenClaims.class, ConditionResult.FAILURE, "FAPI-RW-5.2.2-4");

		callAndStopOnFailure(AddAtHashToIdTokenClaims.class, "OIDCC-3.3.2.11");

		addCustomValuesToIdToken();

		callAndStopOnFailure(AddACRClaimToIdTokenClaims.class,  "OIDCC-3.1.3.7-12");

		callAndStopOnFailure(SignIdToken.class);

		addCustomSignatureOfIdToken();

		callAndStopOnFailure(RedirectBackToClientWithAuthorizationCodeAndIdToken.class, "OIDCC-3.3.2.5");

		exposeEnvString("authorization_endpoint_response_redirect");

		String redirectTo = env.getString("authorization_endpoint_response_redirect");

		setStatus(Status.WAITING);

		call(exec().unmapKey("authorization_endpoint_request").endBlock());

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

		callAndStopOnFailure(GenerateAccountRequestId.class);
		exposeEnvString("account_request_id");

		callAndStopOnFailure(CreateFapiInteractionIdIfNeeded.class, "FAPI-R-6.2.1-12");

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

		call(exec().startBlock("Accounts endpoint")
			.mapKey("incoming_request", requestId));

		callAndStopOnFailure(EnsureBearerAccessTokenNotInParams.class, "FAPI-R-6.2.2-1");
		callAndStopOnFailure(ExtractBearerAccessTokenFromHeader.class, "FAPI-R-6.2.2-1");

		callAndStopOnFailure(RequireBearerAccessToken.class);

		// TODO: should we clear the old headers?
		callAndContinueOnFailure(ExtractFapiDateHeader.class, ConditionResult.INFO, "FAPI-R-6.2.2-3");
		callAndContinueOnFailure(ExtractFapiIpAddressHeader.class, ConditionResult.INFO, "FAPI-R-6.2.2-4");
		callAndContinueOnFailure(ExtractFapiInteractionIdHeader.class, ConditionResult.INFO, "FAPI-R-6.2.2-4");

		callAndStopOnFailure(CreateFapiInteractionIdIfNeeded.class, "FAPI-R-6.2.1-12");

		callAndStopOnFailure(CreateFAPIAccountEndpointResponse.class);

		accountsEndpointProfile();

		callAndStopOnFailure(ClearAccessTokenFromRequest.class);

		call(exec().unmapKey("incoming_request").endBlock());

		JsonObject accountsEndpointResponse = env.getObject("accounts_endpoint_response");
		JsonObject headerJson = env.getObject("accounts_endpoint_response_headers");

		setStatus(Status.WAITING);

		// at this point we can assume the test is fully done
		fireTestFinished();

		return new ResponseEntity<>(accountsEndpointResponse, headersFromJson(headerJson), HttpStatus.OK);
	}

}
