package net.openid.conformance.openid.federation.client;

import com.google.common.base.Strings;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.as.AddCodeToAuthorizationEndpointResponseParams;
import net.openid.conformance.condition.as.CheckClientIdMatchesOnTokenRequestIfPresent;
import net.openid.conformance.condition.as.CreateAuthorizationCode;
import net.openid.conformance.condition.as.CreateAuthorizationEndpointResponseParams;
import net.openid.conformance.condition.as.CreateEffectiveAuthorizationRequestParameters;
import net.openid.conformance.condition.as.EnsureAuthorizationHttpRequestContainsOpenIDScope;
import net.openid.conformance.condition.as.EnsureAuthorizationRequestContainsPkceCodeChallenge;
import net.openid.conformance.condition.as.EnsureNumericRequestObjectClaimsAreNotNull;
import net.openid.conformance.condition.as.EnsureOptionalAuthorizationRequestParametersMatchRequestObject;
import net.openid.conformance.condition.as.EnsurePAREndpointRequestDoesNotContainRequestUriParameter;
import net.openid.conformance.condition.as.EnsureRequestObjectDoesNotContainRequestOrRequestUri;
import net.openid.conformance.condition.as.EnsureRequestObjectDoesNotContainSubWithClientId;
import net.openid.conformance.condition.as.EnsureRequiredAuthorizationRequestParametersMatchRequestObject;
import net.openid.conformance.condition.as.ExtractNonceFromAuthorizationRequest;
import net.openid.conformance.condition.as.ExtractRequestObject;
import net.openid.conformance.condition.as.ExtractRequestedScopes;
import net.openid.conformance.condition.as.GenerateIdTokenClaims;
import net.openid.conformance.condition.as.LoadServerJWKs;
import net.openid.conformance.condition.as.OIDCCGetStaticClientConfigurationForRPTests;
import net.openid.conformance.condition.as.OIDCCValidateRequestObjectExp;
import net.openid.conformance.condition.as.SendAuthorizationResponseWithResponseModeQuery;
import net.openid.conformance.condition.as.SignIdToken;
import net.openid.conformance.condition.as.ValidateAuthorizationCode;
import net.openid.conformance.condition.as.ValidateEncryptedRequestObjectHasKid;
import net.openid.conformance.condition.as.ValidateRedirectUriForTokenEndpointRequest;
import net.openid.conformance.condition.as.ValidateRequestObjectAud;
import net.openid.conformance.condition.as.ValidateRequestObjectIat;
import net.openid.conformance.condition.as.ValidateRequestObjectIss;
import net.openid.conformance.condition.as.ValidateRequestObjectJti;
import net.openid.conformance.condition.as.ValidateRequestObjectMaxAge;
import net.openid.conformance.condition.as.ValidateRequestObjectSignature;
import net.openid.conformance.condition.as.ValidateRequestObjectSubNotPresent;
import net.openid.conformance.condition.as.par.CreatePAREndpointResponse;
import net.openid.conformance.condition.client.ExtractJWKsFromStaticClientConfiguration;
import net.openid.conformance.condition.client.ValidateServerJWKs;
import net.openid.conformance.condition.rs.OIDCCLoadUserInfo;
import net.openid.conformance.openid.federation.AddFederationEntityMetadataToEntityConfiguration;
import net.openid.conformance.openid.federation.AddOpenIDProviderMetadataToEntityConfiguration;
import net.openid.conformance.openid.federation.CallEntityStatementEndpointAndReturnFullResponse;
import net.openid.conformance.openid.federation.EntityUtils;
import net.openid.conformance.openid.federation.ExtractJWTFromFederationEndpointResponse;
import net.openid.conformance.openid.federation.NonBlocking;
import net.openid.conformance.openid.federation.SetPrimaryEntityStatement;
import net.openid.conformance.openid.federation.TrustChainVerifier;
import net.openid.conformance.openid.federation.ValidateFederationUrl;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.testmodule.TestFailureException;
import net.openid.conformance.testmodule.UserFacing;
import net.openid.conformance.variant.FAPIAuthRequestMethod;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.view.RedirectView;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@PublishTestModule(
	testName = "openid-federation-client-happy-path",
	displayName = "openid-federation-client-happy-path",
	summary = "openid-federation-client-happy-path",
	profile = "OIDFED",
	configurationFields = {
		"federation.authority_hints",
		"federation.immediate_subordinates",
		"federation_trust_anchor.immediate_subordinates",
		"federation_trust_anchor.trust_anchor_jwks",
		"federation.entity_identifier_host_override",
		"client.entity_identifier",
		"client.trust_anchor",
		"client.jwks",
		"server.jwks",
		"internal.op_to_rp_mode",
		"internal.ignore_exp_iat"
	}
)

