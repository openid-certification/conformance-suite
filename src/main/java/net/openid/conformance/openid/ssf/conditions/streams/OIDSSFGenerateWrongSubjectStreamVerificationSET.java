package net.openid.conformance.openid.ssf.conditions.streams;

import com.google.gson.JsonObject;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFSecurityEvent;
import net.openid.conformance.openid.ssf.eventstore.OIDSSFEventStore;
import net.openid.conformance.testmodule.Environment;

import java.util.function.Consumer;

/**
 * Answers a receiver's verification request with a verification SET whose {@code sub_id} does
 * not name the stream being verified: the format is opaque as required, but the id is not the
 * stream's. SSF 1.0 8.1.4.1: the receiver "SHALL parse the SET and validate its claims", and
 * for the verification event's {@code sub_id} "The id of the value MUST be the stream_id of the
 * stream being verified". The state the receiver sent is echoed correctly, so the subject is
 * the only defect.
 * <p>
 * The SET is enqueued like a regular verification SET; the generated event is handed to
 * {@code onGenerated} so the test module can single it out when grading the receiver's reaction.
 */
public class OIDSSFGenerateWrongSubjectStreamVerificationSET extends OIDSSFGenerateStreamVerificationSET {

	protected final Consumer<OIDSSFSecurityEvent> onGenerated;

	public OIDSSFGenerateWrongSubjectStreamVerificationSET(OIDSSFEventStore eventStore, Consumer<OIDSSFSecurityEvent> onGenerated) {
		super(eventStore);
		this.onGenerated = onGenerated;
	}

	@Override
	protected JsonObject getSubject(String streamId) {
		String wrongStreamId = "not-" + streamId;
		log("The verification event names a stream other than the one being verified in its sub_id",
			args("stream_id", streamId, "sub_id_stream_id", wrongStreamId));
		return generateStreamSubject(wrongStreamId);
	}

	@Override
	protected void afterSecurityEventTokenGenerated(Environment env, String streamId, JsonObject streamConfig, String setJti, String setTokenString, JsonObject setObject) {
		super.afterSecurityEventTokenGenerated(env, streamId, streamConfig, setJti, setTokenString, setObject);
		onGenerated.accept(new OIDSSFSecurityEvent(setJti, setTokenString, eventType));
	}
}
