package net.openid.conformance.openid.ssf.conditions;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;

public class OIDSSFDeleteStreamConfiguration extends OIDSSFAbstractStreamConfiguration {

	@PreEnvironment(required = {"config", "transmitter_metadata"})
	@Override
	public Environment evaluate(Environment env) {

		var streamAdmin = getStreamAdminClient(env);

		String streamId = env.getString("stream_id");
		ResponseEntity<?> response = streamAdmin.deleteStream(streamId);

		HttpStatusCode statusCode = response.getStatusCode();
		if (!statusCode.is2xxSuccessful()) {
			throw error("Could not delete stream", args("status_code", statusCode.value(), "stream_id", streamId));
		}

		if (statusCode.value() != 204) {
			log("Delete Stream returns wrong status code, should be 204", args("status_code", statusCode.value()));
		}
		logSuccess("Stream deleted", args("status_code", statusCode.value(), "stream_id", streamId));

		return env;
	}
}
