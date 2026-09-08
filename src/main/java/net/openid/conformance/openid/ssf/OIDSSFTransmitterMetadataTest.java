package net.openid.conformance.openid.ssf;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CheckJwksUri;
import net.openid.conformance.condition.client.FetchServerKeys;
import net.openid.conformance.sequence.ValidateJwksSequence;
import net.openid.conformance.openid.ssf.conditions.metadata.OIDSSFAuthorizationSchemesTransmitterMetadataCheck;
import net.openid.conformance.openid.ssf.conditions.metadata.OIDSSFCaepInteropAuthorizationSchemesTransmitterMetadataCheck;
import net.openid.conformance.openid.ssf.conditions.metadata.OIDSSFCaepInteropDeliveryMethodsTransmitterMetadataCheck;
import net.openid.conformance.openid.ssf.conditions.metadata.OIDSSFCheckRequiredFieldConfigurationEndpoint;
import net.openid.conformance.openid.ssf.conditions.metadata.OIDSSFCheckRequiredFieldJwksUri;
import net.openid.conformance.openid.ssf.conditions.metadata.OIDSSFCheckRequiredFieldStatusEndpoint;
import net.openid.conformance.openid.ssf.conditions.metadata.OIDSSFCheckRequiredFieldVerificationEndpoint;
import net.openid.conformance.openid.ssf.conditions.metadata.OIDSSFCheckSupportedDeliveryMethods;
import net.openid.conformance.openid.ssf.conditions.metadata.OIDSSFCheckTransmitterMetadataIssuer;
import net.openid.conformance.openid.ssf.conditions.metadata.OIDSSFDefaultSubjectsTransmitterMetadataCheck;
import net.openid.conformance.openid.ssf.conditions.metadata.OIDSSFEnsureDeliveryMethodIsSupported;
import net.openid.conformance.openid.ssf.conditions.metadata.OIDSSFEnsureHttpsUrlsTransmitterMetadataCheck;
import net.openid.conformance.openid.ssf.conditions.metadata.OIDSSFEnsureNonEmptyArrayClaimsCheck;
import net.openid.conformance.openid.ssf.conditions.metadata.OIDSSFOptionalFieldsTransmitterMetadataCheck;
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
	summary = """
		This test verifies the transmitter metadata document.
		The testsuite expects to observe the following interactions:
		 * fetch the transmitter configuration metadata
		 * validate required fields are present
		 * validate advertised delivery methods
		 * validate advertised supported event types
		""",
	profile = "OIDSSF"
)
@VariantParameters({ServerMetadata.class, SsfDeliveryMode.class,})
@VariantConfigurationFields(parameter = SsfServerMetadata.class, value = "static", configurationFields = {"ssf.transmitter.configuration_metadata_endpoint",})
@VariantConfigurationFields(parameter = SsfServerMetadata.class, value = "discovery", configurationFields = {"ssf.transmitter.issuer", "ssf.transmitter.metadata_suffix",})
public class OIDSSFTransmitterMetadataTest extends AbstractOIDSSFTransmitterTestModule {

	@Override
	protected void checkDeliveryMethod() {
		// skip delivery method check for metadata validation
	}

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

