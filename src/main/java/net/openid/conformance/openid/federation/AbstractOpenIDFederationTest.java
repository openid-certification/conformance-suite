package net.openid.conformance.openid.federation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.EnsureContentTypeJson;
import net.openid.conformance.openid.federation.client.ClientRegistration;
import net.openid.conformance.openid.federation.client.ExtractParametersForTrustAnchorResolveEndpoint;
import net.openid.conformance.openid.federation.client.SignResolveResponseWithTrustAnchorKeys;
import net.openid.conformance.openid.federation.client.ValidateSubParameterForTrustAnchorResolveEndpoint;
import net.openid.conformance.openid.federation.client.ValidateTrustAnchorParameterForResolveEndpoint;
import net.openid.conformance.testmodule.AbstractRedirectServerTestModule;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.testmodule.TestFailureException;
import net.openid.conformance.util.JWKUtil;
import net.openid.conformance.variant.FederationEntityMetadata;
import net.openid.conformance.variant.VariantConfigurationFields;
import net.openid.conformance.variant.VariantNotApplicable;
import net.openid.conformance.variant.VariantParameters;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.Serial;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static net.openid.conformance.openid.federation.EntityUtils.appendWellKnown;
import static net.openid.conformance.openid.federation.EntityUtils.stripWellKnown;

@VariantParameters({
	FederationEntityMetadata.class,
	ClientRegistration.class
})
@VariantConfigurationFields(parameter = FederationEntityMetadata.class, value = "static", configurationFields = {
	"federation.entity_configuration",
})
@VariantNotApplicable(parameter = ClientRegistration.class, values = {"explicit"})
public abstract class AbstractOpenIDFederationTest extends AbstractRedirectServerTestModule {

	public abstract void additionalConfiguration();

	protected boolean opToRpMode() {
		return "true".equals(env.getString("config", "internal.op_to_rp_mode"));
	}

