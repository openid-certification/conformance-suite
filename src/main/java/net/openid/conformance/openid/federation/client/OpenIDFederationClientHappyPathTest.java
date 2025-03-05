package net.openid.conformance.openid.federation.client;

import com.google.gson.JsonObject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import net.openid.conformance.condition.as.LoadServerJWKs;
import net.openid.conformance.condition.as.SignIdToken;
import net.openid.conformance.condition.client.ValidateServerJWKs;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.testmodule.TestFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

@PublishTestModule(
	testName = "openid-federation-client-happy-path",
	displayName = "openid-federation-client-happy-path",
	summary = "openid-federation-client-happy-path",
	profile = "OIDFED",
	configurationFields = {
		"federation.entity_identifier",
		"server.jwks",
	}
)
public class OpenIDFederationClientHappyPathTest extends AbstractOpenIDFederationClientTest {

	@Override
	public void configure(JsonObject config, String baseUrl, String externalUrlOverride, String baseMtlsUrl) {
		env.putString("base_url", baseUrl);
		env.putString("base_mtls_url", baseMtlsUrl);
		env.putObject("config", config);

		callAndStopOnFailure(LoadServerJWKs.class);
		callAndStopOnFailure(ValidateServerJWKs.class, "RFC7517-1.1");
		callAndStopOnFailure(GenerateEntityConfiguration.class);
		callAndStopOnFailure(AddMetadataToEntityConfiguration.class);

		env.putString("entity_identifier", baseUrl);
		env.putString("entity_configuration_url", baseUrl + "/.well-known/openid-federation");
		exposeEnvString("entity_identifier");
		exposeEnvString("entity_configuration_url");

		setStatus(Status.CONFIGURED);
		fireSetupDone();
	}

	@Override
	public void start() {
		setStatus(Status.WAITING);
		//fireTestFinished();
	}

	@Override
	public Object handleHttp(String path, HttpServletRequest req, HttpServletResponse res, HttpSession session, JsonObject requestParts) {
		if (path.startsWith(".well-known/openid-federation")) {
			return entityConfigurationResponse();
		}
		if (path.startsWith("jwks")) {
			return serverJwksResponse();
		}
		throw new TestFailureException(getId(), "Got an HTTP request to '"+path+"' that wasn't expected");
	}

	protected Object entityConfigurationResponse() {
		setStatus(Status.RUNNING);

		env.mapKey("id_token_claims", "server");
		callAndStopOnFailure(SignIdToken.class);
		env.unmapKey("id_token_claims");
		String entityConfiguration = env.getString("id_token");

		setStatus(Status.WAITING);

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(new MediaType("application", "entity-statement+jwt"));
		return new ResponseEntity<Object>(entityConfiguration, headers, HttpStatus.OK);
	}

	protected Object serverJwksResponse() {
		setStatus(Status.RUNNING);

		JsonObject jwks = env.getObject("server_public_jwks");

		setStatus(Status.WAITING);

		return new ResponseEntity<Object>(jwks, HttpStatus.OK);
	}
}
