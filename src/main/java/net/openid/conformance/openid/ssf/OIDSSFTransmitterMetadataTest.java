package net.openid.conformance.openid.ssf;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CheckJwksUri;
import net.openid.conformance.condition.client.DeriveOauthProtectedResourceMetadataUri;
import net.openid.conformance.condition.client.FetchOauthProtectedResourceMetadata;
import net.openid.conformance.condition.client.FetchServerKeys;
import net.openid.conformance.condition.client.ValidateServerJWKs;
import net.openid.conformance.openid.ssf.conditions.metadata.OIDSSFAuthorizationSchemesTransmitterMetadataCheck;
import net.openid.conformance.openid.ssf.conditions.metadata.OIDSSFCheckRequiredFieldConfigurationEndpoint;
import net.openid.conformance.openid.ssf.conditions.metadata.OIDSSFCheckRequiredFieldJwksUri;
import net.openid.conformance.openid.ssf.conditions.metadata.OIDSSFCheckRequiredFieldStatusEndpoint;
import net.openid.conformance.openid.ssf.conditions.metadata.OIDSSFCheckRequiredFieldVerificationEndpoint;
import net.openid.conformance.openid.ssf.conditions.metadata.OIDSSFCheckScopesWithOauthProtectedResourceMetadata;
import net.openid.conformance.openid.ssf.conditions.metadata.OIDSSFCheckSupportedDeliveryMethods;
import net.openid.conformance.openid.ssf.conditions.metadata.OIDSSFCheckTransmitterMetadataIssuer;
import net.openid.conformance.openid.ssf.conditions.metadata.OIDSSFDefaultSubjectsTransmitterMetadataCheck;
import net.openid.conformance.openid.ssf.conditions.metadata.OIDSSFEnsureHttpsUrlsTransmitterMetadataCheck;
import net.openid.conformance.openid.ssf.conditions.metadata.OIDSSFRequiredFieldsTransmitterMetadataCheck;
import net.openid.conformance.openid.ssf.conditions.metadata.OIDSSFSpecVersionTransmitterMetadataCheck;
import net.openid.conformance.openid.ssf.variant.SsfDeliveryMode;
import net.openid.conformance.openid.ssf.variant.SsfProfile;
import net.openid.conformance.openid.ssf.variant.SsfServerMetadata;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ServerMetadata;
import net.openid.conformance.variant.VariantConfigurationFields;
import net.openid.conformance.variant.VariantParameters;

@PublishTestModule(
	testName = "openid-ssf-transmitter-metadata",
	displayName = "OpenID Shared Signals Framework: Validate Transmitter Metadata",
	summary = "This test verifies the information of the transmitter metadata.",
	profile = "OIDSSF"
)
@VariantParameters({ServerMetadata.class, SsfDeliveryMode.class,})
@VariantConfigurationFields(parameter = SsfServerMetadata.class, value = "static", configurationFields = {"ssf.transmitter.configuration_metadata_endpoint",})
@VariantConfigurationFields(parameter = SsfServerMetadata.class, value = "discovery", configurationFields = {"ssf.transmitter.issuer", "ssf.transmitter.metadata_suffix",})
public class OIDSSFTransmitterMetadataTest extends AbstractOIDSSFTransmitterTestModule {

	@Override
	public void start() {

		setStatus(Status.RUNNING);

		eventLog.runBlock("Fetch Transmitter Metadata", () -> {
			fetchTransmitterMetadata();
		});

		eventLog.runBlock("Validate TLS Connection", () -> {
			validateTlsConnection();
		});

		eventLog.runBlock("Validate Transmitter Metadata", () -> {
			validateTransmitterMetadata();
		});

		eventLog.runBlock("Fetch OAuth Protected Resource Metadata", () -> {
			// TODO fetch OAuth protected resource metadata
			// https://ssf.caep.dev/.well-known/oauth-protected-resource
			callAndContinueOnFailure(DeriveOauthProtectedResourceMetadataUri.class, Condition.ConditionResult.INFO, "CAEPIOP-2.7.3");
			callAndContinueOnFailure(FetchOauthProtectedResourceMetadata.class, Condition.ConditionResult.INFO, "CAEPIOP-2.7.3");
		});

		eventLog.runBlock("Validate OAuth Protected Resource Metadata", () -> {
			callAndContinueOnFailure(OIDSSFCheckScopesWithOauthProtectedResourceMetadata.class, Condition.ConditionResult.WARNING, "CAEPIOP-2.7.2.1");
		});

		fireTestFinished();
	}

	private void validateTransmitterMetadata() {

		callAndContinueOnFailure(OIDSSFCheckTransmitterMetadataIssuer.class, Condition.ConditionResult.WARNING, "OIDSSF-6.2");
		callAndStopOnFailure(OIDSSFEnsureHttpsUrlsTransmitterMetadataCheck.class, "OIDSSF-6.1", "CAEPIOP-2.3.7");
		callAndStopOnFailure(OIDSSFRequiredFieldsTransmitterMetadataCheck.class, "OIDSSF-6.1");
		callAndContinueOnFailure(OIDSSFDefaultSubjectsTransmitterMetadataCheck.class, Condition.ConditionResult.WARNING, "OIDSSF-6.1");
		callAndContinueOnFailure(OIDSSFAuthorizationSchemesTransmitterMetadataCheck.class,
				isSsfProfileEnabled(SsfProfile.CAEP_INTEROP)
						? Condition.ConditionResult.FAILURE
						: Condition.ConditionResult.WARNING
				, "OIDSSF-6.1.1", "CAEPIOP-2.3.7");
		callAndContinueOnFailure(OIDSSFCheckSupportedDeliveryMethods.class, Condition.ConditionResult.WARNING, "OIDSSF-6.1", "OIDSSF-7.1.1");

		if (isSsfProfileEnabled(SsfProfile.CAEP_INTEROP)) {
			callAndContinueOnFailure(OIDSSFSpecVersionTransmitterMetadataCheck.class, Condition.ConditionResult.WARNING, "CAEPIOP-2.3.1");
			callAndContinueOnFailure(OIDSSFCheckRequiredFieldJwksUri.class, Condition.ConditionResult.FAILURE, "CAEPIOP-2.3.3");
			callAndContinueOnFailure(OIDSSFCheckRequiredFieldConfigurationEndpoint.class, Condition.ConditionResult.FAILURE, "CAEPIOP-2.3.4");
			callAndContinueOnFailure(OIDSSFCheckRequiredFieldStatusEndpoint.class, Condition.ConditionResult.FAILURE, "CAEPIOP-2.3.5");
			callAndContinueOnFailure(OIDSSFCheckRequiredFieldVerificationEndpoint.class, Condition.ConditionResult.FAILURE, "CAEPIOP-2.3.6");
		}

		// Workaround because we cannot use env.mapKey("server","ssf.transmitter_metadata")
		JsonObject transmitterMetadata = env.getElementFromObject("ssf", "transmitter_metadata").getAsJsonObject();
		env.putObject("transmitter_metadata", transmitterMetadata);

		if (transmitterMetadata.has("jwks_uri")) {
			// treat transmitter_metadata as "server" metadata to leverage existing checks
			env.mapKey("server", "transmitter_metadata");
			try {
				callAndStopOnFailure(CheckJwksUri.class);
				callAndStopOnFailure(FetchServerKeys.class);
				callAndStopOnFailure(ValidateServerJWKs.class, "RFC7517-1.1");
			} finally {
				env.removeObject("transmitter_metadata");
				env.unmapKey("server");
			}
		}
	}
}
