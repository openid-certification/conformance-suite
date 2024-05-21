package net.openid.conformance.openid;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.client.AddJwksUriToDynamicRegistrationRequest;
import net.openid.conformance.condition.client.AddPublicJwksToDynamicRegistrationRequest;
import net.openid.conformance.condition.client.CreateJwksUri;
import net.openid.conformance.condition.client.GenerateRS256ClientJWKs;
import net.openid.conformance.condition.client.GenerateRS256ClientJWKsWithKeyID;
import net.openid.conformance.sequence.client.OIDCCCreateDynamicClientRegistrationRequest;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.testmodule.TestFailureException;
import net.openid.conformance.variant.ClientAuthType;
import net.openid.conformance.variant.ClientRegistration;
import net.openid.conformance.variant.ResponseType;
import net.openid.conformance.variant.VariantNotApplicable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

// Corresponds to https://www.heenan.me.uk/~joseph/oidcc_test_desc-phase1.html#OP_Registration_jwks_uri
@PublishTestModule(
	testName = "oidcc-registration-jwks-uri",
	displayName = "OIDCC: dynamic registration with JWKS URI",
	summary = "This test calls the dynamic registration endpoint with a jwks URI, and continues with authorization. This should result in a successful registration and authorization.",
	profile = "OIDCC"
)
@VariantNotApplicable(parameter = ClientAuthType.class, values = {"none", "client_secret_basic", "client_secret_post", "client_secret_jwt", "mtls"})
@VariantNotApplicable(parameter = ResponseType.class, values = {"id_token", "id_token token"})
@VariantNotApplicable(parameter = ClientRegistration.class, values = {"static_client"})
public class OIDCCRegistrationJwksUri extends AbstractOIDCCServerTest {

	@Override
	protected void createDynamicClientRegistrationRequest() {
		callAndStopOnFailure(CreateJwksUri.class);
		call(new OIDCCCreateDynamicClientRegistrationRequest(responseType)
				.replace(GenerateRS256ClientJWKs.class,
						condition(GenerateRS256ClientJWKsWithKeyID.class))
				.replace(AddPublicJwksToDynamicRegistrationRequest.class,
						condition(AddJwksUriToDynamicRegistrationRequest.class)));

		expose("client_name", env.getString("dynamic_registration_request", "client_name"));
	}

	@Override
	public Object handleHttp(String path, HttpServletRequest req, HttpServletResponse res, HttpSession session, JsonObject requestParts) {
		if (path.equals("client1_jwks")) {
			return handleJwksRequest();
		} else {
			return super.handleHttp(path, req, res, session, requestParts);
		}
	}

	private Object handleJwksRequest() {
		JsonObject clientPublicJwks = env.getObject("client_public_jwks");
		if (clientPublicJwks == null) {
			throw new TestFailureException(getId(), "jwks endpoint called before key exists - please wait for test to initialise before calling client jwks endpoint");
		}
		return ResponseEntity.ok()
				.contentType(MediaType.APPLICATION_JSON)
				.body(clientPublicJwks);
	}
}