	@Override
	protected void processCallback() { }

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
		return super.handleHttp(path, req, res, session, requestParts);
	}

	protected Object trustAnchorEntityConfigurationResponse() {
		env.mapKey("entity_configuration_claims", "trust_anchor");
		env.mapKey("entity_configuration_claims_jwks", "trust_anchor_jwks");
		Object entityConfigurationResponse = NonBlocking.entityConfigurationResponse(env, getId());
		env.unmapKey("entity_configuration_claims");
		env.unmapKey("entity_configuration_claims_jwks");
		return entityConfigurationResponse;
	}

	protected Object trustAnchorJwksResponse() {
		return jwksResponse("trust_anchor_public_jwks");
	}

	protected Object jwksResponse(String mapKey) {
		JsonObject jwks = env.getObject(mapKey);
		JsonObject publicJwks = JWKUtil.toPublicJWKSet(jwks);
		return ResponseEntity
			.status(HttpStatus.OK)
			.contentType(MediaType.APPLICATION_JSON)
			.body(publicJwks);
	}

	protected Object trustAnchorFetchResponse(String requestId) {
		return NonBlocking.trustAnchorFetchResponse(env, getId(), requestId);
	}

	protected Object trustAnchorListResponse(String requestId) {
		JsonArray immediateSubordinates = env.getElementFromObject("config", "federation_trust_anchor.immediate_subordinates").getAsJsonArray();
		return new ResponseEntity<Object>(immediateSubordinates, HttpStatus.OK);
	}

	protected Object trustAnchorResolveResponse(String requestId) {
		// This one is too complicated to execute in a non-blocking manner
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

	protected void fetchAndVerifyEntityStatement() {
		callAndStopOnFailure(ValidateFederationUrl.class, Condition.ConditionResult.FAILURE, "OIDFED-1.2");
		callAndStopOnFailure(CallEntityStatementEndpointAndReturnFullResponse.class, Condition.ConditionResult.FAILURE, "OIDFED-9");
		callAndContinueOnFailure(ExtractJWTFromFederationEndpointResponse.class, Condition.ConditionResult.FAILURE);
		validateEntityStatementResponse();
	}

	protected ResponseEntity<Object> errorResponse(String error, String errorDescription, Integer statusCode) {
		JsonObject errorObject = new JsonObject();
		errorObject.addProperty("error", error);
		errorObject.addProperty("error_description", errorDescription);

		env.removeNativeValue("federation_fetch_endpoint_error");
		env.removeNativeValue("federation_fetch_endpoint_error_description");
		env.removeNativeValue("federation_fetch_endpoint_status_code");

		env.removeNativeValue("federation_resolve_endpoint_error");
		env.removeNativeValue("federation_resolve_endpoint_error_description");
		env.removeNativeValue("federation_resolve_endpoint_status_code");

		return ResponseEntity
			.status(HttpStatus.valueOf(statusCode))
			.contentType(MediaType.APPLICATION_JSON)
			.body(errorObject);
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

	protected Object entityConfigurationResponse(String mapKey, Class<? extends Condition> signCondition) {
		setStatus(Status.RUNNING);

		env.mapKey("entity_configuration_claims", mapKey);
		callAndStopOnFailure(signCondition);
		env.unmapKey("entity_configuration_claims");
		String entityConfiguration = env.getString("signed_entity_statement");

		env.removeNativeValue("signed_entity_statement");
		setStatus(Status.WAITING);

		return ResponseEntity
			.status(HttpStatus.OK)
			.contentType(EntityUtils.ENTITY_STATEMENT_JWT)
			.body(entityConfiguration);
	}

	protected void validateEntityStatement() {
		String entityStatementUrl = env.getString("federation_endpoint_url");

		eventLog.startBlock("Validate basic claims in Entity Statement for %s".formatted(entityStatementUrl));
		callAndContinueOnFailure(ExtractRegisteredClaimsFromFederationResponse.class, Condition.ConditionResult.FAILURE, "OIDFED-3");
		env.putString("expected_iss", stripWellKnown(entityStatementUrl));
		env.putString("expected_sub", stripWellKnown(entityStatementUrl));
		call(sequence(ValidateFederationResponseBasicClaimsSequence.class));
		eventLog.endBlock();

		eventLog.startBlock("Validate JWKs and signature in Entity Statement for %s".formatted(entityStatementUrl));
		callAndContinueOnFailure(ExtractJWKsFromEntityStatement.class, Condition.ConditionResult.FAILURE, "OIDFED-3");
		call(sequence(ValidateFederationResponseSignatureSequence.class));
		eventLog.endBlock();

		eventLog.startBlock("Validate metadata in Entity Statement for %s".formatted(entityStatementUrl));
		callAndContinueOnFailure(ValidateEntityStatementMetadata.class, Condition.ConditionResult.INFO, "OIDFED-5");
		eventLog.endBlock();

		eventLog.startBlock("Validate Federation Entity metadata for %s".formatted(entityStatementUrl));
		callAndContinueOnFailure(ValidateFederationEntityMetadata.class, Condition.ConditionResult.FAILURE, "OIDFED-5.1.1");
		eventLog.endBlock();

		eventLog.startBlock("Validate OpenID Connect Relying Party metadata for %s".formatted(entityStatementUrl));
		callAndContinueOnFailure(ExtractOpenIDConnectRelyingPartyMetadata.class, Condition.ConditionResult.FAILURE, "OIDFED-5.1.2");
		validateOpenIDRelyingPartyMetadata();
		eventLog.endBlock();

		eventLog.startBlock("Validate OpenID Connect OpenID Provider metadata for %s".formatted(entityStatementUrl));
		callAndContinueOnFailure(ExtractOpenIDProviderMetadata.class, Condition.ConditionResult.FAILURE, "OIDFED-5.1.3");
		validateOpenIdProviderMetadata();
		eventLog.endBlock();

		eventLog.startBlock("Validate OAuth Authorization Server metadata for %s".formatted(entityStatementUrl));
		callAndContinueOnFailure(ValidateOAuthAuthorizationServerMetadata.class, Condition.ConditionResult.FAILURE, "OIDFED-5.1.4");
		eventLog.endBlock();

		eventLog.startBlock("Validate OAuth Client metadata for %s".formatted(entityStatementUrl));
		callAndContinueOnFailure(ValidateOAuthClientMetadata.class, Condition.ConditionResult.FAILURE, "OIDFED-5.1.5");
		eventLog.endBlock();

		eventLog.startBlock("Validate OAuth Protected Resource metadata for %s".formatted(entityStatementUrl));
		callAndContinueOnFailure(ValidateOAuthProtectedResourceMetadata.class, Condition.ConditionResult.FAILURE, "OIDFED-5.1.6");
		eventLog.endBlock();
	}

	protected void validateEntityStatementResponse() {
		env.mapKey("endpoint_response", "federation_endpoint_response");
		call(sequence(ValidateEntityStatementResponseSequence.class));
		env.unmapKey("endpoint_response");
	}

	protected void validateListResponse() {
		env.mapKey("endpoint_response", "federation_endpoint_response");
		call(sequence(ValidateListResponseSequence.class));
		env.unmapKey("endpoint_response");
	}

	protected void validateFetchResponse() {
		env.mapKey("endpoint_response", "federation_endpoint_response");
		call(sequence(ValidateFetchResponseSequence.class));
		env.unmapKey("endpoint_response");
	}

	protected void validateFetchErrorResponse(Class<? extends AbstractCondition> condition) {
		env.mapKey("endpoint_response", "federation_endpoint_response");
		callAndContinueOnFailure(EnsureContentTypeJson.class, Condition.ConditionResult.FAILURE, "OIDFED-8.1.2");
		callAndContinueOnFailure(EnsureResponseIsJsonObject.class, Condition.ConditionResult.FAILURE, "OIDFED-8.1.2");
		env.unmapKey("endpoint_response");

		env.mapKey("authorization_endpoint_response", "endpoint_response_body");
		skipIfMissing(new String[]{"authorization_endpoint_response"}, null, Condition.ConditionResult.INFO, condition, Condition.ConditionResult.WARNING, "OIDFED-8.1.2");
		env.unmapKey("authorization_endpoint_response");
	}

	protected void validateResolveResponse() {
		env.mapKey("endpoint_response", "federation_endpoint_response");
		call(sequence(ValidateResolveResponseSequence.class));
		env.unmapKey("endpoint_response");
	}

	protected void validateOpenIDRelyingPartyMetadata() {
		if (env.containsObject("openid_relying_party_metadata")) {
			env.mapKey("client", "openid_relying_party_metadata");
			call(sequence(ValidateOpenIDRelyingPartyMetadataSequence.class));
			callAndContinueOnFailure(ValidateClientRegistrationTypes.class, Condition.ConditionResult.FAILURE, "OIDFED-5.1.2");
			callAndContinueOnFailure(ValidateClientRegistrationTypesValues.class, Condition.ConditionResult.WARNING, "OIDFED-5.1.2");
			env.unmapKey("client");
			env.removeObject("openid_relying_party_metadata");
		}
	}

	protected void validateOpenIdProviderMetadata() {
		if (env.containsObject("openid_provider_metadata")) {
			env.mapKey("server", "openid_provider_metadata");
			String registrationEndpoint = env.getString("openid_provider_metadata", "registration_endpoint");
			net.openid.conformance.variant.ClientRegistration clientRegistration = registrationEndpoint != null
				? net.openid.conformance.variant.ClientRegistration.DYNAMIC_CLIENT
				: net.openid.conformance.variant.ClientRegistration.STATIC_CLIENT;
			call(new ValidateDiscoveryMetadataSequence(clientRegistration));
			callAndContinueOnFailure(ValidateClientRegistrationTypesSupported.class, Condition.ConditionResult.FAILURE, "OIDFED-5.1.3");
			skipIfElementMissing("openid_provider_metadata", "client_registration_types_supported", Condition.ConditionResult.INFO,
				ValidateClientRegistrationTypesSupportedValues.class, Condition.ConditionResult.WARNING, "OIDFED-5.1.3");
			ClientRegistration clientRegistrationType = getVariant(ClientRegistration.class);
			if (ClientRegistration.AUTOMATIC.equals(clientRegistrationType)) {
				callAndContinueOnFailure(ValidateClientRegistrationTypeAutomaticSupported.class, Condition.ConditionResult.FAILURE, "OIDFED-5.1.3");
			}
			if (ClientRegistration.EXPLICIT.equals(clientRegistrationType)) {
				callAndContinueOnFailure(ValidateClientRegistrationTypeExplicitSupported.class, Condition.ConditionResult.FAILURE, "OIDFED-5.1.3");
			}
			skipIfElementMissing("openid_provider_metadata", "client_registration_types_supported", Condition.ConditionResult.INFO,
				ValidateFederationRegistrationEndpoint.class, Condition.ConditionResult.FAILURE, "OIDFED-5.1.3");
			skipIfElementMissing("openid_provider_metadata", "client_registration_types_supported", Condition.ConditionResult.INFO,
				ValidateRequestAuthenticationMethodsSupported.class, Condition.ConditionResult.FAILURE, "OIDFED-5.1.3");
			skipIfElementMissing("openid_provider_metadata", "request_authentication_methods_supported", Condition.ConditionResult.INFO,
				ValidateRequestAuthenticationSigningAlgValuesSupported.class, Condition.ConditionResult.FAILURE, "OIDFED-5.1.3");
			env.unmapKey("server");
			env.removeObject("openid_provider_metadata");
		}
	}

	protected void validateAbsenceOfMetadataPolicy() {
		String entity = env.getString("federation_endpoint_url");
		eventLog.startBlock("Validate that Entity Statement for %s does not have a metadata_policy".formatted(entity));
		callAndContinueOnFailure(ValidateAbsenceOfMetadataPolicy.class, Condition.ConditionResult.FAILURE, "OIDFED-3");
		eventLog.endBlock();
	}

	protected void validateImmediateSuperiors() {
		String entity = env.getString("federation_endpoint_url");
		String anchor = env.getString("config", "federation.trust_anchor");
		// authority_hints is REQUIRED in Entity Configurations of the Entities that have at least one Superior above them,
		// such as Leaf and Intermediate Entities. This claim MUST NOT be present in Entity Configurations of Trust Anchors with no Superiors.
		if (!entity.startsWith(anchor)) {
			eventLog.startBlock("Validate authority hints in Entity Statement for %s".formatted(entity));
			callAndContinueOnFailure(ValidateAuthorityHints.class, Condition.ConditionResult.FAILURE, "OIDFED-3");
		} else {
			eventLog.startBlock("Validate absence of authority hints in Entity Statement for configured trust anchor %s".formatted(entity));
			callAndContinueOnFailure(ValidateAbsenceOfAuthorityHints.class, Condition.ConditionResult.FAILURE, "OIDFED-3");
		}
		validateSubordinateStatements();
		eventLog.endBlock();
	}

	protected void validateSubordinateStatements() {
		JsonElement authorityHintsElement = env.getElementFromObject("federation_response_jwt", "claims.authority_hints");
		if (authorityHintsElement != null) {
			JsonArray authorityHints = authorityHintsElement.getAsJsonArray();
			for (JsonElement authorityHintElement : authorityHints) {
				String authorityHint = OIDFJSON.getString(authorityHintElement);
				String authorityHintUrl = appendWellKnown(authorityHint);

				// Get the entity statement for the superior
				env.putString("federation_endpoint_url", authorityHintUrl);
				callAndStopOnFailure(ValidateFederationUrl.class, Condition.ConditionResult.FAILURE, "OIDFED-1.2");
				callAndStopOnFailure(CallEntityStatementEndpointAndReturnFullResponse.class, Condition.ConditionResult.FAILURE, "OIDFED-9");
				validateEntityStatementResponse();
				callAndStopOnFailure(ExtractJWTFromFederationEndpointResponse.class,  "OIDFED-9");
				validateEntityStatement();

				eventLog.startBlock("Validating subordinate statement by immediate superior %s".formatted(authorityHint));

				// Verify that the primary entity is present in the list endpoint result
				callAndStopOnFailure(ExtractFederationListEndpoint.class, Condition.ConditionResult.FAILURE, "OIDFED-5.1.1");
				callAndStopOnFailure(ValidateFederationUrl.class, Condition.ConditionResult.FAILURE, "OIDFED-1.2");
				callAndStopOnFailure(CallListEndpointAndReturnFullResponse.class, Condition.ConditionResult.FAILURE, "OIDFED-8.2.1");
				validateListResponse();
				callAndContinueOnFailure(VerifyPrimaryEntityPresenceInSubordinateListing.class, Condition.ConditionResult.FAILURE, "OIDFED-8.2");

				// Get the entity statement from the Superior's fetch endpoint
				env.putString("expected_sub", env.getString("primary_entity_statement_iss"));
				callAndStopOnFailure(ExtractFederationFetchEndpoint.class, Condition.ConditionResult.FAILURE, "OIDFED-8.1.1");
				callAndStopOnFailure(ValidateFederationUrl.class, Condition.ConditionResult.FAILURE, "OIDFED-1.2");

				callAndContinueOnFailure(AppendSubToFederationEndpointUrl.class, Condition.ConditionResult.FAILURE, "OIDFED-8.1.1");
				callAndStopOnFailure(CallFetchEndpointAndReturnFullResponse.class, Condition.ConditionResult.FAILURE, "OIDFED-8.1.1");
				validateFetchResponse();
				callAndStopOnFailure(ExtractJWTFromFederationEndpointResponse.class,  "OIDFED-8.1.2");

				call(sequence(ValidateFederationResponseSignatureSequence.class));

				callAndContinueOnFailure(ExtractRegisteredClaimsFromFederationResponse.class, Condition.ConditionResult.FAILURE, "OIDFED-3");
				call(sequence(ValidateFederationResponseBasicClaimsSequence.class));

				callAndContinueOnFailure(ValidateEntityStatementMetadata.class, Condition.ConditionResult.INFO, "OIDFED-5.1.1");
				// No authority hints in subordinate statements
				callAndContinueOnFailure(ValidateAbsenceOfAuthorityHints.class, Condition.ConditionResult.FAILURE, "OIDFED-3");
				// No federation_entity metadata in subordinate statements
				callAndContinueOnFailure(ValidateAbsenceOfFederationEntityMetadata.class, Condition.ConditionResult.FAILURE, "OIDFED-8.1");
				// Only Subordinate Statements may include this claim.
				callAndContinueOnFailure(ValidateEntityStatementMetadataPolicy.class, Condition.ConditionResult.FAILURE, "OIDFED-6.1.2");

				eventLog.endBlock();
			}
		}
	}

	protected List<String> findPath(String fromEntity, String toTrustAnchor) throws CyclicPathException {
		eventLog.log(getName(), "Finding path from %s to %s".formatted(fromEntity, toTrustAnchor));
		List<String> path = findPath(fromEntity, toTrustAnchor, new ArrayList<>());
		 if (path != null) {
			 eventLog.log(getName(), "Path to trust anchor: %s".formatted(String.join(" → ", path)));
			 return path;
		 } else {
			 eventLog.log(getName(), "Unable to find path from %s to trust anchor %s".formatted(fromEntity, toTrustAnchor));
			 return List.of();
		 }
	 }

	private List<String> findPath(String fromEntity, String toTrustAnchor, List<String> path) throws CyclicPathException {

		if (path.isEmpty()) {
			env.mapKey("federation_response_jwt", "primary_entity_statement_jwt");
		} else {
			env.unmapKey("federation_response_jwt");

			String currentWellKnownUrl = appendWellKnown(fromEntity);
			env.putString("federation_endpoint_url", currentWellKnownUrl);

			callAndStopOnFailure(ValidateFederationUrl.class, Condition.ConditionResult.FAILURE, "OIDFED-1.2");
			callAndStopOnFailure(CallEntityStatementEndpointAndReturnFullResponse.class, Condition.ConditionResult.FAILURE, "OIDFED-9");
			validateEntityStatementResponse();
			callAndStopOnFailure(ExtractJWTFromFederationEndpointResponse.class,  "OIDFED-9");
		}

		if (path.contains(fromEntity)) {
			throw new CyclicPathException("Cyclic path detected. Entity %s already exists in the path: %s".formatted(fromEntity, String.join(" → ", path)));
		}

		path.add(fromEntity);

		if (EntityUtils.equals(fromEntity, toTrustAnchor)) {
			return path;
		}

		JsonElement authorityHintsElement = env.getElementFromObject("federation_response_jwt", "claims.authority_hints");
		if (authorityHintsElement == null) {
			return null;
		}
		JsonArray authorityHints = authorityHintsElement.getAsJsonArray();
		if (authorityHints.isJsonNull() || authorityHints.isEmpty()) {
			return null;
		}

		for (JsonElement authorityHintElement : authorityHints) {
			String authorityHint = OIDFJSON.getString(authorityHintElement);
			List<String> result = findPath(authorityHint, toTrustAnchor, new ArrayList<>(path));
			if (result != null) {
				return result;
			}
		}

		return null;
	}

	protected JsonArray buildTrustChain(List<String> path) {
		eventLog.startBlock("Building trust chain from %s to %s".formatted(path.get(0), path.get(path.size() - 1)));

		JsonArray trustChain = new JsonArray();

		if (path.size() == 1) {
			trustChain.add(env.getString("primary_entity_statement_jwt", "value"));
			return trustChain;
		}

		String primaryEntityIdentifier = path.get(0);
		env.putString("federation_endpoint_url", appendWellKnown(primaryEntityIdentifier));
		callAndStopOnFailure(ValidateFederationUrl.class, Condition.ConditionResult.FAILURE, "OIDFED-1.2");
		callAndStopOnFailure(CallEntityStatementEndpointAndReturnFullResponse.class, Condition.ConditionResult.FAILURE, "OIDFED-9");
		validateEntityStatementResponse();
		callAndStopOnFailure(ExtractJWTFromFederationEndpointResponse.class,  "OIDFED-9");
		trustChain.add(OIDFJSON.getString(env.getElementFromObject("federation_response_jwt", "value")));

		for (int i = 1; i < path.size(); i++) {
			String entityIdentifier = path.get(i);
			env.putString("federation_endpoint_url", appendWellKnown(entityIdentifier));
			callAndStopOnFailure(ValidateFederationUrl.class, Condition.ConditionResult.FAILURE, "OIDFED-1.2");
			callAndStopOnFailure(CallEntityStatementEndpointAndReturnFullResponse.class, Condition.ConditionResult.FAILURE, "OIDFED-9");
			validateEntityStatementResponse();
			callAndStopOnFailure(ExtractJWTFromFederationEndpointResponse.class,  "OIDFED-9");
			callAndContinueOnFailure(ExtractFederationEntityMetadataUrls.class, Condition.ConditionResult.FAILURE, "OIDFED-3");

			String fetchEndpoint = env.getString("federation_fetch_endpoint");
			env.putString("federation_endpoint_url", fetchEndpoint);
			String sub = path.get(i - 1);
			env.putString("expected_sub", sub);
			callAndStopOnFailure(ValidateFederationUrl.class, Condition.ConditionResult.FAILURE, "OIDFED-1.2");
			callAndContinueOnFailure(AppendSubToFederationEndpointUrl.class, Condition.ConditionResult.FAILURE, "OIDFED-8.1.1");
			callAndStopOnFailure(CallFetchEndpointAndReturnFullResponse.class, Condition.ConditionResult.FAILURE, "OIDFED-8.1.1");
			validateFetchResponse();
			callAndStopOnFailure(ExtractJWTFromFederationEndpointResponse.class,  "OIDFED-8.1.2");
			trustChain.add(OIDFJSON.getString(env.getElementFromObject("federation_response_jwt", "value")));
		}

		String trustAnchorEntityIdentifier = path.get(path.size() - 1);
		env.putString("federation_endpoint_url", appendWellKnown(trustAnchorEntityIdentifier));
		callAndStopOnFailure(ValidateFederationUrl.class, Condition.ConditionResult.FAILURE, "OIDFED-1.2");
		callAndStopOnFailure(CallEntityStatementEndpointAndReturnFullResponse.class, Condition.ConditionResult.FAILURE, "OIDFED-9");
		validateEntityStatementResponse();
		callAndStopOnFailure(ExtractJWTFromFederationEndpointResponse.class,  "OIDFED-9");
		trustChain.add(OIDFJSON.getString(env.getElementFromObject("federation_response_jwt", "value")));
		eventLog.endBlock();

		return trustChain;
	}

	public static class CyclicPathException extends Exception {

		@Serial
		private static final long serialVersionUID = 1L;

		public CyclicPathException(String message) {
			super(message);
		}

	}

}