@SuppressWarnings("unused")
public class OpenIDFederationClientHappyPathTest extends AbstractOpenIDFederationClientTest {

	protected ConditionCaller caller = this::callAndContinueOnFailure;

	@Override
	public void configure(JsonObject config, String baseUrl, String externalUrlOverride, String baseMtlsUrl) {

		String hostOverride = OIDFJSON.getStringOrNull(config.get("federation").getAsJsonObject().get("entity_identifier_host_override"));
		if (!Strings.isNullOrEmpty(hostOverride)) {
			baseUrl = EntityUtils.replaceHostnameInUrl(baseUrl, hostOverride);
		}

		env.putString("base_url", baseUrl);
		env.putString("base_mtls_url", baseMtlsUrl);
		env.putObject("config", config);

		env.putString("entity_identifier", baseUrl);
		exposeEnvString("entity_identifier");

		env.putString("entity_configuration_url", baseUrl + "/.well-known/openid-federation");
		exposeEnvString("entity_configuration_url");

		env.putString("federation_fetch_endpoint", baseUrl + "/fetch");
		exposeEnvString("federation_fetch_endpoint");

		env.putString("federation_list_endpoint", baseUrl + "/list");
		exposeEnvString("federation_list_endpoint");

		env.putString("trust_anchor_entity_identifier", baseUrl + "/trust-anchor");
		exposeEnvString("trust_anchor_entity_identifier");

		env.putString("trust_anchor_entity_configuration_url", baseUrl + "/trust-anchor/.well-known/openid-federation");
		exposeEnvString("trust_anchor_entity_configuration_url");

		callAndStopOnFailure(AddSelfHostedTrustAnchorToEntityConfiguration.class);
		callAndStopOnFailure(AddSelfToTrustAnchorImmediateSubordinates.class);

		callAndStopOnFailure(LoadServerJWKs.class);
		callAndStopOnFailure(ValidateServerJWKs.class, "RFC7517-1.1");
		callAndStopOnFailure(GenerateEntityConfiguration.class);
		callAndStopOnFailure(AddFederationEntityMetadataToEntityConfiguration.class);
		callAndStopOnFailure(AddOpenIDProviderMetadataToEntityConfiguration.class);
		callAndStopOnFailure(addTokenEndpointAuthMethodSupported);

		callAndStopOnFailure(LoadTrustAnchorJWKs.class);
		callAndStopOnFailure(ValidateTrustAnchorJWKs.class, "RFC7517-1.1");
		callAndStopOnFailure(GenerateTrustAnchorEntityConfiguration.class);
		callAndStopOnFailure(AddFederationEntityMetadataToTrustAnchorEntityConfiguration.class);

		env.putString("config", "client.client_id", env.getString("config", "client.entity_identifier"));
		callAndStopOnFailure(OIDCCGetStaticClientConfigurationForRPTests.class);
		callAndStopOnFailure(ExtractJWKsFromStaticClientConfiguration.class);
		callAndStopOnFailure(ValidateClientTrustAnchor.class);

		additionalConfiguration();

		setStatus(Status.CONFIGURED);
		fireSetupDone();
	}

	@Override
	public void additionalConfiguration() {
		if (opToRpMode()) {
			// We want to stop on failure to produce an error
			caller = this::callAndStopOnFailure;
		}
	}

	@Override
	public void start() {
		setStatus(Status.WAITING);
	}

