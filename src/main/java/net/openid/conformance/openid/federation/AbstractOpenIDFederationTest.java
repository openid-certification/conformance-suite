package net.openid.conformance.openid.federation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.EnsureContentTypeJson;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs200;
import net.openid.conformance.testmodule.AbstractTestModule;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.variant.ClientRegistration;
import net.openid.conformance.variant.ServerMetadata;
import net.openid.conformance.variant.VariantConfigurationFields;
import net.openid.conformance.variant.VariantNotApplicable;
import net.openid.conformance.variant.VariantParameters;

import java.util.ArrayList;
import java.util.List;

import static net.openid.conformance.openid.federation.EntityUtils.appendWellKnown;
import static net.openid.conformance.openid.federation.EntityUtils.stripWellKnown;

@VariantParameters({
	ServerMetadata.class,
	ClientRegistration.class
})
@VariantConfigurationFields(parameter = ServerMetadata.class, value = "static", configurationFields = {
	"federation.entity_configuration"
})
public abstract class AbstractOpenIDFederationTest extends AbstractTestModule {

	@Override
	public void configure(JsonObject config, String baseUrl, String externalUrlOverride, String baseMtlsUrl) {
		env.putString("base_url", baseUrl);
		env.putString("base_mtls_url", baseMtlsUrl);
		env.putObject("config", config);

		eventLog.startBlock("Fetch primary Entity Configuration");
		if (ServerMetadata.STATIC.equals(getVariant(ServerMetadata.class))) {
			// This case is perhaps not applicable in the general case,
			// but f ex the leaf entities in the Swedish sandbox federation
			// do not publish their own entity configurations.
			callAndStopOnFailure(GetStaticEntityStatement.class, Condition.ConditionResult.FAILURE);
			callAndStopOnFailure(ExtractEntityStatementUrlFromConfig.class, Condition.ConditionResult.FAILURE);
			callAndStopOnFailure(SetPrimaryEntityStatement.class, Condition.ConditionResult.FAILURE);
		} else {
			callAndStopOnFailure(ExtractEntityStatementUrlFromConfig.class, Condition.ConditionResult.FAILURE);
			callAndStopOnFailure(CallFederationEndpoint.class, Condition.ConditionResult.FAILURE);
			callAndStopOnFailure(SetPrimaryEntityStatement.class, Condition.ConditionResult.FAILURE);
			validateEntityStatementResponse();
		}
		eventLog.endBlock();

		setStatus(Status.CONFIGURED);
		fireSetupDone();
	}

