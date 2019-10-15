package net.openid.conformance.heart;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddClientAssertionToRevocationEndpointRequest;
import net.openid.conformance.condition.client.AddClientAssertionToTokenEndpointRequest;
import net.openid.conformance.condition.client.CallProtectedResourceWithBearerToken;
import net.openid.conformance.condition.client.CallProtectedResourceWithInactiveBearerToken;
import net.openid.conformance.condition.client.CallRevocationEndpoint;
import net.openid.conformance.condition.client.CallTokenEndpoint;
import net.openid.conformance.condition.client.CheckForAccessTokenValue;
import net.openid.conformance.condition.client.CheckHeartServerJwksFields;
import net.openid.conformance.condition.client.CheckIfTokenEndpointResponseError;
import net.openid.conformance.condition.client.CreateClientAuthenticationAssertionClaims;
import net.openid.conformance.condition.client.CreateClientAuthenticationAssertionClaimsForRevocationEndpoint;
import net.openid.conformance.condition.client.CreateJwksUri;
import net.openid.conformance.condition.client.CreateTokenEndpointRequestForClientCredentialsGrant;
import net.openid.conformance.condition.client.CreateTokenRevocationRequest;
import net.openid.conformance.condition.client.ExtractAccessTokenFromTokenResponse;
import net.openid.conformance.condition.client.ExtractJWKsFromStaticClientConfiguration;
import net.openid.conformance.condition.client.FetchServerKeys;
import net.openid.conformance.condition.client.GetDynamicServerConfiguration;
import net.openid.conformance.condition.client.GetResourceEndpointConfiguration;
import net.openid.conformance.condition.client.GetStaticClientConfiguration;
import net.openid.conformance.condition.client.SetProtectedResourceUrlToSingleResourceEndpoint;
import net.openid.conformance.condition.client.SignClientAuthenticationAssertion;
import net.openid.conformance.condition.client.ValidateClientJWKsPrivatePart;
import net.openid.conformance.condition.client.ValidateServerJWKs;
import net.openid.conformance.condition.common.CheckForKeyIdInServerJWKs;
import net.openid.conformance.condition.common.CheckHeartServerConfiguration;
import net.openid.conformance.condition.common.DisallowTLS10;
import net.openid.conformance.condition.common.DisallowTLS11;
import net.openid.conformance.condition.common.EnsureTLS12;
import net.openid.conformance.condition.common.SetTLSTestHostFromConfig;
import net.openid.conformance.testmodule.AbstractTestModule;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.testmodule.TestFailureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

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

	@Override
	public void configure(JsonObject config, String baseUrl, String externalUrlOverride) {
		env.putString("base_url", baseUrl);
		env.putObject("config", config);

		callAndStopOnFailure(SetTLSTestHostFromConfig.class);
		callAndStopOnFailure(EnsureTLS12.class, "HEART-OAuth2-6");
		callAndContinueOnFailure(DisallowTLS10.class, "HEART-OAuth2-6");
		callAndContinueOnFailure(DisallowTLS11.class, "HEART-OAuth2-6");

		// Get the server's configuration
		callAndContinueOnFailure(GetDynamicServerConfiguration.class, "HEART-OAuth2-3.1.5");

		// make sure the server configuration passes some basic sanity checks
		callAndStopOnFailure(CheckHeartServerConfiguration.class, "HEART-OAuth2-3.1.5");

		// fetch or load the server's keys as needed
		callAndStopOnFailure(FetchServerKeys.class, "HEART-OAuth2-3.1.5");
		callAndStopOnFailure(ValidateServerJWKs.class, "RFC7517-1.1");
		callAndStopOnFailure(CheckHeartServerJwksFields.class, "HEART-OAuth2-3.1.5");
		callAndContinueOnFailure(CheckForKeyIdInServerJWKs.class, Condition.ConditionResult.FAILURE, "OIDCC-10.1");

		// Set up the client configuration
		callAndStopOnFailure(GetStaticClientConfiguration.class);
		callAndStopOnFailure(ValidateClientJWKsPrivatePart.class, "RFC7517-1.1");
		callAndStopOnFailure(ExtractJWKsFromStaticClientConfiguration.class, "HEART-OAuth2-2.1.5");

		callAndStopOnFailure(CreateJwksUri.class);
		exposeEnvString("jwks_uri");

		exposeEnvString("client_id");

		callAndStopOnFailure(GetResourceEndpointConfiguration.class);
		expose("resourceUrl",env.getString("resource","resourceUrl"));
		expose("resourceMethod",env.getString("resource","resourceMethod"));
		callAndStopOnFailure(SetProtectedResourceUrlToSingleResourceEndpoint.class);

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
		JsonObject jwks = env.getObject("server_public_jwks");

		setStatus(Status.WAITING);

		return new ResponseEntity<Object>(jwks, HttpStatus.OK);
	}
}
