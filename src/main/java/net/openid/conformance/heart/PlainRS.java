package net.openid.conformance.heart;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.as.AddIntrospectionUrlToServerConfiguration;
import net.openid.conformance.condition.as.AddRevocationUrlToServerConfiguration;
import net.openid.conformance.condition.as.CopyAccessTokenFromASToClient;
import net.openid.conformance.condition.as.CreateIntrospectionResponse;
import net.openid.conformance.condition.as.EnsureResourceAssertionTypeIsJwt;
import net.openid.conformance.condition.as.ExtractAssertionFromIntrospectionRequest;
import net.openid.conformance.condition.as.ExtractJWKsFromResourceConfiguration;
import net.openid.conformance.condition.as.GenerateBearerAccessToken;
import net.openid.conformance.condition.as.GenerateServerConfiguration;
import net.openid.conformance.condition.as.GetStaticResourceConfiguration;
import net.openid.conformance.condition.as.LoadServerJWKs;
import net.openid.conformance.condition.as.ValidateResourceAssertionClaims;
import net.openid.conformance.condition.as.ValidateResourceAssertionSignature;
import net.openid.conformance.condition.client.CallProtectedResourceWithBearerToken;
import net.openid.conformance.condition.client.CheckHeartServerJwksFields;
import net.openid.conformance.condition.client.ExtractTLSTestValuesFromResourceConfiguration;
import net.openid.conformance.condition.client.GetStaticClientConfiguration;
import net.openid.conformance.condition.client.SetProtectedResourceUrlToSingleResourceEndpoint;
import net.openid.conformance.condition.client.ValidateServerJWKs;
import net.openid.conformance.condition.common.CheckDistinctKeyIdValueInServerJWKs;
import net.openid.conformance.condition.common.CheckForKeyIdInServerJWKs;
import net.openid.conformance.condition.common.CheckHeartServerConfiguration;
import net.openid.conformance.condition.common.CheckServerConfiguration;
import net.openid.conformance.condition.common.DisallowInsecureCipher;
import net.openid.conformance.condition.common.DisallowTLS10;
import net.openid.conformance.condition.common.DisallowTLS11;
import net.openid.conformance.condition.common.EnsureTLS12WithFAPICiphers;
import net.openid.conformance.condition.common.SetTLSTestHostFromConfig;
import net.openid.conformance.testmodule.AbstractTestModule;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.testmodule.TestFailureException;
import net.openid.conformance.condition.Condition;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@PublishTestModule(
	testName = "heart-rs-plain-get",
	displayName = "HEART RS with plain GET request",
	profile = "HEART",
	configurationFields = {
		"server.jwks",
		"client.client_id",
		"resource.resource_id",
		"resource.jwks",
		"resource.scope",
		"tls.testHost",
		"tls.testPort",
		"resource.resourceUrl",
		"resource.resourceMethod"
	}
)
public class PlainRS extends AbstractTestModule {

