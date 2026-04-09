package net.openid.conformance.openid.ssf.conditions.streams;

import com.google.gson.JsonObject;
import net.openid.conformance.openid.ssf.eventstore.OIDSSFEventStore;
import net.openid.conformance.testmodule.Environment;

/**
 * Generates a stream verification SET for the given stream without a {@code state}
 * claim in the verification event data. Used to deliver unsolicited verification
 * events that are not triggered by a prior verification request from the receiver.
 * <p>
 * Per SSF 1.0 §8.1.4.2: "If the Verification Event is initiated by the Transmitter
 * then this parameter [state] MUST not be set." This class enforces that rule by
 * always producing an empty event-data object, regardless of whether the stream
 * config happens to carry a leftover {@code _verification_state} from a prior
 * receiver-initiated verification request.
 */
public class OIDSSFGenerateUnsolicitedStreamVerificationSET extends OIDSSFGenerateStreamVerificationSET {

	private final String streamId;

	public OIDSSFGenerateUnsolicitedStreamVerificationSET(OIDSSFEventStore eventStore, String streamId) {
		super(eventStore);
		this.streamId = streamId;
	}

	@Override
	protected String getCurrentStreamId(Environment env) {
		// Override the default lookup (incoming_request.body_json.stream_id) because
		// unsolicited delivery happens outside the context of an incoming verification request.
		return streamId;
	}

	@Override
	protected JsonObject getEventData(JsonObject streamConfig) {
		// SSF 1.0 §8.1.4.2: transmitter-initiated verification events MUST NOT
		// carry a 'state' claim. Return an empty event-data object unconditionally
		// so a leftover '_verification_state' on streamConfig cannot leak into
		// an unsolicited event.
		return new JsonObject();
	}
}
