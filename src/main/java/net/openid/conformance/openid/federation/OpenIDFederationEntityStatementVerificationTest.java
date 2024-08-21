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
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ClientRegistration;
import net.openid.conformance.variant.ServerMetadata;
import net.openid.conformance.variant.VariantConfigurationFields;
import net.openid.conformance.variant.VariantNotApplicable;
import net.openid.conformance.variant.VariantParameters;

@PublishTestModule(
	testName = "openid-federation-entity-configuration-endpoint-verification",
	displayName = "OpenID Federation: Entity Configuration Endpoint Verification",
	summary = "This test ensures that the server's entity configuration metadata is according to the specifications",
	profile = "OIDFED",
	configurationFields = {
		"federation.entity_statement_url",
		"federation.trust_anchor_jwks"
	}
)
@VariantParameters({
	ServerMetadata.class,
	ClientRegistration.class
})
@VariantConfigurationFields(parameter = ServerMetadata.class, value = "static", configurationFields = {
	"federation.entity_statement"
})
@VariantNotApplicable(parameter = ClientRegistration.class, values={ "static_client" })
public class OpenIDFederationEntityStatementVerificationTest extends AbstractTestModule {

	@Override
	public void configure(JsonObject config, String baseUrl, String externalUrlOverride, String baseMtlsUrl) {
		env.putString("base_url", baseUrl);
		env.putString("base_mtls_url", baseMtlsUrl);
		env.putObject("config", config);

		eventLog.startBlock("Fetch primary Entity Statement");
		if (ServerMetadata.STATIC.equals(getVariant(ServerMetadata.class))) {
			// This case is actually not valid, I believe, but it's here for testing purposes atm
			callAndStopOnFailure(GetStaticEntityStatement.class, Condition.ConditionResult.FAILURE);
		} else {
			callAndStopOnFailure(ExtractEntityStatmentUrlFromConfig.class, Condition.ConditionResult.FAILURE);
			callAndStopOnFailure(GetEntityStatement.class, Condition.ConditionResult.FAILURE);
			callAndStopOnFailure(SetPrimaryEntityStatement.class, Condition.ConditionResult.FAILURE);
			validateEntityStatementResponse();
		}
		eventLog.endBlock();

		setStatus(Status.CONFIGURED);
		fireSetupDone();
	}

	@Override
	public void start() {
		setStatus(Status.RUNNING);

		validateEntityStatement();
		validateAbsenceOfMetadataPolicy();
		validateSuperiors();

		fireTestFinished();
	}

