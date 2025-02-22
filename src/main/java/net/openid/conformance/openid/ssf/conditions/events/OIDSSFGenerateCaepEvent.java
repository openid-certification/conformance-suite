package net.openid.conformance.openid.ssf.conditions.events;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jwt.SignedJWT;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.openid.ssf.mock.OIDSSFTransmitterMock;
import net.openid.conformance.testmodule.Environment;

import java.text.ParseException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class OIDSSFGenerateCaepEvent extends AbstractCondition {

	public static final String CAEP_SESSION_REVOKED = "https://schemas.openid.net/secevent/caep/event-type/session-revoked";

	public static final String CAEP_CREDENTIALS_CHANGED = "https://schemas.openid.net/secevent/caep/event-type/credential-change";

	protected final String eventType;

	public OIDSSFGenerateCaepEvent(String eventType) {
		this.eventType = eventType;
	}

	@Override
	public Environment evaluate(Environment env) {

		OIDSSFTransmitterMock mock = new OIDSSFTransmitterMock(env);

		JsonObject subjectIdObject = env.getElementFromObject("ssf", "stream.subject").getAsJsonObject();
		if (subjectIdObject == null) {
			throw error("Missing valid SubjectId");
		}

		Map<String, Object> reasonAdmin = new HashMap<>();
		Map<String, Object> reasonUser = new HashMap<>();

		Map<String, Object> subjectIdMap = mock.subjectIdObjectToMap(subjectIdObject);
		Map<String, Object> caepEvent = createCaepEvent(subjectIdMap, reasonAdmin, reasonUser);

		switch (eventType) {
			case CAEP_SESSION_REVOKED:
				break;
			case CAEP_CREDENTIALS_CHANGED:

				caepEvent.put("initiating_entity", "user");
				/*
				 * password
				 * pin
				 * x509
				 * fido2-platform
				 * fido2-roaming
				 * fido-u2f
				 * verifiable-credential
				 * phone-voice
				 * phone-sms
				 * app
				 */
				caepEvent.put("credential_type", "password");
				/*
				 * create
				 * revoke
				 * update
				 * delete
				 */
				caepEvent.put("change_type", caepEvent.get("update"));

				caepEvent.put("friendly_name", "Password");
				caepEvent.put("x509_issuer", null);
				caepEvent.put("x509_serial", null);
				caepEvent.put("fido2_aaguid", null);
				break;
			default:
				break;
		}

		Map<String, Map<String, Object>> events = new HashMap<>();
		events.put(eventType, caepEvent);

		JWK key = mock.getSetJWK();
		SignedJWT set = mock.createSetFor(subjectIdObject, events, key);

		String encodedSet = mock.signAndEncodeSet(key, set);

		JsonObject sets = new JsonObject();
		try {
			String jti = set.getJWTClaimsSet().getJWTID();
			sets.add(jti, new JsonPrimitive(encodedSet));
			env.putObject("ssf", "stream.sets", sets);

			logSuccess("Emitted CAEP Event " + eventType, args("jti", jti, "event_type", eventType, "set_json", set.getJWTClaimsSet().toString(false), "set_encoded", encodedSet));
		} catch (ParseException e) {
			throw error("Failed to emit CAEP Event", args("error", e.getMessage()));
		}

		return env;
	}

	protected Map<String, Object> createCaepEvent(Map<String, Object> subjectIdObject, Map<String, Object> reasonAdmin, Map<String, Object> reasonUser) {
		Map<String, Object> caepEvent = new HashMap<>();

		caepEvent.put("subject", subjectIdObject);
		caepEvent.put("event_timestamp", Instant.now().toEpochMilli());
		caepEvent.put("initiating_entity", "system"); //system, policy, user, admin

		caepEvent.put("reason_admin", reasonAdmin);
		caepEvent.put("reason_user", reasonUser);

		return caepEvent;
	}
}
