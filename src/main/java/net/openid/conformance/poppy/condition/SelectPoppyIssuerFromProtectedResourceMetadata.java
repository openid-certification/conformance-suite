package net.openid.conformance.poppy.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.ArrayList;
import java.util.List;

public class SelectPoppyIssuerFromProtectedResourceMetadata extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"poppy_protected_resource_metadata", "poppy_json"}, strings = "poppy_domain")
	@PostEnvironment(strings = "poppy_issuer")
	public Environment evaluate(Environment env) {
		JsonObject metadata = env.getObject("poppy_protected_resource_metadata");
		String domain = env.getString("poppy_domain");
		String expectedResource = "https://" + domain;

		JsonElement resource = metadata.get("resource");
		if (resource == null || !OIDFJSON.isString(resource) || !PoppyUtil.isHttpsOriginOf(OIDFJSON.getString(resource), domain)) {
			throw error("The protected resource metadata 'resource' MUST be exactly https://{organization.domain}",
				args("expected", expectedResource, "actual", resource));
		}

		List<String> notHttps = new ArrayList<>();
		for (String key : metadata.keySet()) {
			if (key.equals("jwks_uri") || key.endsWith("_endpoint")) {
				JsonElement value = metadata.get(key);
				if (!OIDFJSON.isString(value) || !PoppyUtil.isHttpsUrl(OIDFJSON.getString(value))) {
					notHttps.add(key + " = " + value);
				}
			}
		}

		JsonElement servers = metadata.get("authorization_servers");
		List<String> issuers = new ArrayList<>();
		if (servers != null) {
			if (!servers.isJsonArray()) {
				throw error("The protected resource metadata 'authorization_servers' is not an array", args("authorization_servers", servers));
			}
			JsonArray array = servers.getAsJsonArray();
			for (JsonElement entry : array) {
				if (!OIDFJSON.isString(entry) || !PoppyUtil.isHttpsUrl(OIDFJSON.getString(entry))) {
					notHttps.add("authorization_servers entry " + entry);
				} else {
					issuers.add(OIDFJSON.getString(entry));
				}
			}
		}
		if (!notHttps.isEmpty()) {
			throw error("The protected resource metadata contains URLs that are not HTTPS URLs; every URL the protocol defines MUST be an HTTPS URL",
				args("fields", notHttps));
		}
		if (issuers.isEmpty()) {
			throw error("The protected resource metadata lists no authorization servers, so the Company offers no sign-in. This test exercises Direct Sign-In.",
				args("metadata", metadata));
		}

		String authIssuer = env.getString("poppy_json", "auth.issuer");
		String selected;
		if (authIssuer == null) {
			if (issuers.size() > 1) {
				throw error("The protected resource metadata lists more than one authorization server, so poppy.json MUST name the one Personal Agents use in auth.issuer, but it has none",
					args("authorization_servers", issuers));
			}
			selected = issuers.get(0);
		} else {
			if (!issuers.contains(authIssuer)) {
				throw error("poppy.json auth.issuer is not in the protected resource metadata 'authorization_servers'",
					args("auth_issuer", authIssuer, "authorization_servers", issuers));
			}
			selected = authIssuer;
		}
		env.putString("poppy_issuer", selected);
		logSuccess("Selected the Company's issuer", args("poppy_issuer", selected));
		return env;
	}
}
