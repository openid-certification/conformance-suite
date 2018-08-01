package io.fintechlabs.testframework.heart;

import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.Condition;
import io.fintechlabs.testframework.condition.client.*;
import io.fintechlabs.testframework.condition.common.*;
import io.fintechlabs.testframework.frontChannel.BrowserControl;
import io.fintechlabs.testframework.info.TestInfoService;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.runner.TestExecutionManager;
import io.fintechlabs.testframework.testmodule.AbstractTestModule;
import io.fintechlabs.testframework.testmodule.PublishTestModule;
import io.fintechlabs.testframework.testmodule.TestFailureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Map;

/**
 * @author srmoore
 */
@PublishTestModule(
	testName = "heart-token-revocation",
	displayName = "HEART AS: Token Revocation",
	profile = "HEART",
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.jwks",
		"client.scope",
		"tls.testHost",
		"tls.testPort",
		"resource.resourceUrl",
		"resource.resourceMethod"
	}
)
public class TokenRevocationAS extends AbstractTestModule {

	public static Logger logger = LoggerFactory.getLogger(TokenRevocationAS.class);

	public TokenRevocationAS(String id, Map<String, String> owner, TestInstanceEventLog eventLog, BrowserControl browser, TestInfoService testInfo, TestExecutionManager executionManager) {
		super(id, owner, eventLog, browser, testInfo, executionManager);
	}


	@Override
	public void configure(JsonObject config, String baseUrl) {
		env.putString("base_url", baseUrl);
		env.put("config", config);

		callAndStopOnFailure(SetTLSTestHostFromConfig.class);
		callAndStopOnFailure(EnsureTLS12.class, "HEART-OAuth2-6");
		call(DisallowTLS10.class, "HEART-OAuth2-6");
		call(DisallowTLS11.class, "HEART-OAuth2-6");

		// Get the server's configuration
		call(GetDynamicServerConfiguration.class, "HEART-OAuth2-3.1.5");

		// make sure the server configuration passes some basic sanity checks
		callAndStopOnFailure(CheckHeartServerConfiguration.class, "HEART-OAuth2-3.1.5");

		// fetch or load the server's keys as needed
		callAndStopOnFailure(FetchServerKeys.class, "HEART-OAuth2-3.1.5");
		callAndStopOnFailure(CheckHeartServerJwksFields.class, "HEART-OAuth2-3.1.5");
		callAndStopOnFailure(CheckForKeyIdInJWKs.class, "OIDCC-10.1");

		// Set up the client configuration
		callAndStopOnFailure(GetStaticClientConfiguration.class);

		callAndStopOnFailure(ExtractJWKsFromClientConfiguration.class, "HEART-OAuth2-2.1.5");

		callAndStopOnFailure(CreateJwksUri.class);
		exposeEnvString("jwks_uri");

		exposeEnvString("client_id");

		callAndStopOnFailure(GetResourceEndpointConfiguration.class);
		expose("resourceUrl",env.getString("resoruce","resourceUrl"));
		expose("resourceMethod",env.getString("resoruce","resourceMethod"));

		setStatus(Status.CONFIGURED);
		fireSetupDone();
	}

	@Override
	public void start() {

		setStatus(Status.RUNNING);

		callAndStopOnFailure(CreateTokenEndpointRequestForClientCredentialsGrant.class);

		// authenticate using a signed assertion
		callAndStopOnFailure(CreateClientAuthenticationAssertionClaims.class, "HEART-OAuth2-2.2.2");
		callAndStopOnFailure(SignClientAuthenticationAssertion.class, "HEART-OAuth2-2.2.2");
		callAndStopOnFailure(AddClientAssertionToTokenEndpointRequest.class, "HEART-OAuth2-2.2.2");

		callAndStopOnFailure(CallTokenEndpoint.class);

		callAndStopOnFailure(CheckIfTokenEndpointResponseError.class);

		callAndStopOnFailure(CheckForAccessTokenValue.class);

		callAndStopOnFailure(ExtractAccessTokenFromTokenResponse.class);

		// callAndStopOnFailure(ParseAccessTokenAsJwt.class, "HEART-OAuth2-3.2.1");

		// callAndStopOnFailure(ValidateAccessTokenSignature.class, "HEART-OAuth2-3.2.1");

		// call(ValidateAccessTokenHeartClaims.class, Condition.ConditionResult.FAILURE, "HEART-OAuth2-3.2.1");

		// call(CheckForScopesInTokenResponse.class);

		// callAndStopOnFailure(EnsureNoRefreshToken.class);

		// Try the token on a resource server
		callAndStopOnFailure(CallProtectedResourceWithBearerToken.class);


		callAndStopOnFailure(CreateTokenRevocationRequest.class);
		callAndStopOnFailure(CreateClientAuthenticationAssertionClaimsForRevocationEndpoint.class, "HEART-OAuth2-2.2.2");
		callAndStopOnFailure(SignClientAuthenticationAssertion.class, "HEART-OAuth2-2.2.2");
		callAndStopOnFailure(AddClientAssertionToRevocationEndpointRequest.class, "HEART-OAuth2-2.2.2");
		callAndStopOnFailure(CallRevocationEndpoint.class);
		callAndStopOnFailure(CallProtectedResourceWithInactiveBearerToken.class);

		fireTestFinished();
		stop();
	}

	@Override
	public Object handleHttp(String path, HttpServletRequest req, HttpServletResponse res, HttpSession session, JsonObject requestParts) {
		// dispatch based on the path
		if (path.equals("jwks")) {
			return handleJwks(requestParts);
		} else {
			throw new TestFailureException(getId(), "Got an HTTP response we weren't expecting");
		}
	}

	private Object handleJwks(JsonObject requestParts) {
		setStatus(Status.RUNNING);
		JsonObject jwks = env.get("public_jwks");

		setStatus(Status.WAITING);

		return new ResponseEntity<Object>(jwks, HttpStatus.OK);
	}
}
