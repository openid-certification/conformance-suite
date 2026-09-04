package net.openid.conformance.openid.ssf.conditions.streams;

import com.google.gson.JsonObject;
import net.openid.conformance.openid.ssf.SsfEvent;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFSecurityEvent;
import net.openid.conformance.openid.ssf.eventstore.OIDSSFEventStore;
import net.openid.conformance.testmodule.Environment;

import java.util.function.Consumer;

/**
 * Generates a deliberately invalid SET for the receiver negative tests:
 * a receiver MUST validate delivered SETs (iss per SSF 1.0 4.1.6, signature per
 * CAEP Interop Profile 2.4.2, aud per RFC 8935/8936) and must not acknowledge
 * an invalid one.
 * <p>
 * The generated event is optionally enqueued in the event store (for POLL
 * delivery, so the receiver retrieves it via the poll endpoint) and always
 * handed to {@code onGenerated} (for PUSH delivery, where the test module
 * pushes it directly and inspects the receiver's response).
 */
public class OIDSSFGenerateTamperedStreamSET extends OIDSSFGenerateStreamSET {

	public enum TamperMode {
		INVALID_SIGNATURE("valid claims but a corrupted signature"),
		WRONG_ISSUER("an iss claim that does not match the transmitter issuer"),
		WRONG_AUDIENCE("an aud claim that does not match the stream audience");

		private final String description;

		TamperMode(String description) {
			this.description = description;
		}

		public String description() {
			return description;
		}
	}

	protected final TamperMode tamperMode;

	protected final boolean enqueueInEventStore;

	protected final Consumer<OIDSSFSecurityEvent> onGenerated;

	public OIDSSFGenerateTamperedStreamSET(OIDSSFEventStore eventStore, String streamId, JsonObject subject,
			SsfEvent ssfEvent, TamperMode tamperMode, boolean enqueueInEventStore, Consumer<OIDSSFSecurityEvent> onGenerated) {
		super(eventStore, streamId, subject, ssfEvent, (sid, jti) -> {
			// enqueue notification not used for tampered SETs
		});
		this.tamperMode = tamperMode;
		this.enqueueInEventStore = enqueueInEventStore;
		this.onGenerated = onGenerated;
	}

	@Override
	protected String getIssuer(Environment env) {
		if (tamperMode == TamperMode.WRONG_ISSUER) {
			return "https://invalid-issuer.example.com";
		}
		return super.getIssuer(env);
	}

	@Override
	protected String getAudience(Environment env) {
		if (tamperMode == TamperMode.WRONG_AUDIENCE) {
			return "https://invalid-audience.example.com";
		}
		return super.getAudience(env);
	}

	@Override
	protected String postProcessSerializedSecurityEventToken(String setTokenString) {
		if (tamperMode == TamperMode.INVALID_SIGNATURE) {
			return corruptSignature(setTokenString);
		}
		return setTokenString;
	}

	/**
	 * Flips a character in the middle of the base64url-encoded signature part;
	 * the middle always carries signature bits (unlike the final character,
	 * whose low bits may be base64 padding), so the signature is guaranteed to
	 * become invalid while the JWS stays parseable.
	 */
	protected String corruptSignature(String token) {
		int signatureStart = token.lastIndexOf('.') + 1;
		int flipIndex = signatureStart + (token.length() - signatureStart) / 2;
		char original = token.charAt(flipIndex);
		char replacement = original == 'A' ? 'B' : 'A';
		return token.substring(0, flipIndex) + replacement + token.substring(flipIndex + 1);
	}

	@Override
	protected void afterSecurityEventTokenGenerated(Environment env, String streamId, JsonObject streamConfig, String setJti, String setTokenString, JsonObject setObject) {
		OIDSSFSecurityEvent event = new OIDSSFSecurityEvent(setJti, setTokenString, eventType);
		if (enqueueInEventStore) {
			eventStore.storeEvent(streamId, event);
		}
		log("Generated deliberately invalid SET (" + tamperMode.name() + ": " + tamperMode.description() + ") for stream_id=" + streamId,
			args("jti", setJti, "tamper_mode", tamperMode.name(), "enqueued", enqueueInEventStore));
		onGenerated.accept(event);
	}
}
