package net.openid.conformance.poppy.company;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.client.AddCodeVerifierToTokenEndpointRequest;
import net.openid.conformance.condition.client.AddStateToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.BuildPlainRedirectToAuthorizationEndpoint;
import net.openid.conformance.condition.client.CallProtectedResource;
import net.openid.conformance.condition.client.CallTokenEndpointAndReturnFullResponse;
import net.openid.conformance.condition.client.CheckForAccessTokenValue;
import net.openid.conformance.condition.client.CheckForRefreshTokenValue;
import net.openid.conformance.condition.client.CheckForUnexpectedParametersInServerMetadata;
import net.openid.conformance.condition.client.CheckIfAuthorizationEndpointError;
import net.openid.conformance.condition.client.CheckIfTokenEndpointResponseError;
import net.openid.conformance.condition.client.CheckStateInAuthorizationResponse;
import net.openid.conformance.condition.client.CheckTokenEndpointHttpStatus200;
import net.openid.conformance.condition.client.CheckTokenEndpointReturnedJsonContentType;
import net.openid.conformance.condition.client.CreateAuthorizationEndpointRequestFromClientInformation;
import net.openid.conformance.condition.client.CreateRandomStateValue;
import net.openid.conformance.condition.client.CreateTokenEndpointRequestForAuthorizationCodeGrant;
import net.openid.conformance.condition.client.EnsureResourceResponseReturnedJsonContentType;
import net.openid.conformance.condition.client.ExtractAccessTokenFromTokenResponse;
import net.openid.conformance.condition.client.ExtractAuthorizationCodeFromAuthorizationResponse;
import net.openid.conformance.condition.client.ExtractExpiresInFromTokenEndpointResponse;
import net.openid.conformance.condition.client.ExtractGrantedScopeFromTokenEndpointResponse;
import net.openid.conformance.condition.client.ExtractJWKsFromStaticClientConfiguration;
import net.openid.conformance.condition.client.RequireIssInAuthorizationResponse;
import net.openid.conformance.condition.client.SetAuthorizationEndpointRequestResponseTypeToCode;
import net.openid.conformance.condition.client.SetProtectedResourceUrlToSingleResourceEndpoint;
import net.openid.conformance.condition.client.ValidateExpiresIn;
import net.openid.conformance.condition.common.CheckServerConfiguration;
import net.openid.conformance.poppy.condition.AddPoppyHeadersToResourceRequest;
import net.openid.conformance.poppy.condition.CheckForUnexpectedFieldsInPoppyJson;
import net.openid.conformance.poppy.condition.CheckForUnexpectedFieldsInPoppyProtectedResourceMetadata;
import net.openid.conformance.poppy.condition.CreatePoppyClientMetadataDocument;
import net.openid.conformance.poppy.condition.CreatePoppyDelegation;
import net.openid.conformance.poppy.condition.CreatePoppySession;
import net.openid.conformance.poppy.condition.CreatePoppySessionJwt;
import net.openid.conformance.poppy.condition.EnsurePoppyAuthNotDisabled;
import net.openid.conformance.poppy.condition.EnsurePoppyGrantedScopeNotWiderThanRequested;
import net.openid.conformance.poppy.condition.EnsurePoppyResourceResponseStatusIs2xx;
import net.openid.conformance.poppy.condition.EnsurePoppyTokenTypeIsBearer;
import net.openid.conformance.poppy.condition.ExtractPoppyCompanyDomainFromConfig;
import net.openid.conformance.poppy.condition.FetchPoppyAuthorizationServerMetadata;
import net.openid.conformance.poppy.condition.FetchPoppyJson;
import net.openid.conformance.poppy.condition.FetchPoppyProtectedResourceMetadata;
import net.openid.conformance.poppy.condition.GeneratePoppyDelegatedKey;
import net.openid.conformance.poppy.condition.SelectPoppyIssuerFromProtectedResourceMetadata;
import net.openid.conformance.poppy.condition.ValidatePoppyAuthorizationServerMetadata;
import net.openid.conformance.poppy.condition.ValidatePoppyJsonCoreFields;
import net.openid.conformance.sequence.client.CreateJWTClientAuthenticationAssertionWithIssAudAndAddToTokenEndpointRequest;
import net.openid.conformance.sequence.client.SetupPkceAndAddToAuthorizationRequest;
import net.openid.conformance.testmodule.AbstractRedirectServerTestModule;
import net.openid.conformance.testmodule.TestFailureException;
import net.openid.conformance.variant.ConfigurationFields;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.Locale;

