package net.openid.conformance.poppy.agent;

import com.google.gson.JsonObject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.as.AddCodeToAuthorizationEndpointResponseParams;
import net.openid.conformance.condition.as.AddIssToAuthorizationEndpointResponseParams;
import net.openid.conformance.condition.as.CheckForClientAssertionJtiReuse;
import net.openid.conformance.condition.as.CheckPkceCodeVerifier;
import net.openid.conformance.condition.as.CreateAuthorizationCode;
import net.openid.conformance.condition.as.CreateAuthorizationEndpointResponseParams;
import net.openid.conformance.condition.as.CreateEffectiveAuthorizationRequestParameters;
import net.openid.conformance.condition.as.CreateRefreshToken;
import net.openid.conformance.condition.as.CreateTokenEndpointResponse;
import net.openid.conformance.condition.as.EnsureClientAssertionTypeIsJwt;
import net.openid.conformance.condition.as.EnsureResponseTypeIsCode;
import net.openid.conformance.condition.as.ExtractClientAssertion;
import net.openid.conformance.condition.as.GenerateBearerAccessToken;
import net.openid.conformance.condition.as.SendAuthorizationResponseWithResponseModeQuery;
import net.openid.conformance.condition.as.ValidateAuthorizationCode;
import net.openid.conformance.condition.as.ValidateClientAssertionAudClaimIsIssuerAsString;
import net.openid.conformance.condition.as.ValidateClientAssertionClaims;
import net.openid.conformance.condition.as.ValidateClientAssertionSignature;
import net.openid.conformance.condition.as.ValidateRedirectUriForTokenEndpointRequest;
import net.openid.conformance.condition.rs.EnsureIncomingRequestMethodIsGet;
import net.openid.conformance.condition.rs.ExtractBearerAccessTokenFromHeader;
import net.openid.conformance.poppy.PoppyConstants;
import net.openid.conformance.poppy.condition.CheckForUnexpectedFieldsInPoppyClientMetadataDocument;
import net.openid.conformance.poppy.condition.EnsurePoppyAuthorizationRequestContainsS256CodeChallenge;
import net.openid.conformance.poppy.condition.EnsurePoppyAuthorizationRequestContainsScope;
import net.openid.conformance.poppy.condition.EnsurePoppyClientAssertionAlgIsAdvertised;
import net.openid.conformance.poppy.condition.EnsurePoppyClientAssertionContainsIat;
import net.openid.conformance.poppy.condition.EnsurePoppyClientAssertionIsShortLived;
import net.openid.conformance.poppy.condition.EnsurePoppyClientJwksContainsOnlyPublicKeys;
import net.openid.conformance.poppy.condition.EnsurePoppyNoCredentialsInQuery;
import net.openid.conformance.poppy.condition.EnsurePoppyRedirectUriMatchesClientMetadata;
import net.openid.conformance.poppy.condition.EnsurePoppyRootWellKnownAlias;
import net.openid.conformance.poppy.condition.EnsurePoppyVersionIsSupported;
import net.openid.conformance.poppy.condition.ExtractPoppySessionHeaders;
import net.openid.conformance.poppy.condition.FetchPoppyClientJwks;
import net.openid.conformance.poppy.condition.FetchPoppyClientMetadataDocument;
import net.openid.conformance.poppy.condition.GeneratePoppyCompanyDocuments;
import net.openid.conformance.poppy.condition.ValidatePoppyClientMetadataDocument;
import net.openid.conformance.poppy.condition.ValidatePoppyDelegation;
import net.openid.conformance.poppy.condition.ValidatePoppySessionJwt;
import net.openid.conformance.testmodule.AbstractTestModule;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.OAuthUriUtil;
import net.openid.conformance.variant.ConfigurationFields;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.view.RedirectView;

import java.net.URI;

/**
 * The suite acting as a Poppy 0.4 Company, served at the host root, against a Personal Agent under
 * test: it publishes the Company documents, accepts one Direct Sign-In and verifies the Session
 * header on one protected resource call.
 */
@ConfigurationFields({"poppy.company_domain"})
public abstract class AbstractPoppyAgentTestModule extends AbstractTestModule {

	private static final String OPENAPI_DOCUMENT = """
		{
			"openapi": "3.1.0",
			"info": {"title": "Poppy conformance Company API", "version": "1.0.0"},
			"paths": {
				"/api/me": {
					"get": {
						"operationId": "getMe",
						"security": [{"oauth2": ["orders:read"]}],
						"responses": {"200": {"description": "The signed-in user"}}
					}
				}
			},
			"components": {
				"securitySchemes": {
					"oauth2": {
						"type": "oauth2",
						"flows": {
							"authorizationCode": {
								"authorizationUrl": "{ISSUER}/authorize",
								"tokenUrl": "{ISSUER}/token",
								"scopes": {"orders:read": "Read orders"}
							}
						}
					}
				}
			}
		}
		""";