	protected void validateEntityStatement() {
		String entityStatementUrl = env.getString("entity_statement_url");

		eventLog.startBlock("Validate basic claims in Entity Statement for %s".formatted(entityStatementUrl));
		callAndContinueOnFailure(ExtractBasicClaimsFromEntityStatement.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
		env.putString("expected_iss", stripWellKnown(entityStatementUrl));
		env.putString("expected_sub", stripWellKnown(entityStatementUrl));
		call(sequence(ValidateEntityStatementBasicClaimsSequence.class));
		eventLog.endBlock();

		eventLog.startBlock("Validate JWKs and signature in Entity Statement for %s".formatted(entityStatementUrl));
		callAndContinueOnFailure(ExtractJWKsFromEntityStatement.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
		call(sequence(ValidateEntityStatementSignatureSequence.class));
		eventLog.endBlock();

		eventLog.startBlock("Validate metadata in Entity Statement for %s".formatted(entityStatementUrl));
		callAndContinueOnFailure(ValidateEntityStatementMetadata.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
		eventLog.endBlock();

		eventLog.startBlock("Validate Federation Entity metadata for %s".formatted(entityStatementUrl));
		callAndContinueOnFailure(ValidateFederationEntityMetadata.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
		eventLog.endBlock();

		eventLog.startBlock("Validate OpenID Relying Party metadata for %s".formatted(entityStatementUrl));
		callAndContinueOnFailure(ExtractOpenIDRelyingPartyMetadata.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
		validateOpenIdRelyingPartyMetadata();
		eventLog.endBlock();

		eventLog.startBlock("Validate OpenID Provider metadata for %s".formatted(entityStatementUrl));
		callAndContinueOnFailure(ExtractOpenIDProviderMetadata.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
		validateOpenIdProviderMetadata();
		eventLog.endBlock();

		eventLog.startBlock("Validate OAuth Authorization Server metadata for %s".formatted(entityStatementUrl));
		callAndContinueOnFailure(ValidateOAuthAuthorizationServerMetadata.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
		eventLog.endBlock();

		eventLog.startBlock("Validate OAuth Client metadata for %s".formatted(entityStatementUrl));
		callAndContinueOnFailure(ValidateOAuthClientMetadata.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
		eventLog.endBlock();

		eventLog.startBlock("Validate OAuth Protected Resource metadata for %s".formatted(entityStatementUrl));
		callAndContinueOnFailure(ValidateOAuthProtectedResourceMetadata.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
		eventLog.endBlock();
	}

	protected void validateEntityStatementResponse() {
		env.mapKey("endpoint_response", "entity_statement_endpoint_response");
		call(sequence(ValidateEntityStatementResponseSequence.class));
		env.unmapKey("endpoint_response");
	}

	protected void validateOpenIdRelyingPartyMetadata() {
		if (env.containsObject("openid_relying_party_metadata")) {
			env.mapKey("client", "openid_relying_party_metadata");
			call(sequence(ValidateOpenIDRelyingPartyMetadataSequence.class));
			callAndContinueOnFailure(ValidateOpenIDRelyingPartyMetadata.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
			env.unmapKey("client");
			env.removeObject("openid_relying_party_metadata");
		}
	}

	protected void validateOpenIdProviderMetadata() {
		if (env.containsObject("openid_provider_metadata")) {
			env.mapKey("server", "openid_provider_metadata");
			call(new ValidateDiscoveryMetadataSequence(getVariant(ClientRegistration.class)));
			callAndContinueOnFailure(ValidateOpenIDProviderMetadata.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
			env.unmapKey("server");
			env.removeObject("openid_provider_metadata");
		}
	}

	protected void validateAbsenceOfMetadataPolicy() {
		String entity = env.getString("entity_statement_url");
		eventLog.startBlock("Validate that Entity Statement for %s does not have a metadata_policy".formatted(entity));
		callAndContinueOnFailure(ValidateAbsenceOfMetadataPolicy.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
		eventLog.endBlock();
	}

	protected void validateImmediateSuperiors() {
		String entity = env.getString("entity_statement_url");

		eventLog.startBlock("Validate authority hints in Entity Statement for %s".formatted(entity));
		skipIfElementMissing("entity_statement_body", "authority_hints", Condition.ConditionResult.INFO,
				ValidateAuthorityHints.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
		validateAuthorityHints();
		eventLog.endBlock();
	}

	protected void validateAuthorityHints() {
		JsonElement authorityHintsElement = env.getElementFromObject("entity_statement_body", "authority_hints");
		if (authorityHintsElement != null) {
			JsonArray authorityHints = authorityHintsElement.getAsJsonArray();
			for (JsonElement authorityHintElement : authorityHints) {
				String authorityHint = OIDFJSON.getString(authorityHintElement);
				String authorityHintUrl = appendWellKnown(authorityHint);

				// Get the entity statement for the Superior
				env.putString("entity_statement_url", authorityHintUrl);
				callAndStopOnFailure(CallFederationEndpoint.class, Condition.ConditionResult.FAILURE);
				validateEntityStatementResponse();
				validateEntityStatement();

				eventLog.startBlock("Validating subordinate statement by immediate superior %s".formatted(authorityHint));

				// Verify that the primary entity is present in the list endpoint result
				callAndContinueOnFailure(ExtractFederationListEndpoint.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
				callAndContinueOnFailure(CallListEndpoint.class, Condition.ConditionResult.FAILURE, "OIDFED-?");

				env.mapKey("endpoint_response", "federation_list_endpoint_response");
				callAndContinueOnFailure(EnsureHttpStatusCodeIs200.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
				callAndContinueOnFailure(EnsureContentTypeJson.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
				env.unmapKey("endpoint_response");

				callAndContinueOnFailure(VerifyPrimaryEntityPresenceInSubordinateListing.class, Condition.ConditionResult.FAILURE, "OIDFED-?");

				// Get the entity statement from the Superior's fetch endpoint
				env.putString("expected_sub", env.getString("primary_entity_statement_iss"));

				callAndContinueOnFailure(ExtractFederationFetchEndpoint.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
				callAndContinueOnFailure(AppendSubToFederationEndpointUrl.class, Condition.ConditionResult.FAILURE, "OIDFED-?");

				callAndStopOnFailure(CallFederationEndpoint.class, Condition.ConditionResult.FAILURE, "OIDFED-?");

				env.mapKey("endpoint_response", "entity_statement_endpoint_response");
				callAndContinueOnFailure(EnsureHttpStatusCodeIs200.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
				callAndContinueOnFailure(EnsureContentTypeEntityStatementJwt.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
				env.unmapKey("endpoint_response");

				call(sequence(ValidateEntityStatementSignatureSequence.class));

				callAndContinueOnFailure(ExtractBasicClaimsFromEntityStatement.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
				call(sequence(ValidateEntityStatementBasicClaimsSequence.class));

				callAndContinueOnFailure(ValidateEntityStatementMetadata.class, Condition.ConditionResult.FAILURE, "OIDFED-?");

				callAndContinueOnFailure(ValidateAbsenceOfAuthorityHints.class, Condition.ConditionResult.FAILURE, "OIDFED-3");
				callAndContinueOnFailure(ValidateAbsenceOfFederationEntityMetadata.class, Condition.ConditionResult.FAILURE, "OIDFED-8.1");

				callAndContinueOnFailure(ValidateEntityStatementMetadataPolicy.class, Condition.ConditionResult.FAILURE, "OIDFED-?");

				eventLog.endBlock();
			}
		}
	}

	protected List<String> validateSubordinates(String federationListEndpoint) {
		eventLog.startBlock(String.format("Retrieving entities from federation_list_endpoint %s", federationListEndpoint));
		callAndContinueOnFailure(CallListEndpoint.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
		env.mapKey("endpoint_response", "federation_list_endpoint_response");
		callAndContinueOnFailure(EnsureHttpStatusCodeIs200.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
		callAndContinueOnFailure(EnsureContentTypeJson.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
		callAndContinueOnFailure(EnsureResponseIsJsonArray.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
		env.unmapKey("endpoint_response");
		eventLog.endBlock();

		List<String> subordinates = new ArrayList<>();
		JsonArray listEndpointResponse = JsonParser.parseString(env.getString("endpoint_response_body")).getAsJsonArray();
		for (JsonElement listElement : listEndpointResponse) {
			String entityIdentifier = OIDFJSON.getString(listElement);
			eventLog.startBlock(String.format("Validating entity statement for %s", entityIdentifier));
			env.putString("entity_statement_url", appendWellKnown(entityIdentifier));
			callAndContinueOnFailure(CallFederationEndpoint.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
			validateEntityStatementResponse();
			validateEntityStatement();
			subordinates.add(entityIdentifier);
			eventLog.endBlock();
		}

		return subordinates;
	}

	protected List<String> getSubordinates(String federationListEndpoint) {
		eventLog.startBlock(String.format("Retrieving entities from federation_list_endpoint %s", federationListEndpoint));
		callAndContinueOnFailure(CallListEndpoint.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
		env.mapKey("endpoint_response", "federation_list_endpoint_response");
		callAndContinueOnFailure(EnsureHttpStatusCodeIs200.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
		callAndContinueOnFailure(EnsureContentTypeJson.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
		callAndContinueOnFailure(EnsureResponseIsJsonArray.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
		env.unmapKey("endpoint_response");
		eventLog.endBlock();

		List<String> subordinates = new ArrayList<>();
		JsonArray listEndpointResponse = JsonParser.parseString(env.getString("endpoint_response_body")).getAsJsonArray();
		for (JsonElement listElement : listEndpointResponse) {
			String entityIdentifier = OIDFJSON.getString(listElement);
			subordinates.add(entityIdentifier);
		}

		return subordinates;
	}

	protected List<String> findPath(String fromEntity, String trustAnchor) {
		List<String> path = findPath(fromEntity, trustAnchor, new ArrayList<>());
		eventLog.log(getName(), "Path to trust anchor: %s".formatted(String.join(" → ", path)));
		return path;
	}

	protected List<String> findPath(String fromEntity, String trustAnchor, List<String> path) {

		if (path.isEmpty()) {
			env.mapKey("entity_statement_body", "primary_entity_statement_body");
		} else {
			env.unmapKey("entity_statement_body");
			String currentWellKnownUrl = appendWellKnown(fromEntity);
			env.putString("entity_statement_url", currentWellKnownUrl);
			callAndStopOnFailure(CallFederationEndpoint.class, Condition.ConditionResult.FAILURE);
		}

		path.add(fromEntity);

		if (EntityUtils.equals(fromEntity, trustAnchor)) {
			return path;
		}

		JsonElement authorityHintsElement = env.getElementFromObject("entity_statement_body", "authority_hints");
		if (authorityHintsElement == null) {
			return null;
		}
		JsonArray authorityHints = authorityHintsElement.getAsJsonArray();
		if (authorityHints.isJsonNull() || authorityHints.isEmpty()) {
			return null;
		}

		for (JsonElement authorityHintElement : authorityHints) {
			String authorityHint = OIDFJSON.getString(authorityHintElement);
			List<String> result = findPath(authorityHint, trustAnchor, new ArrayList<>(path));
			if (result != null) {
				return result;
			}
		}

		return null;
	}

}
