package net.openid.conformance.openid.ssf.conditions.streams;

import com.google.gson.JsonObject;
import net.openid.conformance.openid.ssf.eventstore.OIDSSFEventStore;
import net.openid.conformance.testmodule.Environment;

public abstract class AbstractOIDSSFGenerateStreamSET extends AbstractOIDSSFGenerateSET {

	public AbstractOIDSSFGenerateStreamSET(OIDSSFEventStore eventStore) {
		super(eventStore);
	}

	@Override
	protected String getCurrentStreamId(Environment env) {
		return env.getString("incoming_request", "body_json.stream_id");
	}

	protected JsonObject generateStreamSubject(String streamId) {
		JsonObject subject = new JsonObject();
		subject.addProperty("format", "opaque");
		subject.addProperty("id", streamId);
		return subject;
	}

}