	@Override
	public void configure(JsonObject config, String baseUrl, String externalUrlOverride, String baseMtlsUrl) {
		env.putObject("config", config);
		env.putString("base_url", baseUrl);

		callAndStopOnFailure(EnsurePoppyRootWellKnownAlias.class);
		callAndStopOnFailure(GeneratePoppyCompanyDocuments.class);

		exposeEnvString("issuer");

		setStatus(Status.CONFIGURED);
		fireSetupDone();
	}

	@Override
	public void start() {
		setStatus(Status.WAITING);
		String domain = env.getString("poppy_domain");
		eventLog.log(getName(), "Point the Personal Agent at Company domain '" + domain + "'");
	}

	private HttpHeaders jsonHeaders() {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		return headers;
	}

	private String storeIncomingRequest(JsonObject requestParts) {
		String requestId = "incoming_request_" + RandomStringUtils.secure().nextAlphanumeric(37);
		env.putObject(requestId, requestParts);
		return requestId;
	}

	private String authorizationServerMetadataPath() {
		return URI.create(OAuthUriUtil.generateWellKnownUrlForPath(env.getString("issuer"), "oauth-authorization-server")).getRawPath();
	}

	@Override
	public Object handleWellKnown(String path, HttpServletRequest req, HttpServletResponse res, HttpSession session, JsonObject requestParts) {
		String document;
		String title;
		if ("/.well-known/poppy.json".equals(path)) {
			document = "poppy_json";
			title = "Serve poppy.json";
		} else if ("/.well-known/oauth-protected-resource".equals(path)) {
			document = "poppy_protected_resource_metadata";
			title = "Serve protected resource metadata";
		} else if (authorizationServerMetadataPath().equals(path)) {
			document = "server";
			title = "Serve OAuth server metadata";
		} else {
			return super.handleWellKnown(path, req, res, session, requestParts);
		}

		setStatus(Status.RUNNING);
		String requestId = storeIncomingRequest(requestParts);
		call(exec().startBlock(title).mapKey("incoming_request", requestId));
		JsonObject body = env.getObject(document);
		call(exec().unmapKey("incoming_request").endBlock());
		setStatus(Status.WAITING);
		return new ResponseEntity<Object>(body, jsonHeaders(), HttpStatus.OK);
	}

	@Override
	public Object handleHttp(String path, HttpServletRequest req, HttpServletResponse res, HttpSession session, JsonObject requestParts) {
		switch (path) {
			case "authorize":
				return authorizationEndpoint(storeIncomingRequest(requestParts));
			case "token":
				return tokenEndpoint(storeIncomingRequest(requestParts));
			case "openapi.json":
				return openApiEndpoint(storeIncomingRequest(requestParts));
			case "api/me":
				return apiEndpoint(storeIncomingRequest(requestParts));
			default:
				return super.handleHttp(path, req, res, session, requestParts);
		}
	}

	protected Object openApiEndpoint(String requestId) {
		setStatus(Status.RUNNING);
		call(exec().startBlock("Serve OpenAPI document").mapKey("incoming_request", requestId));
		String document = OPENAPI_DOCUMENT.replace("{ISSUER}", env.getString("issuer"));
		call(exec().unmapKey("incoming_request").endBlock());
		setStatus(Status.WAITING);
		return new ResponseEntity<Object>(document, jsonHeaders(), HttpStatus.OK);
	}

	protected Object authorizationEndpoint(String requestId) {
		setStatus(Status.RUNNING);

		call(exec().startBlock("Authorization endpoint").mapKey("incoming_request", requestId));

		env.putObject("authorization_endpoint_http_request_params", env.getObject(requestId).getAsJsonObject("query_string_params"));
		callAndStopOnFailure(CreateEffectiveAuthorizationRequestParameters.class);
		env.putString("poppy_incoming_client_id", env.getString(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, CreateEffectiveAuthorizationRequestParameters.CLIENT_ID));

		callAndStopOnFailure(FetchPoppyClientMetadataDocument.class, "POPPY-4.1");
		callAndStopOnFailure(ValidatePoppyClientMetadataDocument.class, "POPPY-4.1");
		callAndContinueOnFailure(CheckForUnexpectedFieldsInPoppyClientMetadataDocument.class, ConditionResult.WARNING, "POPPY-4.1");
		callAndStopOnFailure(FetchPoppyClientJwks.class, "POPPY-4.1");
		callAndStopOnFailure(EnsurePoppyClientJwksContainsOnlyPublicKeys.class, "POPPY-4.1");

		callAndStopOnFailure(EnsureResponseTypeIsCode.class, "RFC6749-4.1.1");
		callAndStopOnFailure(EnsurePoppyRedirectUriMatchesClientMetadata.class, "POPPY-4.2");
		callAndStopOnFailure(EnsurePoppyAuthorizationRequestContainsS256CodeChallenge.class, "POPPY-4.2");
		callAndStopOnFailure(EnsurePoppyAuthorizationRequestContainsScope.class, "POPPY-4.7");

		callAndStopOnFailure(CreateAuthorizationCode.class);
		callAndStopOnFailure(CreateAuthorizationEndpointResponseParams.class);
		callAndStopOnFailure(AddCodeToAuthorizationEndpointResponseParams.class);
		callAndStopOnFailure(AddIssToAuthorizationEndpointResponseParams.class, "OAuth2-iss-2");
		callAndStopOnFailure(SendAuthorizationResponseWithResponseModeQuery.class);

		String redirectTo = env.getString("authorization_endpoint_response_redirect");

		setStatus(Status.WAITING);
		call(exec().unmapKey("incoming_request").endBlock());

		return new RedirectView(redirectTo, false, false, false);
	}

