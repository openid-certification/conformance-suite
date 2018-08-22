package io.fintechlabs.testframework.openbanking;

import java.util.Enumeration;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.view.RedirectView;

import com.google.gson.JsonObject;

import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.condition.as.AuthenticateClientWithClientSecret;
import io.fintechlabs.testframework.condition.as.CheckForClientCertificate;
import io.fintechlabs.testframework.condition.as.CopyAccessTokenToClientCredentialsField;
import io.fintechlabs.testframework.condition.as.CreateAuthorizationCode;
import io.fintechlabs.testframework.condition.as.CreateTokenEndpointResponse;
import io.fintechlabs.testframework.condition.as.EnsureClientIsAuthenticated;
import io.fintechlabs.testframework.condition.as.EnsureMatchingClientCertificate;
import io.fintechlabs.testframework.condition.as.EnsureMatchingClientId;
import io.fintechlabs.testframework.condition.as.EnsureMatchingRedirectUri;
import io.fintechlabs.testframework.condition.as.EnsureMinimumKeyLength;
import io.fintechlabs.testframework.condition.as.ExtractClientCertificateFromTokenEndpointRequestHeaders;
import io.fintechlabs.testframework.condition.as.ExtractClientCredentialsFromBasicAuthorizationHeader;
import io.fintechlabs.testframework.condition.as.ExtractClientCredentialsFromFormPost;
import io.fintechlabs.testframework.condition.as.ExtractRequestedScopes;
import io.fintechlabs.testframework.condition.as.FilterUserInfoForScopes;
import io.fintechlabs.testframework.condition.as.GenerateBearerAccessToken;
import io.fintechlabs.testframework.condition.as.GenerateIdTokenClaims;
import io.fintechlabs.testframework.condition.as.GenerateServerConfigurationMTLS;
import io.fintechlabs.testframework.condition.as.LoadJWKs;
import io.fintechlabs.testframework.condition.as.RedirectBackToClientWithAuthorizationCode;
import io.fintechlabs.testframework.condition.as.SignIdToken;
import io.fintechlabs.testframework.condition.as.ValidateAuthorizationCode;
import io.fintechlabs.testframework.condition.as.ValidateRedirectUri;

import io.fintechlabs.testframework.condition.client.GetStaticClientConfiguration;
import io.fintechlabs.testframework.condition.common.CheckServerConfiguration;
import io.fintechlabs.testframework.condition.common.EnsureMinimumClientSecretEntropy;
import io.fintechlabs.testframework.condition.rs.CreateOpenBankingAccountRequestResponse;
import io.fintechlabs.testframework.condition.rs.CreateOpenBankingAccountsResponse;
import io.fintechlabs.testframework.condition.rs.ExtractBearerAccessTokenFromHeader;
import io.fintechlabs.testframework.condition.rs.ExtractBearerAccessTokenFromParams;
import io.fintechlabs.testframework.condition.rs.GenerateAccountRequestId;
import io.fintechlabs.testframework.condition.rs.GenerateOpenBankingAccountId;
import io.fintechlabs.testframework.condition.rs.LoadUserInfo;
import io.fintechlabs.testframework.condition.rs.RequireBearerAccessToken;
import io.fintechlabs.testframework.condition.rs.RequireBearerClientCredentialsAccessToken;
import io.fintechlabs.testframework.condition.rs.RequireOpenIDScope;
import io.fintechlabs.testframework.frontChannel.BrowserControl;
import io.fintechlabs.testframework.info.TestInfoService;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.runner.TestExecutionManager;
import io.fintechlabs.testframework.testmodule.AbstractTestModule;
import io.fintechlabs.testframework.testmodule.PublishTestModule;
import io.fintechlabs.testframework.testmodule.TestFailureException;
import io.fintechlabs.testframework.testmodule.UserFacing;

@PublishTestModule(
	testName = "ob-client-test-code-with-client-secret-basic-and-matls",
	displayName = "OB: client test (code with client_secret_basic authentication and MATLS)",
	profile = "OB",
	configurationFields = {
		"server.jwks",
		"client.client_id",
		"client.client_secret",
		"client.scope",
		"client.redirect_uri"
	}
)

public class OBClientTestCodeWithSecretBasicAndMATLS extends AbstractTestModule {

