package net.openid.conformance.openid;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.google.gson.JsonObject;

import net.openid.conformance.condition.client.AddJwksUriToDynamicRegistrationRequest;
import net.openid.conformance.condition.client.AddPublicJwksToDynamicRegistrationRequest;
import net.openid.conformance.condition.client.CreateJwksUri;
import net.openid.conformance.sequence.client.OIDCCCreateDynamicClientRegistrationRequest;
import net.openid.conformance.testmodule.PublishTestModule;

// Corresponds to https://www.heenan.me.uk/~joseph/oidcc_test_desc-phase1.html#OP_Registration_jwks_uri
@PublishTestModule(
	testName = "oidcc-registration-jwks-uri",
	displayName = "OIDCC: dynamic registration with JWKS URI",
	summary = "This test calls the dynamic registration endpoint with a jwks URI. This should result in a successful registration.",
	profile = "OIDCC",
	configurationFields = {
		"server.discoveryUrl"
	}
)
public class OIDCCRegistrationJwksUri extends AbstractOIDCCDynamicRegistrationTest {

	protected void onConfigure(JsonObject config, String baseUrl) {
		callAndStopOnFailure(CreateJwksUri.class);
	}

	@Override
	protected void createDynamicClientRegistrationRequest() {
		call(new OIDCCCreateDynamicClientRegistrationRequest(responseType)
				.replace(AddPublicJwksToDynamicRegistrationRequest.class,
						condition(AddJwksUriToDynamicRegistrationRequest.class)));
	}

	@Override
	protected void performAuthorizationFlow() {
		// Don't need to test authorization here.
		fireTestFinished();
	}

	@Override
	public Object handleHttp(String path, HttpServletRequest req, HttpServletResponse res, HttpSession session, JsonObject requestParts) {
		if (path.equals("jwks")) {
			return handleJwksRequest(requestParts);
		} else {
			return super.handleHttp(path, req, res, session, requestParts);
		}
	}

	private Object handleJwksRequest(JsonObject requestParts) {
		return env.getObject("client_public_jwks");
	}
}
