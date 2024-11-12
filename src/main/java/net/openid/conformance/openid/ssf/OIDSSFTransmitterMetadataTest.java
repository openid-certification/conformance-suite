package net.openid.conformance.openid.ssf;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CheckJwksUri;
import net.openid.conformance.condition.client.FetchServerKeys;
import net.openid.conformance.condition.client.ValidateServerJWKs;
import net.openid.conformance.openid.ssf.conditions.metadata.OIDSSFCheckTransmitterMetadataIssuer;
import net.openid.conformance.openid.ssf.conditions.metadata.OIDSSFAuthorizationSchemesTransmitterMetadataCheck;
import net.openid.conformance.openid.ssf.conditions.metadata.OIDSSFDefaultSubjectsTransmitterMetadataCheck;
import net.openid.conformance.openid.ssf.conditions.metadata.OIDSSFEnsureHttpsUrlsTransmitterMetadataCheck;
import net.openid.conformance.openid.ssf.conditions.metadata.OIDSSFRequiredFieldsTransmitterMetadataCheck;
import net.openid.conformance.openid.ssf.conditions.metadata.OIDSSFSpecVersionTransmitterMetadataCheck;
import net.openid.conformance.openid.ssf.variant.SsfDeliveryMode;
import net.openid.conformance.openid.ssf.variant.SsfServerMetadata;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ServerMetadata;
import net.openid.conformance.variant.VariantConfigurationFields;
import net.openid.conformance.variant.VariantParameters;

@PublishTestModule(testName = "openid-ssf-transmitter-metadata", displayName = "OpenID Shared Signals Framework: Validate Transmitter Metadata", summary = "This test verifies the behavior of the transmitter metadata.", profile = "OIDSSF", configurationFields = {

})
@VariantParameters({ServerMetadata.class, SsfDeliveryMode.class,})
@VariantConfigurationFields(parameter = SsfServerMetadata.class, value = "static", configurationFields = {"ssf.transmitter.configuration_metadata_endpoint",})
@VariantConfigurationFields(parameter = SsfServerMetadata.class, value = "discovery", configurationFields = {"ssf.transmitter.issuer", "ssf.transmitter.metadata_suffix",})
public class OIDSSFTransmitterMetadataTest extends AbstractOIDSSFTest {

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

		fireTestFinished();
	}

	private void validateTransmitterMetadata() {


		callAndContinueOnFailure(OIDSSFCheckTransmitterMetadataIssuer.class, Condition.ConditionResult.WARNING, "OIDSSF-6.2");
		callAndStopOnFailure(OIDSSFEnsureHttpsUrlsTransmitterMetadataCheck.class, "OIDSSF-6.1", "CAEPIOP-2.3.7");
		callAndContinueOnFailure(OIDSSFSpecVersionTransmitterMetadataCheck.class, Condition.ConditionResult.WARNING, "CAEPIOP-2.3.1");
		callAndStopOnFailure(OIDSSFRequiredFieldsTransmitterMetadataCheck.class, "OIDSSF-6.1");
		callAndContinueOnFailure(OIDSSFDefaultSubjectsTransmitterMetadataCheck.class, Condition.ConditionResult.WARNING, "OIDSSF-6.1");
		callAndContinueOnFailure(OIDSSFAuthorizationSchemesTransmitterMetadataCheck.class, Condition.ConditionResult.WARNING, "OIDSSF-6.1.1", "CAEPIOP-2.3.7");

		// treat transmitter_metadata as "server" metadata to leverage existing checks
		env.mapKey("server", "transmitter_metadata");

		callAndStopOnFailure(CheckJwksUri.class);
		callAndStopOnFailure(FetchServerKeys.class);
		callAndStopOnFailure(ValidateServerJWKs.class, "RFC7517-1.1");
	}
}
