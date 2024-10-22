package net.openid.conformance.openid.ssf;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs200;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs201;
import net.openid.conformance.openid.ssf.conditions.OIDSSFObtainTransmitterAccessToken;
import net.openid.conformance.openid.ssf.conditions.streams.CheckTransmitterMetadataIssuerMatchesIssuerInResponse;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFCreateStreamConfigCall;
import net.openid.conformance.openid.ssf.conditions.subjects.OIDSSFAddSubjectToStreamConfigCall;
import net.openid.conformance.openid.ssf.conditions.subjects.OIDSSFRemoveSubjectToStreamConfigCall;
import net.openid.conformance.openid.ssf.variant.SsfDeliveryMode;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ServerMetadata;
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
		"ssf.transmitter.access_token"
	}
)
@VariantParameters({
	ServerMetadata.class,
	SsfDeliveryMode.class,
})
@VariantConfigurationFields(parameter = ServerMetadata.class, value="static", configurationFields = {
	"ssf.transmitter.configuration_metadata_endpoint",
})
@VariantConfigurationFields(parameter = ServerMetadata.class, value="discovery", configurationFields = {
	"ssf.transmitter.issuer",
	"ssf.transmitter.metadata_suffix",
})
public class OIDSSFStreamSubjectControlHappyPathTest extends AbstractOIDSSFTest {

	@Override
	public void start() {

		setStatus(Status.RUNNING);

		eventLog.runBlock("Fetch Transmitter Metadata", () -> {
			fetchTransmitterMetadata();

			String addSubjectEndpoint = env.getString("transmitter_metadata", "add_subject_endpoint");
			String removeSubjectEndpoint = env.getString("transmitter_metadata", "remove_subject_endpoint");

			if (addSubjectEndpoint == null) {
				fireTestSkipped("This transmitter does not support the 'add_subject_endpoint'.");
			}

			if (removeSubjectEndpoint == null) {
				fireTestSkipped("This transmitter does not support the 'remove_subject_endpoint'.");
			}
		});

		eventLog.runBlock("Validate TLS Connection", () ->{
			validateTlsConnection();
		});

		// see https://openid.net/specs/openid-caep-interoperability-profile-1_0-ID1.html
		// OID_CAEP_INTEROP https://openid.net/specs/openid-caep-interoperability-profile-1_0-ID1.html
		eventLog.runBlock("Prepare Transmitter Access", () -> {
			callAndStopOnFailure(OIDSSFObtainTransmitterAccessToken.class);
		});

		// ensure stream exists
		eventLog.runBlock("Create Stream Configuration", () -> {
			callAndStopOnFailure(OIDSSFCreateStreamConfigCall.class, "OIDSSF-7.1.1.1", "CAEPIOP-2.3.8.2");
			call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
			callAndContinueOnFailure(EnsureHttpStatusCodeIs201.class, Condition.ConditionResult.WARNING, "OIDSSF-7.1.1.1");
			callAndContinueOnFailure(CheckTransmitterMetadataIssuerMatchesIssuerInResponse.class, Condition.ConditionResult.WARNING, "OIDSSF-7.1.1.1");
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
		eventLog.runBlock("Remove Subject to Stream Configuration", () -> {
			callAndStopOnFailure(OIDSSFRemoveSubjectToStreamConfigCall.class, "OIDSSF-7.1.3.2");
			call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
			callAndContinueOnFailure(EnsureHttpStatusCodeIs200.class, Condition.ConditionResult.WARNING, "OIDSSF-7.1.3.2");
			call(exec().unmapKey("endpoint_response"));
		});

		fireTestFinished();
	}

}
