package net.openid.conformance.openid.ssf.conditions.streams;

import com.google.gson.JsonObject;
import com.nimbusds.jwt.JWTClaimsSet;
import net.openid.conformance.openid.ssf.eventstore.OIDSSFEventStore;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class OIDSSFGenerateVerificationSET extends AbstractOIDSSFGenerateSET {

	public OIDSSFGenerateVerificationSET(OIDSSFEventStore eventStore) {
		super(eventStore);
	}

	@Override
	protected void addSubjectAndEvents(String streamId, JsonObject streamConfig, JWTClaimsSet.Builder claimsBuilder) {

		JsonObject subject = new JsonObject();
		subject.addProperty("format", "opaque");
		subject.addProperty("id", streamId);

		JsonObject verificationEvent = new JsonObject();
		verificationEvent.addProperty("state", OIDFJSON.tryGetString(streamConfig.get("_verification_state")));

		JsonObject events = new JsonObject();
		events.add("https://schemas.openid.net/secevent/ssf/event-type/verification", verificationEvent);

		claimsBuilder
			.claim("sub_id", subject)
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

		// OIDSSFStreamUtils.publishSecurityEventTokenForStream(streamConfig, setJti, setTokenString);
	}
}