	@Override
	public Object handleHttp(String path, HttpServletRequest req, HttpServletResponse res, HttpSession session, JsonObject requestParts) {
		String requestId = "incoming_request_" + RandomStringUtils.secure().nextAlphanumeric(37);
		env.putObject(requestId, requestParts);

		if (path.startsWith("trust-anchor/")) {
			return switch(path) {
				case "trust-anchor/.well-known/openid-federation" -> trustAnchorEntityConfigurationResponse();
				case "trust-anchor/jwks" -> trustAnchorJwksResponse();
				case "trust-anchor/fetch" -> trustAnchorFetchResponse(requestId);
				case "trust-anchor/list" -> trustAnchorListResponse(requestId);
				case "trust-anchor/resolve" -> trustAnchorResolveResponse(requestId);
				default -> new ResponseEntity<>(HttpStatus.NOT_FOUND);
			};
		}

		return switch (path) {
			case ".well-known/openid-federation" -> entityConfigurationResponse();
			case ".well-known/openid-configuration" -> openIdConfigurationResponse();
			case "jwks" -> jwksResponse();
			case "fetch" -> fetchResponse(requestId);
			case "list" -> listResponse(requestId);
			case "authorize" -> authorizeResponse(requestId); // authorizeErrorResponse(requestId);
			case "par" -> parResponse(requestId);
			case "token" -> tokenResponse(requestId);
			default -> new ResponseEntity<>(HttpStatus.NOT_FOUND);
		};
	}

	protected Object entityConfigurationResponse() {
		if (opToRpMode()) {
			env.mapKey("entity_configuration_claims", "server");
			env.mapKey("entity_configuration_claims_jwks", "server_jwks");
			return NonBlocking.entityConfigurationResponse(env, getId());
		}
		return super.entityConfigurationResponse("server", SignEntityStatementWithServerKeys.class);
	}

	protected Object openIdConfigurationResponse() {
		JsonElement openIdProviderConfiguration = env.getElementFromObject("server", "metadata.openid_provider");
		return new ResponseEntity<Object>(openIdProviderConfiguration, HttpStatus.OK);
	}

	protected Object trustAnchorEntityConfigurationResponse() {
		if (opToRpMode()) {
			env.mapKey("entity_configuration_claims", "trust_anchor");
			env.mapKey("entity_configuration_claims_jwks", "trust_anchor_jwks");
			return NonBlocking.entityConfigurationResponse(env, getId());
		}
		return super.entityConfigurationResponse("trust_anchor", SignEntityStatementWithTrustAnchorKeys.class);
	}

	protected Object jwksResponse() {
		return jwksResponse("server_public_jwks");
	}

	protected Object trustAnchorJwksResponse() {
		return jwksResponse("trust_anchor_public_jwks");
	}

	protected Object jwksResponse(String mapKey) {
		setStatus(Status.RUNNING);
		JsonObject jwks = env.getObject(mapKey);
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
		if (error != null) {
			response = errorResponse(error, errorDescription, statusCode);
		} else {
			env.putObject("original_server_jwks", env.getObject("server_jwks")); // Save the jwks, as we'll have to restore it later
			env.putString("federation_endpoint_url", EntityUtils.appendWellKnown(env.getString("fetch_endpoint_parameter_sub")));

			setStatus(Status.WAITING);
			callAndStopOnFailure(ValidateFederationUrl.class, Condition.ConditionResult.FAILURE, "OIDFED-1.2");
			callAndStopOnFailure(CallEntityStatementEndpointAndReturnFullResponse.class, Condition.ConditionResult.FAILURE, "OIDFED-9");
			setStatus(Status.RUNNING);

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
			env.mapKey("entity_configuration_claims", "federation_fetch_response");
			callAndStopOnFailure(SignEntityStatementWithServerKeys.class);
			env.unmapKey("entity_configuration_claims");
			String federationFetchResponse = env.getString("signed_entity_statement");
			response = ResponseEntity
				.status(200)
				.contentType(EntityUtils.ENTITY_STATEMENT_JWT)
				.body(federationFetchResponse);
		}

		call(exec().unmapKey("incoming_request").endBlock());
		setStatus(Status.WAITING);

		return response;
	}