/**
 * The suite acting as a Poppy 0.4 Personal Agent against a Company under test, using Direct Sign-In
 * (authorization code + PKCE) and then one protected resource call with a Poppy-Session header.
 */
@ConfigurationFields({"poppy.company_domain", "client.jwks", "client.scope", "resource.resourceUrl"})
public abstract class AbstractPoppyCompanyTestModule extends AbstractRedirectServerTestModule {

	@Override
	public void configure(JsonObject config, String baseUrl, String externalUrlOverride, String baseMtlsUrl) {
		env.putObject("config", config);
		env.putString("base_url", baseUrl);

		env.putObject("client", copyConfigSection(config, "client", "Client"));
		env.putObject("resource", copyConfigSection(config, "resource", "Resource"));

		callAndStopOnFailure(CreatePoppyClientMetadataDocument.class);
		callAndStopOnFailure(ExtractJWKsFromStaticClientConfiguration.class);

		exposeEnvString("client_id");

		setStatus(Status.CONFIGURED);
		fireSetupDone();
	}

	private JsonObject copyConfigSection(JsonObject config, String key, String uiSection) {
		JsonElement section = config.get(key);
		if (section == null || !section.isJsonObject()) {
			throw new TestFailureException(getId(), "The '" + uiSection + "' section is missing from the test configuration");
		}
		return section.getAsJsonObject().deepCopy();
	}

	@Override
	public Object handleHttp(String path, HttpServletRequest req, HttpServletResponse res, HttpSession session, JsonObject requestParts) {
		if ("agent.json".equals(path)) {
			return new ResponseEntity<Object>(env.getObject("poppy_client_metadata_document"), jsonHeaders(), HttpStatus.OK);
		} else if ("jwks.json".equals(path)) {
			return new ResponseEntity<Object>(env.getObject("client_public_jwks"), jsonHeaders(), HttpStatus.OK);
		}
		return super.handleHttp(path, req, res, session, requestParts);
	}

	private HttpHeaders jsonHeaders() {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		return headers;
	}

	@Override
	public void start() {
		setStatus(Status.RUNNING);

		eventLog.startBlock("Discovery");
		callAndStopOnFailure(ExtractPoppyCompanyDomainFromConfig.class);
		callAndStopOnFailure(FetchPoppyJson.class);
		callAndStopOnFailure(ValidatePoppyJsonCoreFields.class);
		callAndContinueOnFailure(CheckForUnexpectedFieldsInPoppyJson.class, ConditionResult.WARNING);
		callAndStopOnFailure(EnsurePoppyAuthNotDisabled.class);
		callAndStopOnFailure(FetchPoppyProtectedResourceMetadata.class);
		callAndContinueOnFailure(CheckForUnexpectedFieldsInPoppyProtectedResourceMetadata.class, ConditionResult.WARNING);
		callAndStopOnFailure(SelectPoppyIssuerFromProtectedResourceMetadata.class);
		callAndStopOnFailure(FetchPoppyAuthorizationServerMetadata.class);
		callAndStopOnFailure(ValidatePoppyAuthorizationServerMetadata.class);
		callAndContinueOnFailure(CheckForUnexpectedParametersInServerMetadata.class, ConditionResult.WARNING);
		callAndStopOnFailure(CheckServerConfiguration.class);
		eventLog.endBlock();

		eventLog.startBlock("Authorization request");
		callAndStopOnFailure(CreateRandomStateValue.class);
		callAndStopOnFailure(CreateAuthorizationEndpointRequestFromClientInformation.class);
		callAndStopOnFailure(AddStateToAuthorizationEndpointRequest.class);
		callAndStopOnFailure(SetAuthorizationEndpointRequestResponseTypeToCode.class);
		call(sequence(SetupPkceAndAddToAuthorizationRequest.class));
		callAndStopOnFailure(BuildPlainRedirectToAuthorizationEndpoint.class);
		performRedirect();
		eventLog.endBlock();
	}

