package net.openid.conformance.openid.federation.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.as.AddCodeToAuthorizationEndpointResponseParams;
import net.openid.conformance.condition.as.CreateAuthorizationCode;
import net.openid.conformance.condition.as.CreateAuthorizationEndpointResponseParams;
import net.openid.conformance.condition.as.CreateEffectiveAuthorizationRequestParameters;
import net.openid.conformance.condition.as.EnsureAuthorizationHttpRequestContainsOpenIDScope;
import net.openid.conformance.condition.as.EnsureAuthorizationRequestContainsPkceCodeChallenge;
import net.openid.conformance.condition.as.EnsureNumericRequestObjectClaimsAreNotNull;
import net.openid.conformance.condition.as.EnsureOptionalAuthorizationRequestParametersMatchRequestObject;
import net.openid.conformance.condition.as.EnsureRequestObjectDoesNotContainRequestOrRequestUri;
import net.openid.conformance.condition.as.EnsureRequestObjectDoesNotContainSubWithClientId;
import net.openid.conformance.condition.as.EnsureRequiredAuthorizationRequestParametersMatchRequestObject;
import net.openid.conformance.condition.as.ExtractNonceFromAuthorizationRequest;
import net.openid.conformance.condition.as.ExtractRequestObject;
import net.openid.conformance.condition.as.ExtractRequestedScopes;
import net.openid.conformance.condition.as.LoadServerJWKs;
import net.openid.conformance.condition.as.OIDCCGetStaticClientConfigurationForRPTests;
import net.openid.conformance.condition.as.OIDCCValidateRequestObjectExp;
import net.openid.conformance.condition.as.SendAuthorizationResponseWithResponseModeQuery;
import net.openid.conformance.condition.as.SignIdToken;
import net.openid.conformance.condition.as.ValidateEncryptedRequestObjectHasKid;
import net.openid.conformance.condition.as.ValidateRequestObjectAud;
import net.openid.conformance.condition.as.ValidateRequestObjectIat;
import net.openid.conformance.condition.as.ValidateRequestObjectIss;
import net.openid.conformance.condition.as.ValidateRequestObjectMaxAge;
import net.openid.conformance.condition.as.ValidateRequestObjectSignature;
import net.openid.conformance.condition.client.ExtractJWKsFromStaticClientConfiguration;
import net.openid.conformance.condition.client.ValidateServerJWKs;
import net.openid.conformance.openid.federation.AddFederationEntityMetadataToEntityConfiguration;
import net.openid.conformance.openid.federation.AddOpenIDProviderMetadataToEntityConfiguration;
import net.openid.conformance.openid.federation.CallEntityStatementEndpointAndReturnFullResponse;
import net.openid.conformance.openid.federation.EntityUtils;
import net.openid.conformance.openid.federation.ExtractJWTFromFederationEndpointResponse;
import net.openid.conformance.openid.federation.ValidateFederationUrl;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.testmodule.TestFailureException;
import net.openid.conformance.testmodule.UserFacing;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.view.RedirectView;

