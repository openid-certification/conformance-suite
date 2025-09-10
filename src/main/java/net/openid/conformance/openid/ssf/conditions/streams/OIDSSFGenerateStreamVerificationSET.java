package net.openid.conformance.openid.ssf.conditions.streams;

import com.google.gson.JsonObject;
import net.openid.conformance.openid.ssf.SsfEvents;
import net.openid.conformance.openid.ssf.eventstore.OIDSSFEventStore;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.Map;

public class OIDSSFGenerateStreamVerificationSET extends AbstractOIDSSFGenerateStreamSET {

	public OIDSSFGenerateStreamVerificationSET(OIDSSFEventStore eventStore) {
		super(eventStore, SsfEvents.SSF_STREAM_VERIFICATION_EVENT_TYPE);
	}

	@Override
	protected JsonObject getEventData(JsonObject streamConfig) {
		return OIDFJSON.convertMapToJsonObject(Map.of("state", OIDFJSON.tryGetString(streamConfig.get("_verification_state"))));
	}

	@Override
	protected void afterSecurityEventTokenGenerated(Environment env, String streamId, JsonObject streamConfig, String setJti, String setTokenString, JsonObject setObject) {
		super.afterSecurityEventTokenGenerated(env, streamId, streamConfig, setJti, setTokenString, setObject);

		// clear verification state
		streamConfig.remove("_verification_state");
	}
}