	protected Object trustAnchorFetchResponse(String requestId) {
		if (opToRpMode()) {
			return NonBlocking.trustAnchorFetchResponse(env, getId(), requestId);
		}

		String sub = env.getString(requestId, "query_string_params.sub");
		String alias = env.getString("config", "alias");
		if (sub.endsWith(alias)) {
			return NonBlocking.trustAnchorFetchResponse(env, getId(), requestId);
		}

		setStatus(Status.RUNNING);
		call(exec().startBlock("Trust anchor fetch endpoint").mapKey("incoming_request", requestId));

		callAndContinueOnFailure(ValidateSubParameterForTrustAnchorFetchEndpoint.class,  Condition.ConditionResult.FAILURE);

		String error = env.getString("federation_fetch_endpoint_error");
		String errorDescription = env.getString("federation_fetch_endpoint_error_description");
		Integer statusCode = env.getInteger("federation_fetch_endpoint_status_code");

		ResponseEntity<Object> response = null;
		if (error != null) {
			response = errorResponse(error, errorDescription, statusCode);
		} else {
			env.putString("federation_endpoint_url", EntityUtils.appendWellKnown(env.getString("fetch_endpoint_parameter_sub")));

			callAndStopOnFailure(ValidateFederationUrl.class, Condition.ConditionResult.FAILURE, "OIDFED-1.2");
			callAndStopOnFailure(CallEntityStatementEndpointAndReturnFullResponse.class, Condition.ConditionResult.FAILURE, "OIDFED-9");
			validateEntityStatementResponse();
			callAndStopOnFailure(ExtractJWTFromFederationEndpointResponse.class, "OIDFED-9");
			validateEntityStatement();
			env.removeNativeValue("federation_endpoint_url");

			JsonObject claims = env.getElementFromObject("federation_response_jwt", "claims").getAsJsonObject();
			claims.remove("authority_hints");
			claims.remove("trust_mark_issuers");
			claims.remove("trust_mark_owners");
			claims.addProperty("iss", env.getString("trust_anchor_entity_identifier"));
			claims.addProperty("source_endpoint", env.getString("federation_fetch_endpoint"));
			env.putObject("federation_fetch_response", claims);

			env.mapKey("entity_configuration_claims", "federation_fetch_response");
			callAndStopOnFailure(SignEntityStatementWithTrustAnchorKeys.class);
			env.unmapKey("entity_configuration_claims");
			String federationFetchResponse = env.getString("signed_entity_statement");
			response = ResponseEntity
				.status(200)
				.contentType(EntityUtils.ENTITY_STATEMENT_JWT)
				.body(federationFetchResponse);
		}

		call(exec().unmapKey("incoming_request").endBlock());
		setStatus(Status.WAITING);

		return response;
	}

