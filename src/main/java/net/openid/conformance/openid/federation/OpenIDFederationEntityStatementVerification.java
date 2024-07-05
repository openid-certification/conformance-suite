package net.openid.conformance.openid.federation;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.EnsureDiscoveryEndpointResponseStatusCodeIs200;
import net.openid.conformance.condition.client.ValidateServerJWKs;
import net.openid.conformance.testmodule.AbstractTestModule;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ClientRegistration;
import net.openid.conformance.variant.ServerMetadata;
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
@VariantNotApplicable(parameter = ServerMetadata.class, values = { "static"} )
public class OpenIDFederationEntityStatementVerification extends AbstractTestModule {

	@Override
	public void configure(JsonObject config, String baseUrl, String externalUrlOverride, String baseMtlsUrl) {
		env.putString("base_url", baseUrl);
		env.putString("base_mtls_url", baseMtlsUrl);
		env.putObject("config", config);

		callAndStopOnFailure(GetEntityStatement.class);

		env.mapKey("discovery_endpoint_response", "entity_statement_endpoint_response");
		callAndContinueOnFailure(EnsureDiscoveryEndpointResponseStatusCodeIs200.class, Condition.ConditionResult.FAILURE, "OIDFED-3");
		env.unmapKey("discovery_endpoint_response");

		callAndContinueOnFailure(ValidateEntityStatementEndpointReturnedCorrectContentType.class, Condition.ConditionResult.FAILURE, "OIDFED-3");

		setStatus(Status.CONFIGURED);
		fireSetupDone();
	}

	@Override
	public void start() {
		setStatus(Status.RUNNING);
		performEndpointVerification();
		fireTestFinished();
	}

	protected void performEndpointVerification() {
		callAndContinueOnFailure(ValidateEntityStatementIss.class, Condition.ConditionResult.FAILURE, "OIDFED-?"); // Spec doesn't explicitly say so?
		callAndContinueOnFailure(ValidateEntityStatementSub.class, Condition.ConditionResult.FAILURE, "OIDFED-?"); // Spec doesn't explicitly say so?
		callAndContinueOnFailure(ValidateEntityStatementIat.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
		callAndContinueOnFailure(ValidateEntityStatementExp.class, Condition.ConditionResult.FAILURE, "OIDFED-?");

		callAndContinueOnFailure(ExtractJWKsFromEntityStatement.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
		callAndContinueOnFailure(ValidateServerJWKs.class, Condition.ConditionResult.FAILURE, "OIDFED-?");

		callAndContinueOnFailure(ValidateAndExtractEntityStatementMetadataClaim.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
		callAndContinueOnFailure(ValidateFederationEntityMetadata.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
	}

}
