package net.openid.conformance.openid.ssf.conditions.streams;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFSecurityEvent;
import net.openid.conformance.openid.ssf.eventstore.OIDSSFEventStore;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.UUID;
import java.util.function.Consumer;

/**
 * Answers a receiver's verification request with a verification SET whose {@code state}
 * is not the one the receiver sent: a request that carried a state gets a different value
 * echoed, a request without one gets a made-up state (a solicited event carrying a state
 * the receiver never sent is equally wrong). SSF 1.0 8.1.4.1 requires the receiver to
 * confirm the state and lets it reject a mismatch with {@code invalid_state}.
 * <p>
 * The SET is enqueued in the event store like a regular verification SET; the generated
 * event is additionally handed to {@code onGenerated} so the test module can single it out
 * when grading the receiver's reaction.
 */
public class OIDSSFGenerateWrongStateStreamVerificationSET extends OIDSSFGenerateStreamVerificationSET {

	protected final Consumer<OIDSSFSecurityEvent> onGenerated;

	public OIDSSFGenerateWrongStateStreamVerificationSET(OIDSSFEventStore eventStore, Consumer<OIDSSFSecurityEvent> onGenerated) {
		super(eventStore);
		this.onGenerated = onGenerated;
	}

	@Override
	protected JsonObject getEventData(JsonObject streamConfig) {
		String wrongState = "unexpected-" + UUID.randomUUID();

		JsonElement stateEl = streamConfig.get("_verification_state");
		String receiverState = stateEl == null || stateEl.isJsonNull() ? null : OIDFJSON.getString(stateEl);
		if (receiverState == null) {
			log("The verification request carried no state; the verification event gets a state the receiver never sent",
				args("state", wrongState));
		} else {
			log("The verification request carried a state; the verification event echoes a different value",
				args("state", wrongState, "receiver_state", receiverState));
		}

		JsonObject eventData = new JsonObject();
		eventData.addProperty("state", wrongState);
		return eventData;
	}

	@Override
	protected void afterSecurityEventTokenGenerated(Environment env, String streamId, JsonObject streamConfig, String setJti, String setTokenString, JsonObject setObject) {
		super.afterSecurityEventTokenGenerated(env, streamId, streamConfig, setJti, setTokenString, setObject);
		onGenerated.accept(new OIDSSFSecurityEvent(setJti, setTokenString, eventType));
	}
}