	@Override
	protected void processCallback() {
		env.mapKey("authorization_endpoint_response", "callback_query_params");
		callAndStopOnFailure(CheckIfAuthorizationEndpointError.class);
		callAndStopOnFailure(CheckStateInAuthorizationResponse.class);
		callAndContinueOnFailure(RequireIssInAuthorizationResponse.class, ConditionResult.FAILURE, "OAuth2-iss-2");
		callAndStopOnFailure(ExtractAuthorizationCodeFromAuthorizationResponse.class);

		eventLog.startBlock("Token endpoint");
		callAndStopOnFailure(CreateTokenEndpointRequestForAuthorizationCodeGrant.class);
		callAndStopOnFailure(AddCodeVerifierToTokenEndpointRequest.class);
		mapClientAuthKeys("token_endpoint_request_form_parameters", "token_endpoint_request_headers");
		call(sequence(CreateJWTClientAuthenticationAssertionWithIssAudAndAddToTokenEndpointRequest.class));
		unmapClientAuthKeys();
		callAndStopOnFailure(CallTokenEndpointAndReturnFullResponse.class);
		callAndStopOnFailure(CheckIfTokenEndpointResponseError.class);
		callAndStopOnFailure(CheckTokenEndpointHttpStatus200.class);
		callAndContinueOnFailure(CheckTokenEndpointReturnedJsonContentType.class, ConditionResult.FAILURE);
		callAndStopOnFailure(CheckForAccessTokenValue.class);
		callAndContinueOnFailure(EnsurePoppyTokenTypeIsBearer.class, ConditionResult.FAILURE);
		callAndContinueOnFailure(CheckForRefreshTokenValue.class, ConditionResult.WARNING, "POPPY-4.7");
		callAndContinueOnFailure(ExtractExpiresInFromTokenEndpointResponse.class, ConditionResult.WARNING);
		skipIfMissing(new String[] {"expires_in"}, null, ConditionResult.INFO,
			ValidateExpiresIn.class, ConditionResult.WARNING);
		callAndStopOnFailure(ExtractGrantedScopeFromTokenEndpointResponse.class);
		callAndContinueOnFailure(EnsurePoppyGrantedScopeNotWiderThanRequested.class, ConditionResult.FAILURE);
		callAndStopOnFailure(ExtractAccessTokenFromTokenResponse.class);
		eventLog.endBlock();

		eventLog.startBlock("Protected resource with Session header");
		callAndStopOnFailure(SetProtectedResourceUrlToSingleResourceEndpoint.class);
		String configuredMethod = env.getString("resource", "resourceMethod");
		env.putString("poppy_resource_request_method",
			configuredMethod == null || configuredMethod.isEmpty() ? "GET" : configuredMethod.toUpperCase(Locale.ROOT));
		callAndStopOnFailure(GeneratePoppyDelegatedKey.class);
		callAndStopOnFailure(CreatePoppyDelegation.class);
		callAndStopOnFailure(CreatePoppySession.class);
		callAndStopOnFailure(CreatePoppySessionJwt.class);
		callAndStopOnFailure(AddPoppyHeadersToResourceRequest.class);
		callAndStopOnFailure(CallProtectedResource.class);
		callAndContinueOnFailure(EnsurePoppyResourceResponseStatusIs2xx.class, ConditionResult.FAILURE);
		callAndContinueOnFailure(EnsureResourceResponseReturnedJsonContentType.class, ConditionResult.WARNING);
		eventLog.endBlock();

		fireTestFinished();
	}
}
