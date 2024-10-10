package net.openid.conformance.openid.ssf.conditions;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.openid.ssf.support.StreamAdminClient.QueryStreamOutput;
import net.openid.conformance.testmodule.Environment;
import org.springframework.http.ResponseEntity;

public class OIDSSFQueryStreamConfiguration extends OIDSSFAbstractStreamConfiguration {

	@PreEnvironment(required = {"config", "transmitter_metadata"})
	@Override
	public Environment evaluate(Environment env) {

		var streamAdmin = getStreamAdminClient(env);

		String streamId = env.getString("stream_id");
		ResponseEntity<QueryStreamOutput> response =
			streamId == null
				? streamAdmin.queryStreamConfig()
				: streamAdmin.queryStreamConfig(streamId);

		if (!response.getStatusCode().is2xxSuccessful()) {
			throw error("Could not find stream configuration", args("output", response.getBody()));
		}

		streamId = response.getBody().streamId;
		env.putString("stream_id", streamId);
		logSuccess("Stream configuration fetched", args("output", response.getBody(), "stream_id", streamId));

		return env;
	}
}
