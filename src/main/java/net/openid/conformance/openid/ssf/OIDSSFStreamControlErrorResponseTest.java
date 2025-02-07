package net.openid.conformance.openid.ssf;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs201;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs400;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs401;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs404;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs409;
import net.openid.conformance.openid.ssf.conditions.OIDSSFInjectDummyStreamId;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFCreateStreamConditionSequence;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFDeleteStreamConfigCall;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFInjectInvalidAccessTokenOverride;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFInsertBrokenStreamConfigJsonOverride;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFReadStreamConfigCall;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFReplaceStreamConditionSequence;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFUpdateStreamConditionSequence;
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
public class OIDSSFStreamControlErrorResponseTest extends AbstractOIDSSFTransmitterTestModule {

	@Override
	public void start() {
		setStatus(Status.RUNNING);

		eventLog.runBlock("Fetch Transmitter Metadata", this::fetchTransmitterMetadata);

		eventLog.runBlock("Prepare Transmitter Access", this::obtainTransmitterAccessToken);

		SsfDeliveryMode deliveryMode = getVariant(SsfDeliveryMode.class);
		env.putString("ssf", "delivery_method", deliveryMode.getAlias());

		// expect 400	if the request cannot be parsed
		eventLog.runBlock("Create Stream: Attempt to create Stream Configuration with broken input", () -> {

			callAndStopOnFailure(OIDSSFInsertBrokenStreamConfigJsonOverride.class);
			call(sequence(OIDSSFCreateStreamConditionSequence.class));
			OIDSSFInsertBrokenStreamConfigJsonOverride.undo(env);
			call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
			callAndContinueOnFailure(EnsureHttpStatusCodeIs400.class, Condition.ConditionResult.FAILURE, "OIDSSF-7.1.1.1");
			call(exec().unmapKey("endpoint_response"));
		});

		// expect 401	if authorization failed or it is missing
		eventLog.runBlock("Create Stream: Attempt to create Stream Configuration with invalid access token", () -> {
			callAndStopOnFailure(OIDSSFInjectInvalidAccessTokenOverride.class);
			call(sequence(OIDSSFCreateStreamConditionSequence.class));
			OIDSSFInjectInvalidAccessTokenOverride.undo(env);
			call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
			callAndContinueOnFailure(EnsureHttpStatusCodeIs401.class, Condition.ConditionResult.FAILURE, "OIDSSF-7.1.1.1");
			call(exec().unmapKey("endpoint_response"));
		});

		// TODO 403	if the Event Receiver is not allowed to create a stream
//		eventLog.runBlock("Create Stream: Attempt to create a Stream Configuration with insufficiently scoped access token", () -> {
//
//			if (SsfAuthMode.STATIC == getVariant(SsfAuthMode.class)) {
//				// test cannot be executed with a single static auth token
//				return;
//			}
//
//			// obtain an access token without ssf.read and ssf.manage
//			// TODO obtainTransmitterAccessToken("openid");
//
//			// 403	if authorization failed or it is missing
//			call(sequence(OIDSSFCreateStreamConditionSequenceWithInvalidAccessToken.class));
//			call(exec().unmapKey("access_token"));
//
//			call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
//			callAndContinueOnFailure(EnsureHttpStatusCodeIs401.class, Condition.ConditionResult.WARNING, "OIDSSF-7.1.1.1");
//			call(exec().unmapKey("endpoint_response"));
//		});

		// expect 409	if the Transmitter does not support multiple streams per Receiver
		eventLog.runBlock("Create Stream: Attempt to create multiple stream configuration with same access token", () -> {

			cleanUpStreamConfigurationIfNecessary();

			call(sequence(OIDSSFCreateStreamConditionSequence.class));

			call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
			callAndContinueOnFailure(EnsureHttpStatusCodeIs201.class, Condition.ConditionResult.FAILURE, "OIDSSF-7.1.1.1");
			call(exec().unmapKey("endpoint_response"));

			eventLog.log("Attempt to create another stream definition", args());

			// 409	if authorization failed or it is missing
			call(sequence(OIDSSFCreateStreamConditionSequence.class));

			call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
			callAndContinueOnFailure(EnsureHttpStatusCodeIs409.class, Condition.ConditionResult.FAILURE, "OIDSSF-7.1.1.1");
			call(exec().unmapKey("endpoint_response"));
		});

		// expect 401	if authorization failed or it is missing
		eventLog.runBlock("Update Stream: Attempt to read an existing Stream Configuration with invalid access token", () -> {

			callAndStopOnFailure(OIDSSFInjectInvalidAccessTokenOverride.class);
			callAndContinueOnFailure(OIDSSFReadStreamConfigCall.class, Condition.ConditionResult.FAILURE, "OIDSSF-7.1.1.2");
			OIDSSFInjectInvalidAccessTokenOverride.undo(env);
			call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
			callAndContinueOnFailure(EnsureHttpStatusCodeIs401.class, Condition.ConditionResult.FAILURE, "OIDSSF-7.1.1.2");
			call(exec().unmapKey("endpoint_response"));
		});

//		eventLog.runBlock("Read Stream: Attempt to read an existing Stream Configuration with insufficient access token", () -> {
//
//			// TODO 403	if the Event Receiver is not allowed to read the stream configuration
////			if (SsfAuthMode.STATIC == getVariant(SsfAuthMode.class)) {
////				// test cannot be executed with a single static auth token
////				return;
////			}
////
////			// obtain an access token without ssf.read and ssf.manage
////			// TODO obtainTransmitterAccessToken("openid");
//
////			callAndStopOnFailure(OIDSSFInjectInvalidAccessTokenOverride.class);
//			callAndContinueOnFailure(OIDSSFReadStreamConfigCall.class, Condition.ConditionResult.FAILURE, "OIDSSF-7.1.1.2");
//			call(exec().unmapKey("access_token"));
//			call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
//			callAndContinueOnFailure(EnsureHttpStatusCodeIs403.class, Condition.ConditionResult.WARNING, "OIDSSF-7.1.1.2");
//			call(exec().unmapKey("endpoint_response"));
//		});

		// expect 404	if there is no Event Stream with the given "stream_id" for this Event Receiver
		eventLog.runBlock("Read Stream: Attempt to read an non existing Stream Configuration a valid access token", () -> {
			callAndStopOnFailure(OIDSSFInjectDummyStreamId.class);
			callAndStopOnFailure(OIDSSFReadStreamConfigCall.class, "OIDSSF-7.1.1.2");
			OIDSSFInjectDummyStreamId.undo(env);
			call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
			callAndContinueOnFailure(EnsureHttpStatusCodeIs404.class, Condition.ConditionResult.FAILURE, "OIDSSF-7.1.1.2");
			call(exec().unmapKey("endpoint_response"));
		});

		// expect 400	if the request body cannot be parsed, a Transmitter-Supplied property is incorrect, or if the request is otherwise invalid
		eventLog.runBlock("Update Stream: Attempt to update Stream Configuration with invalid body", () -> {
			callAndStopOnFailure(OIDSSFInsertBrokenStreamConfigJsonOverride.class);
			call(sequence(OIDSSFUpdateStreamConditionSequence.class));
			OIDSSFInsertBrokenStreamConfigJsonOverride.undo(env);
			call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
			callAndContinueOnFailure(EnsureHttpStatusCodeIs400.class, Condition.ConditionResult.FAILURE, "OIDSSF-7.1.1.3");
			call(exec().unmapKey("endpoint_response"));
		});

		// expect 401	if authorization failed or it is missing
		eventLog.runBlock("Update Stream: Attempt to update Stream Configuration with invalid access token", () -> {
			callAndStopOnFailure(OIDSSFInjectInvalidAccessTokenOverride.class);
			call(sequence(OIDSSFUpdateStreamConditionSequence.class));
			OIDSSFInjectInvalidAccessTokenOverride.undo(env);
			call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
			callAndContinueOnFailure(EnsureHttpStatusCodeIs401.class, Condition.ConditionResult.FAILURE, "OIDSSF-7.1.1.3");
			call(exec().unmapKey("endpoint_response"));
		});

		// TODO check 403	if the Event Receiver is not allowed to update the stream configuration

		// expect 404	if there is no Event Stream with the given "stream_id" for this Event Receiver
		eventLog.runBlock("Update Stream: Attempt to update Stream Configuration for unknown stream", () -> {
			callAndStopOnFailure(OIDSSFInjectDummyStreamId.class);
			call(sequence(OIDSSFUpdateStreamConditionSequence.class));
			OIDSSFInjectDummyStreamId.undo(env);
			call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
			callAndContinueOnFailure(EnsureHttpStatusCodeIs404.class, Condition.ConditionResult.FAILURE, "OIDSSF-7.1.1.3");
			call(exec().unmapKey("endpoint_response"));
		});

		// Expect 400	if the request body cannot be parsed, a Transmitter-Supplied property is incorrect, or if the request is otherwise invalid
		eventLog.runBlock("Replace Stream: Attempt to replace a Stream Configuration with invalid body", () -> {
			callAndStopOnFailure(OIDSSFInsertBrokenStreamConfigJsonOverride.class);
			call(sequence(OIDSSFReplaceStreamConditionSequence.class));
			OIDSSFInsertBrokenStreamConfigJsonOverride.undo(env);
			call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
			callAndContinueOnFailure(EnsureHttpStatusCodeIs400.class, Condition.ConditionResult.FAILURE, "OIDSSF-7.1.1.4");
			call(exec().unmapKey("endpoint_response"));
		});

		//	Expect 401	if authorization failed or it is missing
		eventLog.runBlock("Replace Stream: Attempt to replace a Stream Configuration with invalid access token", () -> {
			callAndStopOnFailure(OIDSSFInjectInvalidAccessTokenOverride.class);
			call(sequence(OIDSSFReplaceStreamConditionSequence.class));
			OIDSSFInjectInvalidAccessTokenOverride.undo(env);
			call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
			callAndContinueOnFailure(EnsureHttpStatusCodeIs401.class, Condition.ConditionResult.FAILURE, "OIDSSF-7.1.1.4");
			call(exec().unmapKey("endpoint_response"));
		});

		//	TODO check 403	if the Event Receiver is not allowed to replace the stream configuration

		//	Expect 404	if there is no Event Stream with the given "stream_id" for this Event Receiver
		eventLog.runBlock("Replace Stream: Attempt to replace a Stream Configuration for a non existing stream", () -> {
			callAndStopOnFailure(OIDSSFInjectDummyStreamId.class);
			call(sequence(OIDSSFReplaceStreamConditionSequence.class));
			OIDSSFInjectDummyStreamId.undo(env);
			call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
			callAndContinueOnFailure(EnsureHttpStatusCodeIs404.class, Condition.ConditionResult.FAILURE, "OIDSSF-7.1.1.4");
			call(exec().unmapKey("endpoint_response"));
		});

		// Expect 401	if authorization failed or it is missing
		eventLog.runBlock("Delete Stream: Delete Stream Configuration with invalid access token", () -> {
			callAndStopOnFailure(OIDSSFInjectInvalidAccessTokenOverride.class);
			callAndContinueOnFailure(OIDSSFDeleteStreamConfigCall.class, Condition.ConditionResult.FAILURE, "OIDSSF-7.1.1.5");
			OIDSSFInjectInvalidAccessTokenOverride.undo(env);
			call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
			callAndContinueOnFailure(EnsureHttpStatusCodeIs401.class, Condition.ConditionResult.FAILURE, "OIDSSF-7.1.1.5");
			call(exec().unmapKey("endpoint_response"));
		});

		// TODO check 403	if the Event Receiver is not allowed to delete the stream

		// TODO check 404	if there is no Event Stream with the given "stream_id" for this Event Receiver
		eventLog.runBlock("Delete Stream: Delete Stream Configuration for unknown stream", () -> {
			// check 401	if authorization failed or it is missing
			callAndStopOnFailure(OIDSSFInjectDummyStreamId.class);
			callAndContinueOnFailure(OIDSSFDeleteStreamConfigCall.class, Condition.ConditionResult.FAILURE, "OIDSSF-7.1.1.5");
			OIDSSFInjectDummyStreamId.undo(env);
			call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
			callAndContinueOnFailure(EnsureHttpStatusCodeIs404.class, Condition.ConditionResult.FAILURE, "OIDSSF-7.1.1.5");
			call(exec().unmapKey("endpoint_response"));
		});

		fireTestFinished();
	}

}
