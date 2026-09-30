package net.openid.conformance.openid.ssf;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs201;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs204;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFCreateStreamConfigCall;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFDeleteStreamConfigCall;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFEnsureStreamDeliveryDefaultsToPoll;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFPrepareStreamConfigObject;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFPrepareStreamConfigObjectAddRequestedEvents;
import net.openid.conformance.openid.ssf.variant.SsfDeliveryMode;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.testmodule.TestFailureException;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "openid-ssf-stream-control-create-stream-without-delivery",
	displayName = "OpenID Shared Signals Framework: Create a stream without a delivery property (defaults to poll)",
	summary = """
		This test verifies that a stream created without a 'delivery' property defaults to poll delivery.
		SSF 1.0 8.1.1.1: "If the request does not contain the delivery property, then the Transmitter
		MUST assume that the method is urn:ietf:rfc:8936 (poll). If the Transmitter supports Poll-Based
		Delivery, the Transmitter MUST include a delivery property in the response with this method
		property and an endpoint_url property."
		The testsuite expects to observe the following interactions:
		 * create a stream whose request body contains 'description' and 'events_requested' but no 'delivery'
		 * transmitter answers 201 with a stream configuration whose 'delivery' names the poll method
		   and carries a poll 'endpoint_url'
		 * delete the stream
		""",
	profile = "OIDSSF"
)
@VariantNotApplicable(parameter = SsfDeliveryMode.class, values = "push")
public class OIDSSFStreamControlCreateStreamWithoutDeliveryTest extends AbstractStreamControlErrorTest {

	private volatile boolean streamDeleted = false;

	@Override
	protected void testTransmitter() {

		// SSF 1.0 8.1.1.1 lets a transmitter that does not support poll delivery answer 400; such a
		// transmitter cannot show the poll default, so the poll delivery variant is the wrong choice
		JsonElement methodsEl = env.getElementFromObject("ssf", "transmitter_metadata.delivery_methods_supported");
		if (methodsEl != null && methodsEl.isJsonArray()
			&& !OIDFJSON.convertJsonArrayToList(methodsEl.getAsJsonArray()).contains(SsfConstants.DELIVERY_METHOD_POLL_RFC_8936_URI)) {
			throw new TestFailureException(getId(), "Transmitter metadata does not list poll delivery ('" + SsfConstants.DELIVERY_METHOD_POLL_RFC_8936_URI
				+ "') in delivery_methods_supported, so a stream created without a 'delivery' property cannot default to poll. "
				+ "This test needs a transmitter that supports poll delivery; for a push-only transmitter schedule the plan with push delivery.");
		}

		eventLog.runBlock("Create Stream Configuration without a 'delivery' property", () -> {
			callAndStopOnFailure(OIDSSFPrepareStreamConfigObject.class, "OIDSSF-8.1.1.1");
			callAndStopOnFailure(OIDSSFPrepareStreamConfigObjectAddRequestedEvents.class, "OIDSSF-8.1.1.1");
			callAndStopOnFailure(OIDSSFCreateStreamConfigCall.class, "OIDSSF-8.1.1.1");
			eventLog.log(getName(), args("msg", "Sent stream creation request without a 'delivery' property",
				"request_body", env.getString("resource_request_entity")));

			call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
			callAndStopOnFailure(EnsureHttpStatusCodeIs201.class, "OIDSSF-8.1.1.1");
			call(exec().unmapKey("endpoint_response"));

			callAndContinueOnFailure(OIDSSFEnsureStreamDeliveryDefaultsToPoll.class, Condition.ConditionResult.FAILURE, "OIDSSF-8.1.1.1", "OIDSSF-6.1.2");
		});

		eventLog.runBlock("Delete Stream Configuration", () -> {
			callAndStopOnFailure(OIDSSFDeleteStreamConfigCall.class, "OIDSSF-8.1.1.5");
			call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
			callAndStopOnFailure(EnsureHttpStatusCodeIs204.class, "OIDSSF-8.1.1.5");
			call(exec().unmapKey("endpoint_response"));
			streamDeleted = true;
		});
	}

	@Override
	public void cleanup() {
		eventLog.runBlock("Cleanup", () -> {
			if (!streamDeleted && env.getString("ssf", "stream.stream_id") != null) {
				callAndContinueOnFailure(OIDSSFDeleteStreamConfigCall.class, Condition.ConditionResult.INFO);
			}
			super.cleanup();
		});
	}
}
