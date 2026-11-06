package net.openid.conformance.poppy.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ValidatePoppyJsonCoreFields extends AbstractCondition {

	@Override
	@PreEnvironment(required = "poppy_json", strings = "poppy_json_final_url")
	@PostEnvironment(strings = "poppy_domain")
	public Environment evaluate(Environment env) {
		JsonObject json = env.getObject("poppy_json");

		JsonElement version = json.get("protocol_version");
		if (version == null || !OIDFJSON.isString(version)) {
			throw error("poppy.json has no 'protocol_version' string", args("poppy_json", json));
		}
		if (!"0.4".equals(OIDFJSON.getString(version))) {
			throw error("poppy.json 'protocol_version' is not 0.4, the version this test implements",
				args("protocol_version", OIDFJSON.getString(version)));
		}

		JsonElement organization = json.get("organization");
		if (organization == null || !organization.isJsonObject()) {
			throw error("poppy.json has no 'organization' object", args("poppy_json", json));
		}
		JsonElement nameElement = organization.getAsJsonObject().get("name");
		if (nameElement == null || !OIDFJSON.isString(nameElement) || OIDFJSON.getString(nameElement).isBlank()) {
			throw error("poppy.json 'organization.name' is missing, empty or not a string", args("organization", organization));
		}
		JsonElement domainElement = organization.getAsJsonObject().get("domain");
		if (domainElement == null || !OIDFJSON.isString(domainElement)) {
			throw error("poppy.json 'organization.domain' is missing or not a string", args("organization", organization));
		}
		String domain = OIDFJSON.getString(domainElement);
		if (domain.contains(":") || domain.contains("/")) {
			throw error("poppy.json 'organization.domain' must be a host name only, with no port, path or scheme",
				args("domain", domain));
		}
		URI finalUrl = URI.create(env.getString("poppy_json_final_url"));
		String finalHost = finalUrl.getHost() == null ? "" : finalUrl.getHost().toLowerCase(Locale.ROOT);
		if (!finalHost.equals(domain.toLowerCase(Locale.ROOT))) {
			throw error("poppy.json 'organization.domain' is not the host that served the document, after any redirects",
				args("domain", domain, "served_by", finalHost, "final_url", finalUrl.toString()));
		}

		if (!json.has("agent") && !json.has("apis") && !json.has("web")) {
			throw error("poppy.json has none of 'agent', 'apis' or 'web'; at least one is required", args("poppy_json", json));
		}

		List<String> notHttps = new ArrayList<>();
		collect(notHttps, "organization.logo_uri", organization.getAsJsonObject().get("logo_uri"));
		collect(notHttps, "auth.issuer", nested(json, "auth", "issuer"));
		collect(notHttps, "web.browser_session_endpoint", nested(json, "web", "browser_session_endpoint"));
		collect(notHttps, "feedback.endpoint", nested(json, "feedback", "endpoint"));
		collect(notHttps, "excluded_tasks.url", nested(json, "excluded_tasks", "url"));
		JsonElement apis = json.get("apis");
		if (apis != null && apis.isJsonArray()) {
			JsonArray array = apis.getAsJsonArray();
			for (int i = 0; i < array.size(); i++) {
				if (array.get(i).isJsonObject()) {
					collect(notHttps, "apis[" + i + "].url", array.get(i).getAsJsonObject().get("url"));
				}
			}
		}
		JsonElement protocols = nested(json, "agent", "protocols");
		if (protocols != null && protocols.isJsonArray()) {
			JsonArray array = protocols.getAsJsonArray();
			for (int i = 0; i < array.size(); i++) {
				if (array.get(i).isJsonObject()) {
					collect(notHttps, "agent.protocols[" + i + "].endpoint", array.get(i).getAsJsonObject().get("endpoint"));
				}
			}
		}
		if (!notHttps.isEmpty()) {
			throw error("poppy.json contains URLs that are not HTTPS URLs; every URL the protocol defines MUST be an HTTPS URL",
				args("fields", notHttps));
		}

		env.putString("poppy_domain", finalHost);
		logSuccess("poppy.json core fields are valid", args("poppy_domain", finalHost));
		return env;
	}

	private static JsonElement nested(JsonObject json, String parent, String child) {
		JsonElement p = json.get(parent);
		return p != null && p.isJsonObject() ? p.getAsJsonObject().get(child) : null;
	}

	private static void collect(List<String> problems, String name, JsonElement value) {
		if (value == null) {
			return;
		}
		if (!OIDFJSON.isString(value) || !PoppyUtil.isHttpsUrl(OIDFJSON.getString(value))) {
			problems.add(name + " = " + value);
		}
	}
}
