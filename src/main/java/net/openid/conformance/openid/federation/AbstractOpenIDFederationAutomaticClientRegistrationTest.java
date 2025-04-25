package net.openid.conformance.openid.federation;

import com.google.gson.JsonObject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CheckErrorDescriptionFromAuthorizationEndpointResponseErrorContainsCRLFTAB;
import net.openid.conformance.condition.client.CheckForUnexpectedParametersInErrorResponseFromAuthorizationEndpoint;
import net.openid.conformance.condition.client.CheckStateInAuthorizationResponse;
import net.openid.conformance.condition.client.EnsureContentTypeJson;
import net.openid.conformance.condition.client.EnsureErrorFromAuthorizationEndpointResponse;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs200;
import net.openid.conformance.condition.client.ExtractJWKsFromStaticClientConfiguration;
import net.openid.conformance.condition.client.ExtractRequestUriFromPARResponse;
import net.openid.conformance.condition.client.GetStaticClientConfiguration;
import net.openid.conformance.condition.client.RejectAuthCodeInAuthorizationEndpointResponse;
import net.openid.conformance.condition.client.SignRequestObject;
import net.openid.conformance.condition.client.ValidateClientJWKsPrivatePart;
import net.openid.conformance.condition.client.ValidateErrorDescriptionFromAuthorizationEndpointResponseError;
import net.openid.conformance.condition.client.ValidateErrorUriFromAuthorizationEndpointResponseError;
import net.openid.conformance.condition.client.ValidateIssIfPresentInAuthorizationResponse;
import net.openid.conformance.openid.federation.client.GenerateEntityConfiguration;
import net.openid.conformance.openid.federation.client.SignEntityStatementWithClientKeys;
import net.openid.conformance.testmodule.TestFailureException;
import net.openid.conformance.variant.FAPIAuthRequestMethod;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.http.client.utils.URIBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.net.URISyntaxException;

@SuppressWarnings("unused")
public abstract class AbstractOpenIDFederationAutomaticClientRegistrationTest extends AbstractOpenIDFederationTest {

	protected boolean includeTrustChainInAuthorizationRequest = false;

	protected abstract FAPIAuthRequestMethod getRequestMethod();

	protected abstract HttpMethod getHttpMethodForAuthorizeRequest();

	protected abstract void verifyTestConditions();

	protected abstract void redirect(HttpMethod method);

	@Override
	public void additionalConfiguration() {
		eventLog.startBlock("Additional configuration");

		String baseUrl = env.getString("base_url");

		JsonObject clientConfig = env.getElementFromObject("config", "client").getAsJsonObject();
		clientConfig.addProperty("client_id", baseUrl);

		callAndStopOnFailure(GetStaticClientConfiguration.class);
		callAndStopOnFailure(ValidateClientJWKsPrivatePart.class, "RFC7517-1.1");
		callAndStopOnFailure(ExtractJWKsFromStaticClientConfiguration.class);

		env.mapKey("server_public_jwks", "client_public_jwks");
		callAndStopOnFailure(GenerateEntityConfiguration.class);
		callAndStopOnFailure(AddFederationEntityMetadataToEntityConfiguration.class);
		callAndStopOnFailure(AddOpenIDRelyingPartyMetadataToEntityConfiguration.class);
		env.unmapKey("client_public_jwks");

		env.putString("entity_identifier", baseUrl);
		exposeEnvString("entity_identifier");

		env.putString("entity_configuration_url", baseUrl + "/.well-known/openid-federation");
		exposeEnvString("entity_configuration_url");

		verifyTestConditions();

		eventLog.endBlock();
	}

	@Override
	public void start() {
		setStatus(Status.RUNNING);
		makeAuthorizationRequest();
	}

	protected void makeAuthorizationRequest() {
		buildRequestObject();
		signRequestObject();
		encryptRequestObject();

		String endpointUri = env.getString("primary_entity_statement_jwt", "claims.metadata.openid_provider.authorization_endpoint");
		URIBuilder uriBuilder = null;
		try {
			uriBuilder = new URIBuilder(endpointUri);
		} catch (URISyntaxException e) {
			throw new TestFailureException(getId(), "Invalid authorization endpoint URI", e);
		}

		String authorizationEndpointUrl;

		if (FAPIAuthRequestMethod.PUSHED.equals(getRequestMethod())) {
			callParEndpoint();
			extractRequestUri();
			uriBuilder.addParameter("request_uri", env.getString("request_uri"));

		} else {
			createQueryParameters();
			uriBuilder.addParameter("client_id", env.getString("query_parameters", "client_id"));
			uriBuilder.addParameter("scope", env.getString("query_parameters", "scope"));
			uriBuilder.addParameter("response_type", env.getString("query_parameters", "response_type"));
			uriBuilder.addParameter("request", env.getString("query_parameters", "request"));
		}

		try {
			authorizationEndpointUrl = uriBuilder.build().toString();
		} catch (URISyntaxException e) {
			throw new TestFailureException(getId(), "Invalid authorization endpoint URI", e);
		}

		env.putString("redirect_uri", env.getString("request_object_claims", "redirect_uri"));
		env.putString("redirect_to_authorization_endpoint", authorizationEndpointUrl);

		HttpMethod httpMethod = getHttpMethodForAuthorizeRequest();
		redirect(httpMethod);
	}