@PublishTestModule(
	testName = "openid-federation-client-happy-path",
	displayName = "openid-federation-client-happy-path",
	summary = "openid-federation-client-happy-path",
	profile = "OIDFED",
	configurationFields = {
		"federation.authority_hints",
		"federation.immediate_subordinates",
		"client.client_id",
		"client.jwks",
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
		callAndStopOnFailure(AddFederationEntityMetadataToEntityConfiguration.class);
		callAndStopOnFailure(AddOpenIDProviderMetadataToEntityConfiguration.class);

		callAndStopOnFailure(OIDCCGetStaticClientConfigurationForRPTests.class);
		callAndStopOnFailure(ExtractJWKsFromStaticClientConfiguration.class);

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
			case "authorize" -> authorizeResponse(requestId);
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

			env.putObject("original_server_jwks", env.getObject("server_jwks")); // Save the jkws, as we'll have to restore it later
			env.putString("federation_endpoint_url", EntityUtils.appendWellKnown(env.getString("fetch_endpoint_parameter_sub")));
			callAndStopOnFailure(ValidateFederationUrl.class, Condition.ConditionResult.FAILURE, "OIDFED-1.2");
			callAndStopOnFailure(CallEntityStatementEndpointAndReturnFullResponse.class, Condition.ConditionResult.FAILURE, "OIDFED-9");
			validateEntityStatementResponse();
			callAndStopOnFailure(ExtractJWTFromFederationEndpointResponse.class, "OIDFED-9");
			validateEntityStatement();
			env.removeNativeValue("federation_endpoint_url");
			env.putObject("server_jwks", env.getObject("original_server_jwks")); // Restore
			env.removeObject("original_server_jwks");

			JsonObject claims = env.getElementFromObject("federation_response_jwt", "claims").getAsJsonObject();
			claims.remove("authority_hints");
			claims.remove("trust_mark_issuers");
			claims.remove("trust_mark_owners");
			claims.addProperty("iss", env.getString("base_url"));
			claims.addProperty("source_endpoint", env.getString("federation_fetch_endpoint"));
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

	@UserFacing
	protected Object authorizeResponse(String requestId) {
		setStatus(Status.RUNNING);
		call(exec().startBlock("Authorization endpoint").mapKey("incoming_request", requestId));
		env.mapKey("authorization_endpoint_http_request", requestId);
		env.putString("server", "issuer", env.getString("entity_identifier"));

		setAuthorizationEndpointRequestParamsForHttpMethod();
		callAndContinueOnFailure(UrlDecodeClientIdQueryParameter.class, Condition.ConditionResult.FAILURE);
		extractAuthorizationEndpointRequestParameters();

		Environment _env = env;
		String clientId = env.getString("authorization_request_object", "claims.client_id");
		env.putString("federation_endpoint_url", EntityUtils.appendWellKnown(clientId));
		callAndStopOnFailure(ValidateFederationUrl.class, Condition.ConditionResult.FAILURE, "OIDFED-1.2");
		callAndStopOnFailure(CallEntityStatementEndpointAndReturnFullResponse.class, Condition.ConditionResult.FAILURE, "OIDFED-9");
		validateEntityStatementResponse();

		callAndStopOnFailure(CreateAuthorizationCode.class);
		callAndStopOnFailure(CreateAuthorizationEndpointResponseParams.class);
		callAndStopOnFailure(AddCodeToAuthorizationEndpointResponseParams.class, "OIDCC-3.3.2.5");
		callAndStopOnFailure(SendAuthorizationResponseWithResponseModeQuery.class, "OIDCC-3.3.2.5");
		exposeEnvString("authorization_endpoint_response_redirect");
		String redirectTo = env.getString("authorization_endpoint_response_redirect");
		Object viewToReturn = new RedirectView(redirectTo, false, false, false);

		env.unmapKey("authorization_endpoint_http_request");
		call(exec().unmapKey("incoming_request").endBlock());
		setStatus(Status.WAITING);

		return viewToReturn;
	}

	protected void setAuthorizationEndpointRequestParamsForHttpMethod() {
		String httpMethod = env.getString("authorization_endpoint_http_request", "method");
		JsonObject httpRequestObj = env.getObject("authorization_endpoint_http_request");
		if("POST".equals(httpMethod)) {
			env.putObject("authorization_endpoint_http_request_params", httpRequestObj.getAsJsonObject("body_form_params"));
		} else if("GET".equals(httpMethod)) {
			env.putObject("authorization_endpoint_http_request_params", httpRequestObj.getAsJsonObject("query_string_params"));
		} else {
			throw new TestFailureException(getId(), "Got unexpected HTTP method to authorization endpoint");
		}
	}

	protected void extractAuthorizationEndpointRequestParameters() {
		callAndStopOnFailure(ExtractRequestObject.class, "OIDCC-6.1");
		callAndStopOnFailure(EnsureAuthorizationHttpRequestContainsOpenIDScope.class, "OIDCC-6.1", "OIDCC-6.2");
		validateRequestObject();
		callAndStopOnFailure(EnsureRequiredAuthorizationRequestParametersMatchRequestObject.class, "OIDCC-6.1", "OIDCC-6.2");
		skipIfElementMissing("authorization_request_object", "jwe_header", Condition.ConditionResult.INFO, ValidateEncryptedRequestObjectHasKid.class, Condition.ConditionResult.FAILURE, "OIDCC-10.2", "OIDCC-10.2.1");
		callAndContinueOnFailure(EnsureOptionalAuthorizationRequestParametersMatchRequestObject.class, Condition.ConditionResult.WARNING, "OIDCC-6.1", "OIDCC-6.2");
		callAndStopOnFailure(CreateEffectiveAuthorizationRequestParameters.class, "OIDCC-6.1", "OIDCC-6.2");
		callAndStopOnFailure(ExtractRequestedScopes.class);
		extractNonceFromAuthorizationEndpointRequestParameters();
		skipIfElementMissing(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, CreateEffectiveAuthorizationRequestParameters.CODE_CHALLENGE, Condition.ConditionResult.INFO, EnsureAuthorizationRequestContainsPkceCodeChallenge.class, Condition.ConditionResult.FAILURE, "RFC7636-4.3");
	}

	protected void extractNonceFromAuthorizationEndpointRequestParameters() {
		String responseType = env.getString(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, CreateEffectiveAuthorizationRequestParameters.RESPONSE_TYPE);
		if (responseType != null && responseType.contains("id_token")) {
			callAndStopOnFailure(ExtractNonceFromAuthorizationRequest.class, Condition.ConditionResult.FAILURE, "OIDCC-3.1.2.1", "OIDCC-3.2.2.1");
		} else {
			callAndContinueOnFailure(ExtractNonceFromAuthorizationRequest.class, Condition.ConditionResult.INFO, "OIDCC-3.1.2.1");
		}
	}

	protected void validateRequestObject() {
		skipIfElementMissing("authorization_request_object", "claims.exp", Condition.ConditionResult.INFO,
			OIDCCValidateRequestObjectExp.class, Condition.ConditionResult.FAILURE, "RFC7519-4.1.4");
		callAndContinueOnFailure(ValidateRequestObjectIat.class, Condition.ConditionResult.WARNING, "OIDCC-6.1");
		callAndContinueOnFailure(EnsureNumericRequestObjectClaimsAreNotNull.class, Condition.ConditionResult.WARNING, "OIDCC-13.3");
		callAndContinueOnFailure(ValidateRequestObjectMaxAge.class, Condition.ConditionResult.FAILURE, "OIDCC-13.3");
		callAndContinueOnFailure(EnsureRequestObjectDoesNotContainRequestOrRequestUri.class, Condition.ConditionResult.WARNING, "OIDCC-6.1");
		callAndContinueOnFailure(EnsureRequestObjectDoesNotContainSubWithClientId.class, Condition.ConditionResult.WARNING, "JAR-10.8");

		String alg = env.getString("authorization_request_object", "header.alg");

		callAndContinueOnFailure(ValidateRequestObjectIss.class, Condition.ConditionResult.WARNING, "OIDCC-6.1");
		callAndContinueOnFailure(ValidateRequestObjectAud.class, Condition.ConditionResult.WARNING, "OIDCC-6.1");

		skipIfMissing(new String[]{"client_public_jwks"}, null, Condition.ConditionResult.FAILURE,
			ValidateRequestObjectSignature.class, Condition.ConditionResult.FAILURE, "OIDCC-6.1");
	}

	/*
	// From AbstractOIDCCClientTest
	@UserFacing
	protected Object handleAuthorizationEndpointRequest(String requestId) {

		call(exec().startBlock(getAuthorizationEndpointBlockText()).mapKey("authorization_endpoint_http_request", requestId));
		setAuthorizationEndpointRequestParamsForHttpMethod();

		extractAuthorizationEndpointRequestParameters();

		validateAuthorizationEndpointRequestParameters();

		skipIfElementMissing("authorization_request_object", "claims", Condition.ConditionResult.INFO,
			CheckForUnexpectedClaimsInRequestObject.class, Condition.ConditionResult.WARNING, "RFC6749-4.1.1", "OIDCC-3.1.2.1", "RFC7636-4.3", "OAuth2-RT-2.1", "RFC7519-4.1", "DPOP-10", "RFC8485-4.1", "RFC8707-2.1", "RFC9396-2");

		skipIfElementMissing("authorization_request_object", "claims.claims", Condition.ConditionResult.INFO,
			CheckForUnexpectedClaimsInClaimsParameter.class, Condition.ConditionResult.WARNING, "OIDCC-5.5");
		skipIfElementMissing("authorization_request_object", "claims.claims", Condition.ConditionResult.INFO,
			CheckForUnexpectedOpenIdClaims.class, Condition.ConditionResult.WARNING, "OIDCC-5.1", "OIDCC-5.5.1.1", "BrazilOB-5.2.2.3", "BrazilOB-5.2.2.4", "OBSP-3.4");
		skipIfElementMissing("authorization_request_object", "claims.claims", Condition.ConditionResult.INFO,
			CheckRequestObjectClaimsParameterValues.class, Condition.ConditionResult.FAILURE, "OIDCC-5.5");
		skipIfElementMissing("authorization_request_object", "claims.claims", Condition.ConditionResult.INFO,
			CheckRequestObjectClaimsParameterMemberValues.class, Condition.ConditionResult.FAILURE, "OIDCC-5.5.1");

		if(responseType.includesCode()) {
			createAuthorizationCode();
		}

		if(responseType.includesToken()) {
			generateAccessToken();
		}

		if(responseType.includesIdToken()) {
			createIdToken(false);
		}

		callAndStopOnFailure(CreateAuthorizationEndpointResponseParams.class);

		if(responseType.includesCode()) {
			callAndStopOnFailure(AddCodeToAuthorizationEndpointResponseParams.class, "OIDCC-3.3.2.5");
		}
		if(responseType.includesIdToken()) {
			callAndStopOnFailure(AddIdTokenToAuthorizationEndpointResponseParams.class, "OIDCC-3.3.2.5");
		}
		if(responseType.includesToken()) {
			callAndStopOnFailure(AddTokenToAuthorizationEndpointResponseParams.class, "OIDCC-3.3.2.5");
		}


		customizeAuthorizationEndpointResponseParams();

		Object viewToReturn = null;
		if(responseMode.isFormPost()) {

			viewToReturn = generateFormPostResponse();

		} else {

			redirectFromAuthorizationEndpoint();

			exposeEnvString("authorization_endpoint_response_redirect");

			String redirectTo = env.getString("authorization_endpoint_response_redirect");

			viewToReturn = new RedirectView(redirectTo, false, false, false);
		}

		env.putString("auth_time", Long.toString(Instant.now().getEpochSecond()));

		call(exec().unmapKey("authorization_endpoint_http_request").endBlock());
		return viewToReturn;
	}
	*/
}
