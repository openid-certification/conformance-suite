package net.openid.conformance.openid.ssf.conditions.streams;

import com.google.gson.JsonObject;
import com.nimbusds.jwt.JWTClaimsSet;
import net.openid.conformance.openid.ssf.eventstore.OIDSSFEventStore;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.Map;

public class OIDSSFGenerateStreamVerificationSET extends AbstractOIDSSFGenerateStreamSET {

	public OIDSSFGenerateStreamVerificationSET(OIDSSFEventStore eventStore) {
		super(eventStore);
	}

	@Override
	protected void addSubjectAndEvents(String streamId, JsonObject streamConfig, JWTClaimsSet.Builder claimsBuilder) {

		JsonObject verificationEvent = OIDFJSON.convertMapToJsonObject(Map.of("state", OIDFJSON.tryGetString(streamConfig.get("_verification_state"))));

		JsonObject events = OIDFJSON.convertMapToJsonObject(Map.of("https://schemas.openid.net/secevent/ssf/event-type/verification", verificationEvent));

		claimsBuilder
			.claim("sub_id", generateStreamSubject(streamId))
			.claim("events", events);
	}

	@Override
	protected void afterSecurityEventTokenGenerated(Environment env, String streamId, JsonObject streamConfig, String setJti, String setTokenString, JsonObject setObject) {
		super.afterSecurityEventTokenGenerated(env, streamId, streamConfig, setJti, setTokenString, setObject);

		// clear verification state
		streamConfig.remove("_verification_state");

		JsonObject jtiSetObject = new JsonObject();
		jtiSetObject.addProperty(setJti, setTokenString);
		eventStore.storeEvent(streamId, jtiSetObject);
	}
}
