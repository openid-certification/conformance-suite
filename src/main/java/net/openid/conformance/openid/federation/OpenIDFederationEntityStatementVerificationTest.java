package net.openid.conformance.openid.federation;

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.testmodule.AbstractTestModule;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ClientRegistration;
import net.openid.conformance.variant.ServerMetadata;
import net.openid.conformance.variant.VariantConfigurationFields;
import net.openid.conformance.variant.VariantNotApplicable;
import net.openid.conformance.variant.VariantParameters;

import java.util.Set;

@PublishTestModule(
	testName = "openid-federation-entity-configuration-endpoint-verification",
	displayName = "OpenID Federation: Entity Configuration Endpoint Verification",
	summary = "This test ensures that the server's entity configuration metadata is according to the specifications",
	profile = "OIDFED",
	configurationFields = {
		"server.entityStatementUrl",
	}
)
@VariantParameters({
	ServerMetadata.class,
	ClientRegistration.class
})
@VariantConfigurationFields(parameter = ServerMetadata.class, value = "static", configurationFields = {
	"server.entityStatement"
})
@VariantNotApplicable(parameter = ClientRegistration.class, values={ "static_client" })
public class OpenIDFederationEntityStatementVerificationTest extends AbstractTestModule {

	private static Set<String> AUTHORITY_HINT_IGNORE_LIST = ImmutableSet.of();

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
			env.unmapKey("entity_statement_url");
		}
		eventLog.endBlock();

		setStatus(Status.CONFIGURED);
		fireSetupDone();
	}

	@Override
	public void start() {
		setStatus(Status.RUNNING);

		validateEntityStatement();

		fireTestFinished();
	}

	protected void validateEntityStatement() {
		String entity = env.getString("entity_statement_url");

		eventLog.startBlock("Validate basic claims in Entity Statement for %s".formatted(entity));
		call(sequence(ValidateEntityStatementBasicClaimsSequence.class));
		eventLog.endBlock();

		eventLog.startBlock("Validate JWKs and signature in Entity Statement for %s".formatted(entity));
		call(sequence(ValidateEntityStatementSignatureSequence.class));
		eventLog.endBlock();

		eventLog.startBlock("Validate metadata in Entity Statement for %s".formatted(entity));
		callAndContinueOnFailure(ValidateEntityStatementMetadataClaim.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
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

		// eventLog.startBlock("Validate authority hints in Entity Statement for %s".formatted(entity));
		skipIfElementMissing("entity_statement_body", "authority_hints", Condition.ConditionResult.INFO,
			ValidateAuthorityHints.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
		validateAuthorityHints();
		// eventLog.endBlock();
	}

	private void validateEntityStatementResponse() {
		env.mapKey("discovery_endpoint_response", "entity_statement_endpoint_response");
		call(sequence(ValidateEntityStatementResponseSequence.class));
		env.unmapKey("discovery_endpoint_response");
		env.removeObject("entity_statement_endpoint_response");
	}

	private void validateOpenIdRelyingPartyMetadata() {
		if (env.containsObject("openid_relying_party_metadata")) {
			env.mapKey("client", "openid_relying_party_metadata");
			call(sequence(ValidateOpenIDRelyingPartyMetadataSequence.class));
			callAndContinueOnFailure(ValidateOpenIDRelyingPartyMetadata.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
			env.unmapKey("client");
			env.removeObject("openid_relying_party_metadata");
		}
	}

	private void validateOpenIdProviderMetadata() {
		if (env.containsObject("openid_provider_metadata")) {
			env.mapKey("server", "openid_provider_metadata");
			call(new ValidateDiscoveryMetadataSequence(getVariant(ClientRegistration.class)));
			callAndContinueOnFailure(ValidateOpenIDProviderMetadata.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
			env.unmapKey("server");
			env.removeObject("openid_provider_metadata");
		}
	}

	private void validateAuthorityHints() {
		JsonElement authorityHintsElement = env.getElementFromObject("entity_statement_body", "authority_hints");
		if (authorityHintsElement != null) {
			JsonArray authorityHints = authorityHintsElement.getAsJsonArray();
			for (JsonElement authorityHintElement : authorityHints) {
				String authorityHint = OIDFJSON.getString(authorityHintElement);
				String authorityHintUrl = authorityHint + ".well-known/openid-federation";

				eventLog.startBlock("Validating immediate superior %s".formatted(authorityHint));

				// Get the entity statement for the Superior
				env.putString("entity_statement_url", authorityHintUrl);
				callAndStopOnFailure(GetEntityStatement.class, Condition.ConditionResult.FAILURE);
				validateEntityStatementResponse();
				validateEntityStatement();

				// Get the entity statement from the Superior's fetch endpoint
				callAndContinueOnFailure(ExtractFederationFetchEndpoint.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
				env.putString("entity_statement_url", env.getString("federation_fetch_endpoint"));
				callAndContinueOnFailure(GetEntityStatement.class, Condition.ConditionResult.FAILURE, "OIDFED-?");

				callAndContinueOnFailure(ValidateEntityStatementEndpointReturnedCorrectContentType.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
				call(sequence(ValidateEntityStatementSignatureSequence.class));

				callAndContinueOnFailure(ValidateEntityStatementIat.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
				callAndContinueOnFailure(ValidateEntityStatementExp.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
				callAndContinueOnFailure(ValidateEntityStatementIss.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
				env.putString("entity_statement_url", OIDFJSON.getString(env.getElementFromObject("primary_entity_statement_body", "sub")));
				callAndContinueOnFailure(ValidateEntityStatementSub.class, Condition.ConditionResult.FAILURE, "OIDFED-?");

				eventLog.endBlock();

			}
		}
	}
}