	protected void validateEntityStatement() {
		String entity = env.getString("entity_statement_url");

		eventLog.startBlock("Validate basic claims in Entity Statement for %s".formatted(entity));
		call(sequence(ValidateEntityStatementBasicClaimsSequence.class));
		eventLog.endBlock();

		eventLog.startBlock("Validate JWKs and signature in Entity Statement for %s".formatted(entity));
		callAndContinueOnFailure(ExtractJWKsFromEntityStatement.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
		call(sequence(ValidateEntityStatementSignatureSequence.class));
		eventLog.endBlock();

		eventLog.startBlock("Validate metadata in Entity Statement for %s".formatted(entity));
		callAndContinueOnFailure(ValidateEntityStatementMetadata.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
		eventLog.endBlock();

		eventLog.startBlock("Validate Federation Entity metadata for %s".formatted(entity));
		callAndContinueOnFailure(ValidateFederationEntityMetadata.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
		eventLog.endBlock();

		eventLog.startBlock("Validate OpenID Relying Party metadata for %s".formatted(entity));
		callAndContinueOnFailure(ExtractOpenIDRelyingPartyMetadata.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
		validateOpenIdRelyingPartyMetadata();
		eventLog.endBlock();

		eventLog.startBlock("Validate OpenID Provider metadata for %s".formatted(entity));
		callAndContinueOnFailure(ExtractOpenIDProviderMetadata.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
		validateOpenIdProviderMetadata();
		eventLog.endBlock();

		eventLog.startBlock("Validate OAuth Authorization Server metadata for %s".formatted(entity));
		callAndContinueOnFailure(ValidateOAuthAuthorizationServerMetadata.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
		eventLog.endBlock();

		eventLog.startBlock("Validate OAuth Client metadata for %s".formatted(entity));
		callAndContinueOnFailure(ValidateOAuthClientMetadata.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
		eventLog.endBlock();

		eventLog.startBlock("Validate OAuth Protected Resource metadata for %s".formatted(entity));
		callAndContinueOnFailure(ValidateOAuthProtectedResourceMetadata.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
		eventLog.endBlock();
	}

	protected void validateEntityStatementResponse() {
		env.mapKey("discovery_endpoint_response", "entity_statement_endpoint_response");
		call(sequence(ValidateEntityStatementResponseSequence.class));
		env.unmapKey("discovery_endpoint_response");
		env.removeObject("entity_statement_endpoint_response");
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

	protected void validateSuperiors() {
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
				String authorityHintUrl = authorityHint + ".well-known/openid-federation";

				// Get the entity statement for the Superior
				env.putString("entity_statement_url", authorityHintUrl);
				callAndStopOnFailure(GetEntityStatement.class, Condition.ConditionResult.FAILURE);
				validateEntityStatementResponse();
				validateEntityStatement();

				eventLog.startBlock("Validating subordinate statement by immediate superior %s".formatted(authorityHint));

				// Verify that the primary entity is present in the list endpoint result
				callAndContinueOnFailure(ExtractFederationListEndpoint.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
				callAndContinueOnFailure(GetSubordinateListingResponse.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
				env.mapKey("endpoint_response", "federation_list_endpoint_response");
				callAndContinueOnFailure(EnsureHttpStatusCodeIs200.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
				callAndContinueOnFailure(EnsureContentTypeJson.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
				env.unmapKey("endpoint_response");
				callAndContinueOnFailure(VerifyPrimaryEntityPresenceInSubordinateListing.class, Condition.ConditionResult.FAILURE, "OIDFED-?");

				// Get the entity statement from the Superior's fetch endpoint
				callAndContinueOnFailure(ExtractFederationFetchEndpoint.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
				env.putString("entity_statement_url", env.getString("federation_fetch_endpoint"));
				callAndContinueOnFailure(GetEntityStatement.class, Condition.ConditionResult.FAILURE, "OIDFED-?");

				callAndContinueOnFailure(ValidateEntityStatementEndpointReturnedCorrectContentType.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
				env.mapKey("server_jwks", "federation_fetch_endpoint_jkws");
				call(sequence(ValidateEntityStatementSignatureSequence.class));
				env.unmapKey("server_jwks");

				callAndContinueOnFailure(ValidateEntityStatementIat.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
				callAndContinueOnFailure(ValidateEntityStatementExp.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
				env.putString("entity_statement_url", env.getString("federation_fetch_endpoint_iss"));
				callAndContinueOnFailure(ValidateEntityStatementIss.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
				env.putString("entity_statement_url", env.getString("primary_entity_statement_sub"));
				callAndContinueOnFailure(ValidateEntityStatementSub.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
				callAndContinueOnFailure(ValidateEntityStatementMetadata.class, Condition.ConditionResult.FAILURE, "OIDFED-?");

				callAndContinueOnFailure(ValidateAbsenceOfAuthorityHints.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
				callAndContinueOnFailure(ValidateAbsenceOfFederationEntityMetadata.class, Condition.ConditionResult.FAILURE, "OIDFED-?");

				callAndContinueOnFailure(ValidateEntityStatementMetadataPolicy.class, Condition.ConditionResult.FAILURE, "OIDFED-?");

				eventLog.endBlock();
			}
		}
	}
}