	protected void buildRequestObject() {
		callAndContinueOnFailure(CreateRequestObjectClaims.class, Condition.ConditionResult.FAILURE);
		if (includeTrustChainInAuthorizationRequest) {
			callAndContinueOnFailure(AddTrustChainParameterToRequestObject.class, Condition.ConditionResult.FAILURE);
		}
	}

	protected void signRequestObject() {
		callAndContinueOnFailure(SignRequestObject.class, Condition.ConditionResult.FAILURE);
	}

	protected void encryptRequestObject() {
	}

	protected void callParEndpoint() {
		callAndContinueOnFailure(CallPAREndpointWithPostAndReturnFullResponse.class, Condition.ConditionResult.FAILURE);
		env.mapKey("endpoint_response", "authorization_endpoint_response");
		callAndContinueOnFailure(EnsureHttpStatusCodeIs200.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(EnsureContentTypeJson.class, Condition.ConditionResult.FAILURE);
		env.unmapKey("endpoint_response");
	}

	protected void extractRequestUri() {
		env.mapKey("pushed_authorization_endpoint_response", "authorization_endpoint_response");
		callAndContinueOnFailure(ExtractRequestUriFromPARResponse.class, Condition.ConditionResult.FAILURE);
		env.unmapKey("pushed_authorization_endpoint_response");
	}

	protected void createQueryParameters() {
		callAndStopOnFailure(CreateQueryParametersForAuthorizationRequest.class, Condition.ConditionResult.FAILURE);
	}

	@Override
	public Object handleHttp(String path, HttpServletRequest req, HttpServletResponse res, HttpSession session, JsonObject requestParts) {
		String requestId = "incoming_request_" + RandomStringUtils.secure().nextAlphanumeric(37);
		env.putObject(requestId, requestParts);
		return switch (path) {
			case ".well-known/openid-federation" -> entityConfigurationResponse();
			case "jwks" -> clientJwksResponse();
			default -> super.handleHttp(path, req, res, session, requestParts);
		};
	}

	protected Object entityConfigurationResponse() {
		if (opToRpMode()) {
			env.mapKey("entity_configuration_claims", "server");
			env.mapKey("entity_configuration_claims_jwks", "client_jwks");
			return NonBlocking.entityConfigurationResponse(env, getId());
		}

		setStatus(Status.RUNNING);

		env.mapKey("entity_configuration_claims", "server");
		callAndStopOnFailure(SignEntityStatementWithClientKeys.class);
		env.unmapKey("entity_configuration_claims");
		String entityConfiguration = env.getString("signed_entity_statement");

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

	/**
	 * Do generic checks on an error response from the authorization endpoint
	 *
	 * Generally called from onAuthorizationCallbackResponse. The caller stills needs to check for the exact specific
	 * error code their test scenario expects.
	 */
	protected void performGenericAuthorizationEndpointErrorResponseValidation() {
		callAndContinueOnFailure(CheckStateInAuthorizationResponse.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(ValidateIssIfPresentInAuthorizationResponse.class, Condition.ConditionResult.FAILURE, "OAuth2-iss-2");
		callAndContinueOnFailure(EnsureErrorFromAuthorizationEndpointResponse.class, Condition.ConditionResult.FAILURE, "OIDCC-3.1.2.6");
		callAndContinueOnFailure(RejectAuthCodeInAuthorizationEndpointResponse.class, Condition.ConditionResult.FAILURE, "OIDCC-3.1.2.6");
		callAndContinueOnFailure(CheckForUnexpectedParametersInErrorResponseFromAuthorizationEndpoint.class, Condition.ConditionResult.WARNING, "OIDCC-3.1.2.6");
		callAndContinueOnFailure(CheckErrorDescriptionFromAuthorizationEndpointResponseErrorContainsCRLFTAB.class, Condition.ConditionResult.WARNING, "RFC6749-4.1.2.1");
		callAndContinueOnFailure(ValidateErrorDescriptionFromAuthorizationEndpointResponseError.class, Condition.ConditionResult.FAILURE,"RFC6749-4.1.2.1");
		callAndContinueOnFailure(ValidateErrorUriFromAuthorizationEndpointResponseError.class, Condition.ConditionResult.FAILURE,"RFC6749-4.1.2.1");
	}

}
