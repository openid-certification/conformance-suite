package net.openid.conformance.openid.federation.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.as.LoadServerJWKs;
import net.openid.conformance.condition.as.SignIdToken;
import net.openid.conformance.condition.client.ValidateServerJWKs;
import net.openid.conformance.openid.federation.CallEntityStatementEndpointAndReturnFullResponse;
import net.openid.conformance.openid.federation.EntityUtils;
import net.openid.conformance.openid.federation.ExtractJWTFromFederationEndpointResponse;
import net.openid.conformance.openid.federation.ValidateFederationUrl;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.testmodule.TestFailureException;
import org.apache.commons.lang3.RandomStringUtils;
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
		"federation.authority_hints",
		"federation.immediate_subordinates",
		"server.jwks",
	}
)
@SuppressWarnings("unused")
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
		exposeEnvString("entity_identifier");

		env.putString("entity_configuration_url", baseUrl + "/.well-known/openid-federation");
		exposeEnvString("entity_configuration_url");

		env.putString("federation_fetch_endpoint", baseUrl + "/fetch");
		exposeEnvString("federation_fetch_endpoint");

		env.putString("federation_list_endpoint", baseUrl + "/list");
		exposeEnvString("federation_list_endpoint");

		setStatus(Status.CONFIGURED);
		fireSetupDone();
	}

	@Override
	public void additionalConfiguration() {
	}

	@Override
	public void start() {
		setStatus(Status.WAITING);
		//fireTestFinished();
	}

	@Override
	public Object handleHttp(String path, HttpServletRequest req, HttpServletResponse res, HttpSession session, JsonObject requestParts) {
		String requestId = "incoming_request_" + RandomStringUtils.randomAlphanumeric(37);
		env.putObject(requestId, requestParts);
		return switch (path) {
			case ".well-known/openid-federation" -> entityConfigurationResponse();
			case "jwks" -> serverJwksResponse();
			case "fetch" -> fetchResponse(requestId);
			case "list" -> listResponse(requestId);
			default ->
				throw new TestFailureException(getId(), "Got an HTTP request to '" + path + "' that wasn't expected");
		};
	}

	protected Object entityConfigurationResponse() {
		setStatus(Status.RUNNING);

		env.mapKey("id_token_claims", "server");
		callAndStopOnFailure(SignIdToken.class);
		env.unmapKey("id_token_claims");
		String entityConfiguration = env.getString("id_token");

		setStatus(Status.WAITING);

		return ResponseEntity
			.status(HttpStatus.OK)
			.contentType(EntityUtils.ENTITY_STATEMENT_JWT)
			.body(entityConfiguration);
	}

	protected Object serverJwksResponse() {
		setStatus(Status.RUNNING);

		JsonObject jwks = env.getObject("server_public_jwks");

		setStatus(Status.WAITING);

		return ResponseEntity
			.status(HttpStatus.OK)
			.contentType(MediaType.APPLICATION_JSON)
			.body(jwks);
	}

	protected Object fetchResponse(String requestId) {
		setStatus(Status.RUNNING);
		call(exec().startBlock("Fetch endpoint").mapKey("incoming_request", requestId));

		callAndContinueOnFailure(ValidateSubParameterForFetchEndpoint.class,  Condition.ConditionResult.FAILURE);

		String error = env.getString("federation_fetch_endpoint_error");
		String errorDescription = env.getString("federation_fetch_endpoint_error_description");
		Integer statusCode = env.getInteger("federation_fetch_endpoint_status_code");

		ResponseEntity<Object> response = null;
		if (error!= null) {
			JsonObject errorObject = new JsonObject();
			errorObject.addProperty("error", error);
			errorObject.addProperty("error_description", errorDescription);
			env.removeNativeValue("federation_fetch_endpoint_error");
			env.removeNativeValue("federation_fetch_endpoint_error_description");
			env.removeNativeValue("federation_fetch_endpoint_status_code");
			response = ResponseEntity
				.status(HttpStatus.valueOf(statusCode))
				.contentType(MediaType.APPLICATION_JSON)
				.body(errorObject);
		} else {
			env.putString("federation_endpoint_url", EntityUtils.appendWellKnown(env.getString("fetch_endpoint_parameter_sub")));
			callAndStopOnFailure(ValidateFederationUrl.class, Condition.ConditionResult.FAILURE, "OIDFED-1.2");
			callAndStopOnFailure(CallEntityStatementEndpointAndReturnFullResponse.class, Condition.ConditionResult.FAILURE, "OIDFED-9");
			validateEntityStatementResponse();
			callAndStopOnFailure(ExtractJWTFromFederationEndpointResponse.class, "OIDFED-9");
			validateEntityStatement();
			env.removeNativeValue("federation_endpoint_url");

			JsonObject claims = env.getElementFromObject("federation_response_jwt", "claims").getAsJsonObject();
			claims.addProperty("iss", env.getString("base_url"));
			env.putObject("federation_fetch_response", claims);
			env.mapKey("id_token_claims", "federation_fetch_response");
			callAndStopOnFailure(SignIdToken.class);
			env.unmapKey("id_token_claims");
			String federationFetchResponse = env.getString("id_token");
			response = ResponseEntity
				.status(200)
				.contentType(EntityUtils.ENTITY_STATEMENT_JWT)
				.body(federationFetchResponse);
		}

		call(exec().unmapKey("incoming_request").endBlock());
		setStatus(Status.WAITING);

		return response;
	}

	protected Object listResponse(String requestId) {
		setStatus(Status.RUNNING);
		call(exec().startBlock("List endpoint").mapKey("incoming_request", requestId));

		JsonArray immediateSubordinates = new JsonArray();
		JsonElement immediateSubordinatesElement = env.getElementFromObject("config", "federation.immediate_subordinates");
		if (immediateSubordinatesElement != null) {
			immediateSubordinates = immediateSubordinatesElement.getAsJsonArray();
		}

		call(exec().unmapKey("incoming_request").endBlock());
		setStatus(Status.WAITING);

		return new ResponseEntity<Object>(immediateSubordinates, HttpStatus.OK);
	}
}
