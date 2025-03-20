package net.openid.conformance.openid.federation;

import com.google.gson.JsonObject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.as.SignIdToken;
import net.openid.conformance.condition.client.ExtractJWKsFromStaticClientConfiguration;
import net.openid.conformance.condition.client.GetStaticClientConfiguration;
import net.openid.conformance.condition.client.SignRequestObject;
import net.openid.conformance.condition.client.ValidateClientJWKsPrivatePart;
import net.openid.conformance.openid.federation.client.GenerateEntityConfiguration;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.testmodule.TestFailureException;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.http.client.utils.URIBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.net.URISyntaxException;

@PublishTestModule(
		testName = "openid-federation-automatic-client-registration",
		displayName = "openid-federation-automatic-client-registration",
		summary = "The test acts as an RP wanting to perform automatic client registration with an OP",
		profile = "OIDFED",
		configurationFields = {
			"client.jwks",
			"federation.entity_identifier",
			"federation.trust_anchor_jwks"
		}
)
@SuppressWarnings("unused")
public class OpenIDFederationAutomaticClientRegistrationTest extends AbstractOpenIDFederationTest {

	@Override
	public void additionalConfiguration() {
		eventLog.startBlock("Additional configuration");

		String baseUrl = env.getString("base_url");

		JsonObject clientConfig = env.getElementFromObject("config", "client").getAsJsonObject();
		clientConfig.addProperty("client_id", baseUrl);

		callAndStopOnFailure(GenerateEntityConfiguration.class);
		callAndStopOnFailure(AddFederationEntityMetadataToEntityConfiguration.class);
		callAndStopOnFailure(AddOpenIDRelyingPartyMetadataToEntityConfiguration.class);

		callAndStopOnFailure(GetStaticClientConfiguration.class);
		callAndStopOnFailure(ValidateClientJWKsPrivatePart.class, "RFC7517-1.1");
		callAndStopOnFailure(ExtractJWKsFromStaticClientConfiguration.class);

		env.putString("entity_identifier", baseUrl);
		exposeEnvString("entity_identifier");

		env.putString("entity_configuration_url", baseUrl + "/.well-known/openid-federation");
		exposeEnvString("entity_configuration_url");

		eventLog.endBlock();
	}

	@Override
	public void start() {
		setStatus(Status.RUNNING);

		callAndContinueOnFailure(CreateRequestObjectClaims.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(SignRequestObject.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(EncryptRequestObject.class, Condition.ConditionResult.FAILURE);

		final String endpointUri = env.getString("primary_entity_statement_jwt", "claims.metadata.openid_provider.authorization_endpoint");
		final JsonObject requestObjectClaims = env.getObject("request_object_claims");
		final String requestObject = env.getString("request_object");
		final String authorizationEndpointUrl;
		try {
			URIBuilder uriBuilder = new URIBuilder(endpointUri);
			uriBuilder.addParameter("client_id", OIDFJSON.getString(requestObjectClaims.get("client_id")));
			uriBuilder.addParameter("scope", OIDFJSON.getString(requestObjectClaims.get("scope")));
			uriBuilder.addParameter("response_type", OIDFJSON.getString(requestObjectClaims.get("response_type")));
			uriBuilder.addParameter("request", requestObject);
			authorizationEndpointUrl = uriBuilder.build().toString();
		} catch (URISyntaxException e) {
			throw new TestFailureException(getId(), "Invalid authorization endpoint URI", e);
		}
		env.putString("redirect_to_authorization_endpoint", authorizationEndpointUrl);
		performRedirect(HttpMethod.POST.name());
	}

	@Override
	public Object handleHttp(String path, HttpServletRequest req, HttpServletResponse res, HttpSession session, JsonObject requestParts) {
		String requestId = "incoming_request_" + RandomStringUtils.randomAlphanumeric(37);
		env.putObject(requestId, requestParts);
		return switch (path) {
			case ".well-known/openid-federation" -> entityConfigurationResponse();
			case "jwks" -> clientJwksResponse();
			default -> super.handleHttp(path, req, res, session, requestParts);
		};
	}

	protected Object entityConfigurationResponse() {
		setStatus(Status.RUNNING);

		env.mapKey("id_token_claims", "server");
		env.mapKey("server_jwks", "client_jwks");
		callAndStopOnFailure(SignIdToken.class);
		env.unmapKey("server_jwks");
		env.unmapKey("id_token_claims");
		String entityConfiguration = env.getString("id_token");

		setStatus(Status.WAITING);

		return ResponseEntity
			.status(HttpStatus.OK)
			.contentType(EntityUtils.ENTITY_STATEMENT_JWT)
			.body(entityConfiguration);
	}

	protected Object clientJwksResponse() {
		setStatus(Status.RUNNING);

		JsonObject jwks = env.getObject("client_public_jwks");

		setStatus(Status.WAITING);

		return ResponseEntity
			.status(HttpStatus.OK)
			.contentType(MediaType.APPLICATION_JSON)
			.body(jwks);
	}
}