	protected Object tokenEndpoint(String requestId) {
		setStatus(Status.RUNNING);

		call(exec().startBlock("Token endpoint").mapKey("token_endpoint_request", requestId));

		callAndStopOnFailure(ExtractClientAssertion.class, "POPPY-4.1");
		callAndStopOnFailure(EnsureClientAssertionTypeIsJwt.class, "POPPY-4.1");
		callAndStopOnFailure(EnsurePoppyClientAssertionAlgIsAdvertised.class, "POPPY-4.1");
		callAndStopOnFailure(ValidateClientAssertionSignature.class, "POPPY-4.1");
		callAndStopOnFailure(ValidateClientAssertionClaims.class, "POPPY-4.1");
		callAndStopOnFailure(ValidateClientAssertionAudClaimIsIssuerAsString.class, "POPPY-4.1");
		callAndStopOnFailure(EnsurePoppyClientAssertionContainsIat.class, "POPPY-4.1");
		callAndContinueOnFailure(EnsurePoppyClientAssertionIsShortLived.class, ConditionResult.WARNING, "POPPY-4.1");
		callAndContinueOnFailure(CheckForClientAssertionJtiReuse.class, ConditionResult.WARNING, "RFC7523-3");
		callAndStopOnFailure(ValidateAuthorizationCode.class, "RFC6749-4.1.3");
		callAndStopOnFailure(ValidateRedirectUriForTokenEndpointRequest.class, "RFC6749-4.1.3");
		call(sequence(CheckPkceCodeVerifier.class));

		callAndStopOnFailure(GenerateBearerAccessToken.class);
		callAndStopOnFailure(CreateRefreshToken.class);
		env.putString("access_token_expiration", "3600");
		env.putString("scope", env.getString(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, CreateEffectiveAuthorizationRequestParameters.SCOPE));
		callAndStopOnFailure(CreateTokenEndpointResponse.class);

		JsonObject response = env.getObject("token_endpoint_response");

		call(exec().unmapKey("token_endpoint_request").endBlock());
		setStatus(Status.WAITING);

		HttpHeaders headers = jsonHeaders();
		headers.setCacheControl("no-store");
		return new ResponseEntity<Object>(response, headers, HttpStatus.OK);
	}

	protected Object apiEndpoint(String requestId) {
		setStatus(Status.RUNNING);

		call(exec().startBlock("API endpoint").mapKey("incoming_request", requestId));

		callAndStopOnFailure(EnsureIncomingRequestMethodIsGet.class, "POPPY-5.2");
		callAndStopOnFailure(EnsurePoppyNoCredentialsInQuery.class, "POPPY-5.2");
		callAndStopOnFailure(ExtractBearerAccessTokenFromHeader.class, "RFC6750-2.1");
		callAndStopOnFailure(ExtractPoppySessionHeaders.class, "POPPY-3.4", "POPPY-5.2");
		callAndStopOnFailure(EnsurePoppyVersionIsSupported.class, "POPPY-3.4");
		callAndStopOnFailure(ValidatePoppyDelegation.class, "POPPY-5.2");
		callAndStopOnFailure(ValidatePoppySessionJwt.class, "POPPY-5.2", "POPPY-5.3");

		JsonObject body = new JsonObject();
		body.addProperty("user", OIDFJSON.getString(env.getObject("poppy_session_claims").get("sub")));
		body.addProperty("scope", PoppyConstants.COMPANY_SCOPES.iterator().next());

		call(exec().unmapKey("incoming_request").endBlock());

		ResponseEntity<Object> response = new ResponseEntity<>(body, jsonHeaders(), HttpStatus.OK);
		fireTestFinished();
		return response;
	}
}
