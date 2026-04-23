package net.openid.conformance.openid.ssf.conditions.streams;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.openid.ssf.SsfEvents;
import net.openid.conformance.openid.ssf.eventstore.OIDSSFEventStore;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class OIDSSFGenerateStreamVerificationSET extends AbstractOIDSSFGenerateStreamSET {

	public OIDSSFGenerateStreamVerificationSET(OIDSSFEventStore eventStore) {
		super(eventStore, SsfEvents.SSF_STREAM_VERIFICATION_EVENT_TYPE);
	}

	@Override
	protected JsonObject getEventData(JsonObject streamConfig) {
		// "state" is optional in a verification event (SSF 1.0 §8.1.4-2).
		// It is only included when a prior verification request supplied one;
		// otherwise this is an ad-hoc / unsolicited verification event.
		JsonObject eventData = new JsonObject();
		JsonElement stateEl = streamConfig.get("_verification_state");
		if (stateEl != null && !stateEl.isJsonNull()) {
			eventData.addProperty("state", OIDFJSON.getString(stateEl));
		}
		return eventData;
	}

	@Override
	protected void afterSecurityEventTokenGenerated(Environment env, String streamId, JsonObject streamConfig, String setJti, String setTokenString, JsonObject setObject) {
		super.afterSecurityEventTokenGenerated(env, streamId, streamConfig, setJti, setTokenString, setObject);

		// clear verification state
		streamConfig.remove("_verification_state");
	}
}
