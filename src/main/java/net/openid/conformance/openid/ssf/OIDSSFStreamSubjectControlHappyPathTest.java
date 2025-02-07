package net.openid.conformance.openid.ssf;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs200;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs201;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs204;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFCheckTransmitterMetadataIssuerMatchesIssuerInResponse;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFCreateStreamConditionSequence;
import net.openid.conformance.openid.ssf.conditions.subjects.OIDSSFAddSubjectToStreamConfigCall;
import net.openid.conformance.openid.ssf.conditions.subjects.OIDSSFRemoveSubjectToStreamConfigCall;
import net.openid.conformance.openid.ssf.variant.SsfAuthMode;
import net.openid.conformance.openid.ssf.variant.SsfDeliveryMode;
import net.openid.conformance.openid.ssf.variant.SsfServerMetadata;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.VariantConfigurationFields;
import net.openid.conformance.variant.VariantParameters;

@PublishTestModule(
	testName = "openid-ssf-stream-subject-control",
	displayName = "OpenID Shared Signals Framework: Validate Subject Control",
	summary = "This test verifies the behavior of the subject control.",
	profile = "OIDSSF",
	configurationFields = {
		"ssf.transmitter.issuer",
		"ssf.transmitter.metadata_suffix", // see: https://openid.net/specs/openid-sharedsignals-framework-1_0.html#section-6.2.1
	}
)
@VariantParameters({
	SsfServerMetadata.class,
	SsfAuthMode.class,
	SsfDeliveryMode.class,
})
@VariantConfigurationFields(parameter = SsfServerMetadata.class, value="static", configurationFields = {
	"ssf.transmitter.configuration_metadata_endpoint",
})
@VariantConfigurationFields(parameter = SsfServerMetadata.class, value="discovery", configurationFields = {
	"ssf.transmitter.issuer",
	"ssf.transmitter.metadata_suffix",
})
@VariantConfigurationFields(parameter = SsfAuthMode.class, value = "static", configurationFields = {
	"ssf.transmitter.access_token"
})
@VariantConfigurationFields(parameter = SsfAuthMode.class, value = "dynamic", configurationFields = {
})
public class OIDSSFStreamSubjectControlHappyPathTest extends AbstractOIDSSFTransmitterTestModule {

	@Override
	public void start() {

		setStatus(Status.RUNNING);

		eventLog.runBlock("Fetch Transmitter Metadata", () -> {
			fetchTransmitterMetadata();

			String addSubjectEndpoint = env.getString("ssf", "transmitter_metadata.add_subject_endpoint");
			String removeSubjectEndpoint = env.getString("ssf", "transmitter_metadata.remove_subject_endpoint");

			if (addSubjectEndpoint == null) {
				fireTestSkipped("This transmitter does not support the 'add_subject_endpoint'.");
			}

			if (removeSubjectEndpoint == null) {
				fireTestSkipped("This transmitter does not support the 'remove_subject_endpoint'.");
			}
		});

		eventLog.runBlock("Validate TLS Connection", this::validateTlsConnection);

		// see https://openid.net/specs/openid-caep-interoperability-profile-1_0-ID1.html
		// OID_CAEP_INTEROP https://openid.net/specs/openid-caep-interoperability-profile-1_0-ID1.html
		eventLog.runBlock("Prepare Transmitter Access Token", this::obtainTransmitterAccessToken);

		eventLog.runBlock("Clean stream environment if necessary", this::cleanUpStreamConfigurationIfNecessary);

		// ensure stream exists
		eventLog.runBlock("Create Stream Configuration", () -> {
			SsfDeliveryMode deliveryMode = getVariant(SsfDeliveryMode.class);
			env.putString("ssf", "delivery_method", deliveryMode.getAlias());

			call(sequence(OIDSSFCreateStreamConditionSequence.class));
			call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
			callAndContinueOnFailure(EnsureHttpStatusCodeIs201.class, Condition.ConditionResult.WARNING, "OIDSSF-7.1.1.1");
			callAndContinueOnFailure(OIDSSFCheckTransmitterMetadataIssuerMatchesIssuerInResponse.class, Condition.ConditionResult.WARNING, "OIDSSF-7.1.1.1");
			call(exec().unmapKey("endpoint_response"));
		});

		// add subject with subjectid formats:
		// - email
		// - iss_sub
		// - opaque (for the Verification event only)
		eventLog.runBlock("Add Subject to Stream Configuration", () -> {
			callAndStopOnFailure(OIDSSFAddSubjectToStreamConfigCall.class, "OIDSSF-7.1.3.1");
			call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
			callAndContinueOnFailure(EnsureHttpStatusCodeIs200.class, Condition.ConditionResult.WARNING, "OIDSSF-7.1.3.1");
			call(exec().unmapKey("endpoint_response"));
		});

		// remove subject(s)
		eventLog.runBlock("Remove Subject from Stream Configuration", () -> {
			callAndStopOnFailure(OIDSSFRemoveSubjectToStreamConfigCall.class, "OIDSSF-7.1.3.2");
			call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
			callAndContinueOnFailure(EnsureHttpStatusCodeIs204.class, Condition.ConditionResult.WARNING, "OIDSSF-7.1.3.2");
			call(exec().unmapKey("endpoint_response"));
		});

		fireTestFinished();
	}

}
