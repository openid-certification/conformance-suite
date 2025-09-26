package net.openid.conformance.openid.ssf.conditions.streams;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.openid.ssf.SsfEvents;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.Set;

public class OIDSSFPrepareStreamConfigObjectAddRequestedEvents extends AbstractOIDSSFPrepareStreamConfigObject {

	public static final Set<String> DEFAULT_EVENTS = Set.of( //
		SsfEvents.CAEP_SESSION_REVOKED_EVENT_TYPE, //
		SsfEvents.CAEP_CREDENTIAL_CHANGE_EVENT_TYPE //
	);

	@Override
	@PreEnvironment(required = {"ssf"})
	public Environment evaluate(Environment env) {

		JsonObject streamConfig = getStreamConfig(env);

		Set<String> eventsRequested = getEventsRequested();
		streamConfig.add("events_requested", OIDFJSON.convertListToJsonArray(eventsRequested.stream().toList()));

		logSuccess("Added 'events_requested' to stream configuration", args("config", streamConfig, "events_requested", eventsRequested));

		return env;
	}

	protected Set<String> getEventsRequested() {
		return DEFAULT_EVENTS;
	}
}