		if (getVariant(SsfServerMetadata.class) == SsfServerMetadata.DISCOVERY) {
			callAndContinueOnFailure(OIDSSFCheckTransmitterMetadataIssuer.class, Condition.ConditionResult.FAILURE, "OIDSSF-7.2");
		} else {
			// ssf_server_metadata=static: the metadata is fetched from a configured URL,
			// not derived from an issuer - there is no expected issuer to compare against
			// (the 'SSF Issuer' config field only exists under the discovery variant).
			eventLog.log(getName(), "Skipping transmitter metadata issuer check: not applicable for static transmitter metadata");
		}
		callAndStopOnFailure(OIDSSFEnsureHttpsUrlsTransmitterMetadataCheck.class, "OIDSSF-7.1", "CAEPIOP-2.1");
		callAndStopOnFailure(OIDSSFRequiredFieldsTransmitterMetadataCheck.class, "OIDSSF-7.1");
		callAndContinueOnFailure(OIDSSFOptionalFieldsTransmitterMetadataCheck.class, Condition.ConditionResult.INFO, "OIDSSF-7.1");
		callAndContinueOnFailure(OIDSSFDefaultSubjectsTransmitterMetadataCheck.class, Condition.ConditionResult.WARNING, "OIDSSF-7.1");
		callAndContinueOnFailure(OIDSSFAuthorizationSchemesTransmitterMetadataCheck.class, Condition.ConditionResult.INFO, "OIDSSF-7.1.1");
		if (isSsfProfileEnabled(SsfProfile.CAEP_INTEROP)) {
			callAndContinueOnFailure(OIDSSFCaepInteropAuthorizationSchemesTransmitterMetadataCheck.class, Condition.ConditionResult.FAILURE, "OIDSSF-7.1.1", "CAEPIOP-2.3.7");
		}
		callAndContinueOnFailure(OIDSSFCheckSupportedDeliveryMethods.class, Condition.ConditionResult.WARNING, "OIDSSF-7.1", "OIDSSF-8.1.1");

		if (isSsfProfileEnabled(SsfProfile.CAEP_INTEROP)) {
			callAndContinueOnFailure(OIDSSFCaepInteropDeliveryMethodsTransmitterMetadataCheck.class, Condition.ConditionResult.FAILURE, "CAEPIOP-2.3.2");
			// The certification run is scheduled for one delivery mode, so require exactly
			// that mode to be advertised. Whether 2.3.8.1 obliges transmitters to support
			// BOTH standard methods is ambiguous (2.4.1 explicitly requires receivers to
			// support only one) - certification is granted per delivery mode either way.
			callAndContinueOnFailure(new OIDSSFEnsureDeliveryMethodIsSupported(deliveryMode), Condition.ConditionResult.FAILURE, "CAEPIOP-2.3.2", "CAEPIOP-2.3.8.1");
			callAndContinueOnFailure(OIDSSFSpecVersionTransmitterMetadataCheck.class, Condition.ConditionResult.FAILURE, "CAEPIOP-2.3.1");
			callAndContinueOnFailure(OIDSSFCheckRequiredFieldJwksUri.class, Condition.ConditionResult.FAILURE, "CAEPIOP-2.3.3");
			callAndContinueOnFailure(OIDSSFCheckRequiredFieldConfigurationEndpoint.class, Condition.ConditionResult.FAILURE, "CAEPIOP-2.3.4");
			callAndContinueOnFailure(OIDSSFCheckRequiredFieldStatusEndpoint.class, Condition.ConditionResult.FAILURE, "CAEPIOP-2.3.5");
			callAndContinueOnFailure(OIDSSFCheckRequiredFieldVerificationEndpoint.class, Condition.ConditionResult.FAILURE, "CAEPIOP-2.3.6");
		}

		callAndContinueOnFailure(OIDSSFEnsureNonEmptyArrayClaimsCheck.class, Condition.ConditionResult.FAILURE, "OIDSSF-7.2.3");

		// Workaround because we cannot use env.mapKey("server","ssf.transmitter_metadata")
		JsonObject transmitterMetadata = env.getElementFromObject("ssf", "transmitter_metadata").getAsJsonObject();
		env.putObject("transmitter_metadata", transmitterMetadata);

		if (transmitterMetadata.has("jwks_uri")) {
			// treat transmitter_metadata as "server" metadata to leverage existing checks
			env.mapKey("server", "transmitter_metadata");
			try {
				callAndStopOnFailure(CheckJwksUri.class);
				callAndStopOnFailure(FetchServerKeys.class);
				call(new ValidateJwksSequence("server_jwks", null, "transmitter JWKS", "RFC7517-1.1"));
			} finally {
				env.removeObject("transmitter_metadata");
				env.unmapKey("server");
			}
		}
	}
}
