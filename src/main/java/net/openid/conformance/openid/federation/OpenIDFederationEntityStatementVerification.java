package net.openid.conformance.openid.federation;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.EnsureDiscoveryEndpointResponseStatusCodeIs200;
import net.openid.conformance.testmodule.AbstractTestModule;
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
public class OpenIDFederationEntityStatementVerification extends AbstractTestModule {

	@Override
	public void configure(JsonObject config, String baseUrl, String externalUrlOverride, String baseMtlsUrl) {
		env.putString("base_url", baseUrl);
		env.putString("base_mtls_url", baseMtlsUrl);
		env.putObject("config", config);

		eventLog.startBlock("Fetch Entity Statement");
		if (ServerMetadata.STATIC.equals(getVariant(ServerMetadata.class))) {
			// This case is actually not valid, I believe, but it's here for testing purposes atm
			callAndStopOnFailure(GetStaticEntityStatement.class, Condition.ConditionResult.FAILURE);
		} else {
			callAndStopOnFailure(GetEntityStatement.class, Condition.ConditionResult.FAILURE);
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
		fireTestFinished();
	}

	protected void validateEntityStatement() {
		eventLog.startBlock("Validate basic claims in Entity Statement");
		call(sequence(ValidateEntityStatementBasicClaimsSequence.class));
		eventLog.endBlock();

		eventLog.startBlock("Validate JWKs and signature in Entity Statement");
		call(sequence(ValidateEntityStatementSignatureSequence.class));
		eventLog.endBlock();

		eventLog.startBlock("Validate metadata in Entity Statement");
		callAndContinueOnFailure(ValidateEntityStatementMetadataClaim.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
		eventLog.endBlock();

		eventLog.startBlock("Validate Federation Entity metadata");
		callAndContinueOnFailure(ValidateFederationEntityMetadata.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
		eventLog.endBlock();

		eventLog.startBlock("Validate OpenID Relying Party metadata");
		callAndContinueOnFailure(ExtractOpenIDRelyingPartyMetadata.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
		validateOpenIdRelyingPartyMetadata();
		eventLog.endBlock();

		eventLog.startBlock("Validate OpenID Provider metadata");
		callAndContinueOnFailure(ExtractOpenIDProviderMetadata.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
		validateOpenIdProviderMetadata();
		eventLog.endBlock();

		eventLog.startBlock("Validate OAuth Authorization Server metadata");
		callAndContinueOnFailure(ValidateOAuthAuthorizationServerMetadata.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
		eventLog.endBlock();

		eventLog.startBlock("Validate OAuth Client metadata");
		callAndContinueOnFailure(ValidateOAuthClientMetadata.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
		eventLog.endBlock();

		eventLog.startBlock("Validate OAuth Protected Resource metadata");
		callAndContinueOnFailure(ValidateOAuthProtectedResourceMetadata.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
		eventLog.endBlock();

		eventLog.startBlock("Validate authority hints in Entity Statement");
		skipIfElementMissing("entity_statement_body", "authority_hints", Condition.ConditionResult.INFO,
			ValidateAuthorityHints.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
		eventLog.endBlock();
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
			call(sequence(ValidateClientRegistrationMetadataSequence.class));
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

}
