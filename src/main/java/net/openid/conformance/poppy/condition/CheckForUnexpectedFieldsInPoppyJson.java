package net.openid.conformance.poppy.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class CheckForUnexpectedFieldsInPoppyJson extends AbstractCheckForUnexpectedPoppyFields {

	private static final Set<String> TOP_LEVEL = Set.of("protocol_version", "supported_versions", "organization", "auth",
		"agent", "apis", "web", "feedback", "excluded_tasks", "skill_index", "extensions");
	private static final Set<String> ORGANIZATION = Set.of("name", "domain", "logo_uri");
	private static final Set<String> AUTH = Set.of("issuer", "disabled", "scopes", "scope_hints");
	private static final Set<String> WEB = Set.of("browser_session_endpoint", "disabled", "unscoped");
	private static final Set<String> AGENT = Set.of("protocols");
	private static final Set<String> AGENT_PROTOCOL = Set.of("type", "endpoint", "access");
	private static final Set<String> API = Set.of("type", "url", "description");
	private static final Set<String> FEEDBACK = Set.of("endpoint");
	private static final Set<String> EXCLUDED_TASKS = Set.of("url");

	@Override
	@PreEnvironment(required = "poppy_json")
	public Environment evaluate(Environment env) {
		JsonObject json = env.getObject("poppy_json");
		List<String> unexpected = new ArrayList<>();
		checkObject(unexpected, "", json, TOP_LEVEL);
		checkObject(unexpected, "organization", child(json, "organization"), ORGANIZATION);
		checkObject(unexpected, "auth", child(json, "auth"), AUTH);
		checkObject(unexpected, "web", child(json, "web"), WEB);
		checkObject(unexpected, "agent", child(json, "agent"), AGENT);
		if (child(json, "agent") != null && child(json, "agent").isJsonObject()) {
			checkArrayEntries(unexpected, "agent.protocols", child(json, "agent").getAsJsonObject().get("protocols"), AGENT_PROTOCOL);
		}
		checkArrayEntries(unexpected, "apis", child(json, "apis"), API);
		checkObject(unexpected, "feedback", child(json, "feedback"), FEEDBACK);
		checkObject(unexpected, "excluded_tasks", child(json, "excluded_tasks"), EXCLUDED_TASKS);
		if (!unexpected.isEmpty()) {
			throw error("poppy.json contains fields the Poppy 0.4 specification does not define", args("unexpected_fields", unexpected));
		}
		logSuccess("poppy.json contains only fields defined by Poppy 0.4");
		return env;
	}
}