	public OBClientTestCodeWithSecretBasicAndMATLS(String id, Map<String, String> owner, TestInstanceEventLog eventLog, BrowserControl browser, TestInfoService testInfo, TestExecutionManager executionManager) {
		super(id, owner, eventLog, browser, testInfo, executionManager);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.testmodule.TestModule#configure(com.google.gson.JsonObject, io.fintechlabs.testframework.logging.EventLog, java.lang.String, io.fintechlabs.testframework.frontChannel.BrowserControl, java.lang.String)
	 */
	@Override
	public void configure(JsonObject config, String baseUrl) {
		env.putString("base_url", baseUrl);
		env.putObject("config", config);

		callAndStopOnFailure(GenerateServerConfigurationMTLS.class);
		exposeEnvString("discoveryUrl");
		exposeEnvString("issuer");

		callAndStopOnFailure(CheckServerConfiguration.class);

		callAndStopOnFailure(LoadJWKs.class);

		callAndStopOnFailure(EnsureMinimumKeyLength.class, "FAPI-1-5.2.2-5", "FAPI-1-5.2.2-6");

		callAndStopOnFailure(LoadUserInfo.class);

		callAndStopOnFailure(GetStaticClientConfiguration.class);

		call(EnsureMinimumClientSecretEntropy.class, ConditionResult.FAILURE, "RFC6819-5.1.4.2-2", "RFC6749-10.10");

		setStatus(Status.CONFIGURED);
		fireSetupDone();
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.testmodule.TestModule#start()
	 */
	@Override
	public void start() {
		setStatus(Status.RUNNING);
		// nothing to do here
		setStatus(Status.WAITING);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.testmodule.TestModule#handleHttp(java.lang.String, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, javax.servlet.http.HttpSession, org.springframework.util.MultiValueMap, org.springframework.ui.Model)
	 */
	@Override
	public Object handleHttp(String path, HttpServletRequest req, HttpServletResponse res, HttpSession session, JsonObject requestParts) {

		if (path.equals("authorize")) {
			return authorizationEndpoint(requestParts);
		} else if (path.equals("token")) {
			return tokenEndpoint(requestParts);
		} else if (path.equals("jwks")) {
			return jwksEndpoint();
		} else if (path.equals("userinfo")) {
			return userinfoEndpoint(requestParts);
		} else if (path.equals(".well-known/openid-configuration")) {
			return discoveryEndpoint();
		} else if (path.equals("open-banking/v1.1/account-requests")) {
			return accountRequestsEndpoint(requestParts);
		} else if (path.equals("open-banking/v1.1/accounts")) {
			return accountsEndpoint(requestParts);
		} else {
			throw new TestFailureException(getId(), "Got unexpected HTTP call to " + path);
		}

	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.testmodule.TestModule#handleHttpMtls(java.lang.String, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, javax.servlet.http.HttpSession, com.google.gson.JsonObject)
	 */
	@Override
	public Object handleHttpMtls(String path, HttpServletRequest req, HttpServletResponse res, HttpSession session, JsonObject requestParts) {

		// Update the environment with the current request headers
		JsonObject clientHeaders = new JsonObject();
		Enumeration<String> headerNames = req.getHeaderNames();
		while (headerNames.hasMoreElements()) {
			String name = headerNames.nextElement();
			clientHeaders.addProperty(name, req.getHeader(name));
		}
		env.putObject("client_request_headers", clientHeaders);

		if (path.equals("authorize")) {
			return authorizationEndpoint(requestParts);
		} else if (path.equals("token")) {
			return tokenEndpoint(requestParts);
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

	private Object userinfoEndpoint(JsonObject requestParts) {

		setStatus(Status.RUNNING);

		env.putObject("incoming_request", requestParts);

		call(ExtractBearerAccessTokenFromHeader.class);
		call(ExtractBearerAccessTokenFromParams.class);

		callAndStopOnFailure(RequireBearerAccessToken.class);

		callAndStopOnFailure(RequireOpenIDScope.class, "FAPI-1-5.2.3-7");

		callAndStopOnFailure(FilterUserInfoForScopes.class);

		JsonObject user = env.getObject("user_info_endpoint_response");

		setStatus(Status.WAITING);

		return new ResponseEntity<Object>(user, HttpStatus.OK);

	}

	private Object jwksEndpoint() {

		setStatus(Status.RUNNING);
		JsonObject jwks = env.getObject("server_public_jwks");

		setStatus(Status.WAITING);

		return new ResponseEntity<Object>(jwks, HttpStatus.OK);
	}

	private Object tokenEndpoint(JsonObject requestParts) {

		setStatus(Status.RUNNING);

		env.putObject("token_endpoint_request", requestParts);

		call(ExtractClientCertificateFromTokenEndpointRequestHeaders.class);

		callAndStopOnFailure(CheckForClientCertificate.class, "OB-5.2.4");

		call(EnsureMatchingClientCertificate.class);

		call(ExtractClientCredentialsFromBasicAuthorizationHeader.class);

		call(ExtractClientCredentialsFromFormPost.class); // FIXME: is this meant to be client secret post or basic?

		call(AuthenticateClientWithClientSecret.class);

		callAndStopOnFailure(EnsureClientIsAuthenticated.class);

		// dispatch based on grant type
		String grantType = requestParts.get("params").getAsJsonObject().get("grant_type").getAsString();

		if (grantType.equals("authorization_code")) {
			// we're doing the authorization code grant for user access
			return authorizationCodeGrantType(requestParts);
		} else if (grantType.equals("client_credentials")) {
			// we're doing the client credentials grant for initial token access
			return clientCredentialsGrantType(requestParts);
		} else {
			throw new TestFailureException(getId(), "Got a grant type on the token endpoint we didn't understand: " + grantType);
		}

	}

	private Object clientCredentialsGrantType(JsonObject requestParts) {

		callAndStopOnFailure(GenerateBearerAccessToken.class);

		callAndStopOnFailure(CreateTokenEndpointResponse.class);

		// this puts the client credentials specific token into its own box for later
		callAndStopOnFailure(CopyAccessTokenToClientCredentialsField.class);

		setStatus(Status.WAITING);

		return new ResponseEntity<Object>(env.getObject("token_endpoint_response"), HttpStatus.OK);

	}

	private Object authorizationCodeGrantType(JsonObject requestParts) {

		callAndStopOnFailure(ValidateAuthorizationCode.class);

		callAndStopOnFailure(ValidateRedirectUri.class);

		callAndStopOnFailure(GenerateBearerAccessToken.class);

		callAndStopOnFailure(GenerateIdTokenClaims.class);

		callAndStopOnFailure(SignIdToken.class);

		callAndStopOnFailure(CreateTokenEndpointResponse.class);

		setStatus(Status.WAITING);

		return new ResponseEntity<Object>(env.getObject("token_endpoint_response"), HttpStatus.OK);

	}

	@UserFacing
	private Object authorizationEndpoint(JsonObject requestParts) {

		setStatus(Status.RUNNING);

		env.putObject("authorization_endpoint_request", requestParts.get("params").getAsJsonObject());

		callAndStopOnFailure(EnsureMatchingClientId.class);

		callAndStopOnFailure(EnsureMatchingRedirectUri.class);

		callAndStopOnFailure(ExtractRequestedScopes.class);

		callAndStopOnFailure(CreateAuthorizationCode.class);

		callAndStopOnFailure(RedirectBackToClientWithAuthorizationCode.class);

		exposeEnvString("authorization_endpoint_response_redirect");

		String redirectTo = env.getString("authorization_endpoint_response_redirect");

		setStatus(Status.WAITING);

		return new RedirectView(redirectTo, false, false, false);

	}

	/**
	 * OpenBanking account request API
	 *
	 * @param requestParts
	 * @return
	 */
	private Object accountRequestsEndpoint(JsonObject requestParts) {

		env.putObject("incoming_request", requestParts);

		call(ExtractBearerAccessTokenFromHeader.class);
		call(ExtractBearerAccessTokenFromParams.class);

		callAndStopOnFailure(RequireBearerClientCredentialsAccessToken.class);

		callAndStopOnFailure(GenerateAccountRequestId.class);
		exposeEnvString("account_request_id");

		callAndStopOnFailure(CreateOpenBankingAccountRequestResponse.class);

		JsonObject accountRequestResponse = env.getObject("account_request_response");

		return new ResponseEntity<Object>(accountRequestResponse, HttpStatus.OK);
	}

	private Object accountsEndpoint(JsonObject requestParts) {
		setStatus(Status.RUNNING);

		env.putObject("incoming_request", requestParts);

		call(ExtractBearerAccessTokenFromHeader.class);
		call(ExtractBearerAccessTokenFromParams.class);

		callAndStopOnFailure(RequireBearerAccessToken.class);

		callAndStopOnFailure(GenerateOpenBankingAccountId.class);
		exposeEnvString("account_id");

		callAndStopOnFailure(CreateOpenBankingAccountsResponse.class);

		// at this point we can assume the test is fully done
		fireTestFinished();

		return new ResponseEntity<>(env.getObject("accounts_endpoint_response"), HttpStatus.OK);
	}

}