	@Override
	public void configure(JsonObject config, String baseUrl, String externalUrlOverride) {
		env.putString("base_url", baseUrl);
		env.putObject("config", config);

		callAndStopOnFailure(SetTLSTestHostFromConfig.class);
		callAndStopOnFailure(EnsureTLS12WithFAPICiphers.class, "HEART-OAuth2-6");
		callAndContinueOnFailure(DisallowTLS10.class, "HEART-OAuth2-6");
		callAndContinueOnFailure(DisallowTLS11.class, "HEART-OAuth2-6");

		callAndStopOnFailure(GenerateServerConfiguration.class, "HEART-OAuth2-3.1.5");

		callAndStopOnFailure(AddIntrospectionUrlToServerConfiguration.class, "HEART-OAuth2-3.1.5");
		callAndStopOnFailure(AddRevocationUrlToServerConfiguration.class, "HEART-OAuth2-3.1.5");

		exposeEnvString("discoveryUrl");
		exposeEnvString("issuer");

		callAndStopOnFailure(CheckServerConfiguration.class);


		// make sure the server configuration passes some basic sanity checks
		callAndStopOnFailure(CheckHeartServerConfiguration.class, "HEART-OAuth2-3.1.5");

		// load the server's keys as needed
		callAndStopOnFailure(LoadServerJWKs.class);

		callAndStopOnFailure(ValidateServerJWKs.class, "RFC7517-1.1");

		callAndStopOnFailure(CheckHeartServerJwksFields.class, "HEART-OAuth2-3.1.5");
		callAndContinueOnFailure(CheckForKeyIdInServerJWKs.class, Condition.ConditionResult.FAILURE, "OIDCC-10.1");
		callAndContinueOnFailure(CheckDistinctKeyIdValueInServerJWKs.class, Condition.ConditionResult.FAILURE, "RFC7517-4.5");

		// Set up the resource configuration
		callAndStopOnFailure(GetStaticResourceConfiguration.class);
		callAndStopOnFailure(SetProtectedResourceUrlToSingleResourceEndpoint.class);
		callAndStopOnFailure(ExtractTLSTestValuesFromResourceConfiguration.class);

		callAndStopOnFailure(ExtractJWKsFromResourceConfiguration.class, "HEART-OAuth2-2.1.5");

		exposeEnvString("resource_id");

		// get the client's ID
		callAndStopOnFailure(GetStaticClientConfiguration.class);

		exposeEnvString("client_id");

		setStatus(Status.CONFIGURED);
		fireSetupDone();
	}

	/* (non-Javadoc)
	 * @see io.bspk.selenium.TestModule#start()
	 */
	@Override
	public void start() {

		setStatus(Status.RUNNING);

		eventLog.startBlock("Resource Endpoint TLS test");
		env.mapKey("tls", "resource_endpoint_tls");
		callAndContinueOnFailure(EnsureTLS12WithFAPICiphers.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(DisallowTLS10.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(DisallowTLS11.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(DisallowInsecureCipher.class, Condition.ConditionResult.FAILURE);

		eventLog.endBlock();
		env.unmapKey("tls");

		// create an access token for the client to use
		callAndStopOnFailure(GenerateBearerAccessToken.class);

		exposeEnvString("access_token");

		callAndStopOnFailure(CopyAccessTokenFromASToClient.class);

		setStatus(Status.WAITING);
		// CallProtectedResourceWithBearerToken is accessing the Environment, so can't be called thread safely unless
		// the lock is held (which it isn't as the test status has been changed to WAITING). I'm presuming this test
		// also won't work correctly if the lock is held as I presume other conditions in this test module need to run
		// before the http call to the protected resource can return.
		// see https://gitlab.com/openid/conformance-suite/merge_requests/822
		callAndStopOnFailure(CallProtectedResourceWithBearerToken.class);
		setStatus(Status.RUNNING);

		setStatus(Status.FINISHED);
	}

	@Override
	public Object handleHttp(String path, HttpServletRequest req, HttpServletResponse res, HttpSession session, JsonObject requestParts) {
		// dispatch based on the path
		if (path.equals("introspect")) {
			return handleIntrospection(requestParts);
		} else {
			throw new TestFailureException(getId(), "Got unexpected HTTP call to " + path);
		}

	}

	/**
	 * @param requestParts
	 * @return
	 */
	private Object handleIntrospection(JsonObject requestParts) {

		setStatus(Status.RUNNING);

		env.putObject("introspection_request", requestParts);

		callAndStopOnFailure(ExtractAssertionFromIntrospectionRequest.class);

		callAndStopOnFailure(EnsureResourceAssertionTypeIsJwt.class);

		callAndStopOnFailure(ValidateResourceAssertionClaims.class);

		callAndStopOnFailure(ValidateResourceAssertionSignature.class);

		callAndStopOnFailure(CreateIntrospectionResponse.class);

		setStatus(Status.WAITING);

		return new ResponseEntity<>(env.getObject("introspection_response"), HttpStatus.OK);

	}

}
