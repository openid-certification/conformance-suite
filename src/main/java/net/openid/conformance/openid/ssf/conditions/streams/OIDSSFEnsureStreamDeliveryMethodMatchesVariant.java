package net.openid.conformance.openid.ssf.conditions.streams;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.openid.ssf.variant.SsfDeliveryMode;
import net.openid.conformance.testmodule.Environment;

/**
 * Asserts that the delivery method the receiver under test chose when creating its stream
 * matches the {@code ssf_delivery_mode} variant the test was scheduled with.
 * <p>
 * The delivery method is part of the certification evidence (it appears in the certification
 * profile name), so a receiver that creates a poll stream during a run scheduled as push -
 * or vice versa - would otherwise be certified for a delivery method it never demonstrated.
 * The CAEP Interop Profile (2.4.1) lets a receiver support either method; this check only
 * requires the run to exercise the one it was scheduled for.
 */
public class OIDSSFEnsureStreamDeliveryMethodMatchesVariant extends AbstractCondition {

	protected final String streamId;

	protected final SsfDeliveryMode expectedDeliveryMode;

	public OIDSSFEnsureStreamDeliveryMethodMatchesVariant(String streamId, SsfDeliveryMode expectedDeliveryMode) {
		this.streamId = streamId;
		this.expectedDeliveryMode = expectedDeliveryMode;
	}

	@Override
	public Environment evaluate(Environment env) {

		JsonObject streamConfig = OIDSSFStreamUtils.getStreamConfig(env, streamId);
		if (streamConfig == null) {
			throw error("Could not find stream configuration", args("stream_id", streamId));
		}

		String actualDeliveryMethod = OIDSSFStreamUtils.getStreamDeliveryMethod(streamConfig);
		String expectedDeliveryMethod = expectedDeliveryMode.getAlias();

		if (!expectedDeliveryMethod.equals(actualDeliveryMethod)) {
			throw error("The receiver created a stream with a delivery method that does not match the 'SSF Delivery Mode' selected for this test. "
					+ "Schedule the test with the delivery mode the receiver uses, or have the receiver request the selected one.",
				args("stream_id", streamId, "selected_delivery_mode", expectedDeliveryMode.toString(),
					"expected_delivery_method", expectedDeliveryMethod, "actual_delivery_method", actualDeliveryMethod));
		}

		logSuccess("The receiver's stream uses the delivery method selected for this test",
			args("stream_id", streamId, "delivery_method", actualDeliveryMethod));

		return env;
	}
}