	protected Object trustAnchorListResponse(String requestId) {
		setStatus(Status.RUNNING);
		call(exec().startBlock("List endpoint").mapKey("incoming_request", requestId));

		JsonArray immediateSubordinates = env.getElementFromObject("config", "federation_trust_anchor.immediate_subordinates").getAsJsonArray();

		call(exec().unmapKey("incoming_request").endBlock());
		setStatus(Status.WAITING);

		return new ResponseEntity<Object>(immediateSubordinates, HttpStatus.OK);
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

	protected Object trustAnchorResolveResponse(String requestId) {

		String sub = env.getString(requestId, "query_string_params.sub");
		String alias = env.getString("config", "alias");
		if (sub.endsWith(alias)) {
			JsonObject error = new JsonObject();
			error.addProperty("error", "invalid_request");
			error.addProperty("error_description", "The test suite is (technically) not able to build " +
				"a trust chain that includes the test alias entity, i.e. " + sub);
			return ResponseEntity
				.status(400)
				.contentType(MediaType.APPLICATION_JSON)
				.body(error);
		}

		setStatus(Status.RUNNING);
		call(exec().startBlock("Trust anchor resolve endpoint").mapKey("incoming_request", requestId));

		callAndContinueOnFailure(ExtractParametersForTrustAnchorResolveEndpoint.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(ValidateSubParameterForTrustAnchorResolveEndpoint.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(ValidateTrustAnchorParameterForResolveEndpoint.class, Condition.ConditionResult.FAILURE);

		String error = env.getString("federation_resolve_endpoint_error");
		String errorDescription = env.getString("federation_resolve_endpoint_error_description");
		Integer statusCode = env.getInteger("federation_resolve_endpoint_status_code");

		ResponseEntity<Object> response = null;
		if (error != null) {
			response = errorResponse(error, errorDescription, statusCode);
		} else {
			sub = env.getString("resolve_endpoint_parameter_sub");
			env.putString("federation_endpoint_url", EntityUtils.appendWellKnown(sub));
			callAndStopOnFailure(ValidateFederationUrl.class, Condition.ConditionResult.FAILURE, "OIDFED-1.2");
			fetchAndVerifyEntityStatement();
			callAndContinueOnFailure(SetPrimaryEntityStatement.class, Condition.ConditionResult.FAILURE);

			String trustAnchor = env.getString("resolve_endpoint_parameter_trust_anchor");
			JsonArray trustChain;
			try {
				List<String> trustChainList = findPath(sub, trustAnchor);
				if (trustChainList.isEmpty()) {
					throw new TestFailureException(getId(), "Could not build a trust chain from the sub %s to trust anchor %s".formatted(sub, trustAnchor));
				}
				trustChain = buildTrustChain(trustChainList);
				TrustChainVerifier.VerificationResult result = TrustChainVerifier.verifyTrustChain(sub, trustAnchor, OIDFJSON.convertJsonArrayToList(trustChain));
				if(!result.isVerified()) {
					throw new TestFailureException(getId(), "Could not verify the trust chain from the sub %s to trust anchor %s. Error: %s"
						.formatted(sub, trustAnchor, result.getError()));
				} else {
					eventLog.log(getId(),"**** TRUST CHAIN VERIFIED ****");
				}
			} catch (CyclicPathException e) {
				throw new TestFailureException(getId(), e.getMessage(), e);
			}

			JsonObject resolveResponse = EntityUtils.createBasicClaimsObject(trustAnchor, sub);

			// Keep only the metadata that matches the entity type parameter(s)
			JsonArray entityTypes = env.getElementFromObject("resolve_endpoint_parameters", "entity_types").getAsJsonArray();
			JsonObject metadata = env.getElementFromObject("primary_entity_statement_jwt", "claims.metadata").getAsJsonObject();
			List<String> entityTypesList = OIDFJSON.convertJsonArrayToList(entityTypes);
			JsonObject filteredMetadata = filterMetadataForEntityTypes(metadata, entityTypesList);

			resolveResponse.add("metadata", filteredMetadata);
			resolveResponse.add("trust_chain", trustChain);
			env.putObject("federation_resolve_response", resolveResponse);

			env.mapKey("entity_configuration_claims", "federation_resolve_response");
			callAndStopOnFailure(SignResolveResponseWithTrustAnchorKeys.class);
			env.unmapKey("entity_configuration_claims");
			String federationResolveResponse = env.getString("signed_entity_statement");
			response = ResponseEntity
				.status(200)
				.contentType(EntityUtils.RESOLVE_RESPONSE_JWT)
				.body(federationResolveResponse);
		}
		call(exec().unmapKey("incoming_request").endBlock());
		setStatus(Status.WAITING);

		return response;
	}

	@UserFacing
	protected Object authorizeResponse(String requestId) {
		setStatus(Status.RUNNING);
		call(exec().startBlock("Authorization endpoint").mapKey("incoming_request", requestId));
		env.mapKey("authorization_endpoint_http_request", requestId);
		env.putString("server", "issuer", env.getString("entity_identifier"));

		String requestUri = env.getString("authorization_endpoint_http_request", "query_string_params.request_uri");
		if (requestUri == null) {
			extractAndVerifyRequestObject(FAPIAuthRequestMethod.BY_VALUE);
			extractClientIdFromRequestObject();
			callAndStopOnFailure(ValidateClientIdInParametersMatchesEntityIdentifier.class, Condition.ConditionResult.FAILURE);
			callAndStopOnFailure(ValidateClientIdInRequestObjectMatchesEntityIdentifier.class, Condition.ConditionResult.FAILURE);
			extractRedirectUriFromRequestObject();
		} else {
			callAndContinueOnFailure(VerifyRequestUri.class, Condition.ConditionResult.FAILURE);
		}

		String rpEntity = env.getString("config", "client.entity_identifier");
		String opEntity = env.getString("entity_identifier");
		String rpTrustAnchor = env.getString("config", "client.trust_anchor");

		env.putString("federation_endpoint_url", EntityUtils.appendWellKnown(rpEntity));
		fetchAndVerifyEntityStatement();
		callAndContinueOnFailure(SetPrimaryEntityStatement.class, Condition.ConditionResult.FAILURE);

		JsonObject trustChainInfo = new JsonObject();
		trustChainInfo.addProperty("subject", rpEntity);
		trustChainInfo.addProperty("trust_anchor", rpTrustAnchor);
		env.putObject("trust_chain", trustChainInfo);

		// Get the trust chain from the request object if it exists, otherwise build it.
		callAndContinueOnFailure(ExtractTrustChainFromRequestObject.class, Condition.ConditionResult.FAILURE);
		JsonElement trustChainElement = env.getElementFromObject("trust_chain", "trust_chain");
		if (trustChainElement == null) {
			List<String> rpTrustChain;
			try {
				// There must be a trust chain from the RP to the trust anchor
				rpTrustChain = findPath(rpEntity, rpTrustAnchor);
				if (rpTrustChain.isEmpty()) {
					throw new TestFailureException(getId(), "Could not build a trust chain from the RP %s to trust anchor %s".formatted(rpEntity, rpTrustAnchor));
				}
				env.putArray("trust_chain", "trust_chain", buildTrustChain(rpTrustChain));

				// And there must also be a trust chain from the OP (i.e. this test) to that  trust anchor
				List<String> opTrustChain = findPath(opEntity, rpTrustAnchor);
				if (opTrustChain.isEmpty()) {
					throw new TestFailureException(getId(), "Could not build a trust chain from the OP %s to trust anchor %s".formatted(opEntity, rpTrustAnchor));
				}
			} catch (CyclicPathException e) {
				throw new TestFailureException(getId(), e.getMessage(), e);
			}
		}

		caller.call(VerifyTrustChain.class, Condition.ConditionResult.FAILURE);

		callAndContinueOnFailure(CreateAuthorizationCode.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(CreateAuthorizationEndpointResponseParams.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(AddCodeToAuthorizationEndpointResponseParams.class, Condition.ConditionResult.FAILURE,  "OIDCC-3.3.2.5");
		callAndContinueOnFailure(SendAuthorizationResponseWithResponseModeQuery.class, Condition.ConditionResult.FAILURE, "OIDCC-3.3.2.5");
		exposeEnvString("authorization_endpoint_response_redirect");
		String redirectTo = env.getString("authorization_endpoint_response_redirect");
		Object viewToReturn = new RedirectView(redirectTo, false, false, false);

		env.unmapKey("authorization_endpoint_http_request");
		call(exec().unmapKey("incoming_request").endBlock());
		setStatus(Status.WAITING);

		return viewToReturn;
	}

	protected Object parResponse(String requestId) {
		setStatus(Status.RUNNING);
		call(exec().startBlock("PAR endpoint").mapKey("incoming_request", requestId));
		env.mapKey("par_endpoint_http_request", requestId);
		env.mapKey("authorization_endpoint_http_request", requestId);
		env.putString("server", "issuer", env.getString("entity_identifier"));

		callAndContinueOnFailure(EnsurePAREndpointRequestDoesNotContainRequestUriParameter.class, Condition.ConditionResult.FAILURE, "PAR-2.1");

		extractAndVerifyRequestObject(FAPIAuthRequestMethod.PUSHED);
		extractClientIdFromRequestObject();
		callAndStopOnFailure(ValidateClientIdInRequestObjectMatchesEntityIdentifier.class, Condition.ConditionResult.FAILURE);
		extractRedirectUriFromRequestObject();

		callAndContinueOnFailure(CreatePAREndpointResponse.class, Condition.ConditionResult.FAILURE, "PAR-2.1");

		env.mapKey("par_endpoint_http_request", requestId);
		env.unmapKey("authorization_endpoint_http_request");
		call(exec().unmapKey("incoming_request").endBlock());
		setStatus(Status.WAITING);

		JsonObject response = env.getObject("par_endpoint_response");
		return new ResponseEntity<Object>(response, HttpStatus.OK);
	}

	protected Object tokenResponse(String requestId) {
		setStatus(Status.RUNNING);
		call(exec().startBlock("Token endpoint").mapKey("incoming_request", requestId));
		env.mapKey("token_endpoint_request", requestId);
		env.putString("issuer", env.getString("entity_identifier"));

		callAndContinueOnFailure(VerifyGrantTypeIsPresent.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(CheckClientIdMatchesOnTokenRequestIfPresent.class, Condition.ConditionResult.FAILURE, "RFC6749-3.2.1");

		env.putString("server", "issuer", env.getString("client", "entity_identifier"));
		env.putString("server", "token_endpoint", env.getString("server", "metadata.openid_provider.token_endpoint"));
		call(sequence(validateClientAuthenticationSteps));
		env.removeElement("server", "issuer");
		env.removeElement("server", "token_endpoint");

		callAndStopOnFailure(ValidateAuthorizationCode.class, "OIDCC-3.1.3.2");
		callAndContinueOnFailure(ValidateRedirectUriForTokenEndpointRequest.class, Condition.ConditionResult.FAILURE, "OIDCC-3.1.3.2");

		callAndContinueOnFailure(OIDCCLoadUserInfo.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(GenerateIdTokenClaims.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(SignIdToken.class, Condition.ConditionResult.FAILURE);
		JsonObject response = new JsonObject();
		response.addProperty("id_token", env.getString("id_token"));

		env.removeNativeValue("issuer");
		env.unmapKey("authorization_endpoint_http_request");
		call(exec().unmapKey("incoming_request").endBlock());
		fireTestFinished();

		return new ResponseEntity<Object>(response, HttpStatus.OK);
	}

	protected ResponseEntity<Object> errorResponse(String error, String errorDescription, Integer statusCode) {
		JsonObject errorObject = new JsonObject();
		errorObject.addProperty("error", error);
		errorObject.addProperty("error_description", errorDescription);
		env.removeNativeValue("federation_fetch_endpoint_error");
		env.removeNativeValue("federation_fetch_endpoint_error_description");
		env.removeNativeValue("federation_fetch_endpoint_status_code");
		return ResponseEntity
			.status(HttpStatus.valueOf(statusCode))
			.contentType(MediaType.APPLICATION_JSON)
			.body(errorObject);
	}

	protected void extractAndVerifyRequestObject(FAPIAuthRequestMethod requestMethod) {
		setAuthorizationEndpointRequestParamsForHttpMethod();
		if (FAPIAuthRequestMethod.BY_VALUE.equals(requestMethod)) {
			callAndContinueOnFailure(UrlDecodeClientIdQueryParameter.class, Condition.ConditionResult.FAILURE);
		}
		extractAuthorizationEndpointRequestParameters(requestMethod);
	}

	protected void extractClientIdFromRequestObject() {
		String clientId = env.getString("authorization_request_object", "claims.client_id");
		env.putString("request_object_client_id", clientId);
		env.putString("federation_endpoint_url", EntityUtils.appendWellKnown(clientId));
	}

	protected void extractRedirectUriFromRequestObject() {
		String redirectUri = env.getString("authorization_request_object", "claims.redirect_uri");
		env.putString("authorization_endpoint_request_redirect_uri", redirectUri);
	}

	protected void fetchAndVerifyEntityStatement() {
		callAndStopOnFailure(ValidateFederationUrl.class, Condition.ConditionResult.FAILURE, "OIDFED-1.2");
		callAndStopOnFailure(CallEntityStatementEndpointAndReturnFullResponse.class, Condition.ConditionResult.FAILURE, "OIDFED-9");
		callAndContinueOnFailure(ExtractJWTFromFederationEndpointResponse.class, Condition.ConditionResult.FAILURE);
		validateEntityStatementResponse();
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

	protected void extractAuthorizationEndpointRequestParameters(FAPIAuthRequestMethod requestMethod) {
		callAndStopOnFailure(ExtractRequestObject.class, "OIDCC-6.1");
		validateRequestObject();
		skipIfElementMissing("authorization_request_object", "jwe_header", Condition.ConditionResult.INFO, ValidateEncryptedRequestObjectHasKid.class, Condition.ConditionResult.FAILURE, "OIDCC-10.2", "OIDCC-10.2.1");

		if (FAPIAuthRequestMethod.BY_VALUE.equals(requestMethod)) {
			callAndStopOnFailure(EnsureAuthorizationHttpRequestContainsOpenIDScope.class, "OIDCC-6.1", "OIDCC-6.2");
			callAndStopOnFailure(EnsureRequiredAuthorizationRequestParametersMatchRequestObject.class, "OIDCC-6.1", "OIDCC-6.2");
			callAndContinueOnFailure(EnsureOptionalAuthorizationRequestParametersMatchRequestObject.class, Condition.ConditionResult.WARNING, "OIDCC-6.1", "OIDCC-6.2");
		}

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
		callAndContinueOnFailure(ValidateRequestObjectIat.class, Condition.ConditionResult.WARNING, "OIDCC-6.1");
		callAndContinueOnFailure(EnsureNumericRequestObjectClaimsAreNotNull.class, Condition.ConditionResult.WARNING, "OIDCC-13.3");
		callAndContinueOnFailure(EnsureRequestObjectDoesNotContainRequestOrRequestUri.class, Condition.ConditionResult.WARNING, "OIDCC-6.1");
		callAndContinueOnFailure(EnsureRequestObjectDoesNotContainSubWithClientId.class, Condition.ConditionResult.WARNING, "JAR-10.8");

		caller.call(OIDCCValidateRequestObjectExp.class, Condition.ConditionResult.FAILURE, "OIDCC-6.1", "OIDFED-12.1.1.1");
		caller.call(ValidateRequestObjectMaxAge.class, Condition.ConditionResult.FAILURE, "OIDCC-13.3");
		caller.call(ValidateRequestObjectJti.class, Condition.ConditionResult.FAILURE, "OIDFED-12.1.1.1");
		caller.call(ValidateRequestObjectIss.class, Condition.ConditionResult.FAILURE, "OIDCC-6.1");
		caller.call(ValidateRequestObjectAud.class, Condition.ConditionResult.FAILURE, "OIDCC-6.1");
		caller.call(ValidateRequestObjectSubNotPresent.class, Condition.ConditionResult.FAILURE, "OIDFED-12.1.1.1");

		// It needs to stop on failure and skipIfMissing doesn't do that
		/*
		skipIfMissing(new String[]{"client_public_jwks"}, null, Condition.ConditionResult.FAILURE,
			ValidateRequestObjectSignature.class, Condition.ConditionResult.FAILURE, "OIDCC-6.1");
		*/
		call(condition(ValidateRequestObjectSignature.class)
			.skipIfObjectsMissing("client_public_jwks")
			.onSkip(Condition.ConditionResult.FAILURE)
			.requirements("OIDCC-6.1")
			.onFail(Condition.ConditionResult.FAILURE));
	}

	protected static JsonObject filterMetadataForEntityTypes(JsonObject metadata, List<String> entityTypesList) {
		Set<String> entityTypesToRemove = new HashSet<>();
		for(String entityType : metadata.keySet()) {
			if (EntityUtils.STANDARD_ENTITY_TYPES.contains(entityType) &&  !entityTypesList.isEmpty() && !entityTypesList.contains(entityType)) {
				entityTypesToRemove.add(entityType);
			}
		}
		JsonObject filteredMetadata = metadata.deepCopy();
		entityTypesToRemove.forEach(filteredMetadata::remove);
		return filteredMetadata;
	}

}
