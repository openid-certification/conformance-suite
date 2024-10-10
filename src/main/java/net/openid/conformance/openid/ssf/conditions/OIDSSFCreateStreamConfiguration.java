package net.openid.conformance.openid.ssf.conditions;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.openid.ssf.support.StreamAdminClient.CreateStreamInput;
import net.openid.conformance.openid.ssf.support.StreamAdminClient.CreateStreamOutput;
import net.openid.conformance.testmodule.Environment;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.Set;

public class OIDSSFCreateStreamConfiguration extends OIDSSFAbstractStreamConfiguration {

	@PreEnvironment(required = {"config", "transmitter_metadata"})
	@Override
	public Environment evaluate(Environment env) {

		var streamAdmin = getStreamAdminClient(env);

		var input = new CreateStreamInput();
		input.eventsRequested = Set.of(
			"https://schemas.openid.net/secevent/caep/event-type/session-revoked",
			"https://schemas.openid.net/secevent/caep/event-type/credential-change",
			"https://schemas.openid.net/secevent/caep/event-type/device-compliance-change"
		);
		input.description = "Stream for Receiver OIDF Conformance Test-Suite";
		input.delivery = Map.of("method", "urn:ietf:rfc:8935", "endpoint_url", "https://receiver.example.com/events");
		input.audience = "https://localhost.emobix.co.uk:8443";

		ResponseEntity<CreateStreamOutput> response = streamAdmin.createStream(input);

		if (!response.getStatusCode().is2xxSuccessful()) {
			throw error("Stream creation failed", args("output", response.getBody()));
		}
		String streamId = response.getBody().streamId;
		logSuccess("Stream created", args("output", response.getBody(), "stream_id", streamId));

		return env;
	}
}
