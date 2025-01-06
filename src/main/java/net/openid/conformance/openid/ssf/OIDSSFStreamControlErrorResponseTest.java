package net.openid.conformance.openid.ssf;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs400;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs401;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs404;
import net.openid.conformance.openid.ssf.conditions.OIDSSFInjectDummyStreamId;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFAttemptCreateStreamConfigCallWithBrokenInput;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFAttemptReadStreamConfigCallWithUnknownStreamId;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFCreateStreamConditionSequenceWithInvalidAccessToken;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFDeleteStreamConfigCall;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFInjectInvalidAccessTokenOverride;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFReadStreamConfigCall;
import net.openid.conformance.openid.ssf.variant.SsfAuthMode;
import net.openid.conformance.openid.ssf.variant.SsfDeliveryMode;
import net.openid.conformance.openid.ssf.variant.SsfServerMetadata;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.VariantConfigurationFields;
import net.openid.conformance.variant.VariantParameters;

@PublishTestModule(
	testName = "openid-ssf-stream-control-error-path",
	displayName = "OpenID Shared Signals Framework: Validate Stream Control (Error Paths)",
	summary = "This test verifies the behavior of the stream control for error cases.",
	profile = "OIDSSF",
	configurationFields = {
		"ssf.transmitter.issuer",
		"ssf.transmitter.metadata_suffix", // see: https://openid.net/specs/openid-sharedsignals-framework-1_0.html#section-6.2.1
	}
)
@VariantParameters({SsfServerMetadata.class, SsfAuthMode.class, SsfDeliveryMode.class,})
@VariantConfigurationFields(parameter = SsfServerMetadata.class, value = "static", configurationFields = {"ssf.transmitter.configuration_metadata_endpoint",})
@VariantConfigurationFields(parameter = SsfServerMetadata.class, value = "discovery", configurationFields = {"ssf.transmitter.issuer", "ssf.transmitter.metadata_suffix",})
@VariantConfigurationFields(parameter = SsfAuthMode.class, value = "static", configurationFields = {
	"ssf.transmitter.access_token"
})
@VariantConfigurationFields(parameter = SsfAuthMode.class, value = "dynamic", configurationFields = {
})
public class OIDSSFStreamControlErrorResponseTest extends AbstractOIDSSFTest {

	@Override
	public void start() {
		setStatus(Status.RUNNING);

		eventLog.runBlock("Fetch Transmitter Metadata", this::fetchTransmitterMetadata);

		eventLog.runBlock("Prepare Transmitter Access", this::obtainTransmitterAccessToken);

		eventLog.runBlock("Create Stream Configuration", () -> {

			SsfDeliveryMode deliveryMode = getVariant(SsfDeliveryMode.class);
			env.putString("ssf", "delivery_method", deliveryMode.getAlias());

			// 400	if the request cannot be parsed
			callAndStopOnFailure(OIDSSFAttemptCreateStreamConfigCallWithBrokenInput.class, "OIDSSF-7.1.1.1");
			call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
			callAndContinueOnFailure(EnsureHttpStatusCodeIs400.class, Condition.ConditionResult.FAILURE, "OIDSSF-7.1.1.1");
			call(exec().unmapKey("endpoint_response"));

			// 401	if authorization failed or it is missing
//			callAndStopOnFailure(OIDSSFInjectInvalidAccessTokenOverride.class);
//			callAndContinueOnFailure(OIDSSFCreateStreamConfigCall.class, Condition.ConditionResult.FAILURE, "OIDSSF-7.1.1.1");
			call(sequence(OIDSSFCreateStreamConditionSequenceWithInvalidAccessToken.class));
			call(exec().unmapKey("access_token"));

			call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
			callAndContinueOnFailure(EnsureHttpStatusCodeIs401.class, Condition.ConditionResult.WARNING, "OIDSSF-7.1.1.1");
			call(exec().unmapKey("endpoint_response"));

			// TODO 403	if the Event Receiver is not allowed to create a stream

			// TODO 409	if the Transmitter does not support multiple streams per Receiver
		});

		eventLog.runBlock("Read Stream Configuration", () -> {
			// 401	if authorization failed or it is missing
			callAndStopOnFailure(OIDSSFInjectInvalidAccessTokenOverride.class);
			callAndContinueOnFailure(OIDSSFReadStreamConfigCall.class, Condition.ConditionResult.FAILURE, "OIDSSF-7.1.1.2");
			call(exec().unmapKey("access_token"));
			call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
			callAndContinueOnFailure(EnsureHttpStatusCodeIs401.class, Condition.ConditionResult.WARNING, "OIDSSF-7.1.1.2");
			call(exec().unmapKey("endpoint_response"));

			// TODO 403	if the Event Receiver is not allowed to read the stream configuration

			// 404	if there is no Event Stream with the given "stream_id" for this Event Receiver
			callAndStopOnFailure(OIDSSFAttemptReadStreamConfigCallWithUnknownStreamId.class, "OIDSSF-7.1.1.2");
			call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
			callAndContinueOnFailure(EnsureHttpStatusCodeIs404.class, Condition.ConditionResult.WARNING, "OIDSSF-7.1.1.2");
			call(exec().unmapKey("endpoint_response"));
		});

		eventLog.runBlock("Update Stream Configuration", () -> {
			// TODO check 400	if the request body cannot be parsed, a Transmitter-Supplied property is incorrect, or if the request is otherwise invalid
			// TODO check 401	if authorization failed or it is missing
			// TODO check 403	if the Event Receiver is not allowed to update the stream configuration
			// TODO check 404	if there is no Event Stream with the given "stream_id" for this Event Receiver
		});

		eventLog.runBlock("Replace Stream Configuration", () -> {
			//	TODO check 400	if the request body cannot be parsed, a Transmitter-Supplied property is incorrect, or if the request is otherwise invalid
			//	TODO check 401	if authorization failed or it is missing
			//	TODO check 403	if the Event Receiver is not allowed to replace the stream configuration
			//	TODO check 404	if there is no Event Stream with the given "stream_id" for this Event Receiver
		});

		eventLog.runBlock("Delete Stream Configuration", () -> {
			// check 401	if authorization failed or it is missing
			callAndStopOnFailure(OIDSSFInjectInvalidAccessTokenOverride.class);
			callAndStopOnFailure(OIDSSFInjectDummyStreamId.class);
			callAndContinueOnFailure(OIDSSFDeleteStreamConfigCall.class, Condition.ConditionResult.FAILURE, "OIDSSF-7.1.1.2");
			call(exec().unmapKey("access_token"));
			call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
			callAndContinueOnFailure(EnsureHttpStatusCodeIs401.class, Condition.ConditionResult.WARNING, "OIDSSF-7.1.1.2");
			call(exec().unmapKey("endpoint_response"));

			// TODO check 403	if the Event Receiver is not allowed to delete the stream
			// TODO check 404	if there is no Event Stream with the given "stream_id" for this Event Receiver
		});

		fireTestFinished();
	}

}
