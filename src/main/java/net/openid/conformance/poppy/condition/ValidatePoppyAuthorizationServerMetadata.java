package net.openid.conformance.poppy.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.ArrayList;
import java.util.List;

public class ValidatePoppyAuthorizationServerMetadata extends AbstractCondition {

	@Override
	@PreEnvironment(required = "server", strings = {"poppy_issuer", "poppy_domain"})
	public Environment evaluate(Environment env) {
		JsonObject server = env.getObject("server");
		String issuer = env.getString("poppy_issuer");
		String domain = env.getString("poppy_domain");

		JsonElement metadataIssuer = server.get("issuer");
		if (metadataIssuer == null || !OIDFJSON.isString(metadataIssuer) || !issuer.equals(OIDFJSON.getString(metadataIssuer))) {
			throw error("The authorization server metadata 'issuer' does not exactly match the issuer found in the protected resource metadata",
				args("expected", issuer, "actual", metadataIssuer));
		}

		List<String> resources = stringList(server.get("protected_resources"));
		if (resources.stream().noneMatch(r -> PoppyUtil.isHttpsOriginOf(r, domain))) {
			throw error("The authorization server metadata 'protected_resources' does not include https://{organization.domain}, so the issuer does not claim this Company's domain",
				args("expected", "https://" + domain, "protected_resources", server.get("protected_resources")));
		}

		if (!server.has("grant_types_supported")) {
			throw error("The authorization server metadata has no 'grant_types_supported', which Poppy marks as Required",
				args("server", server));
		}
		List<String> grantTypes = stringList(server.get("grant_types_supported"));
		if (!grantTypes.contains("refresh_token")) {
			throw error("The authorization server metadata 'grant_types_supported' MUST include 'refresh_token'",
				args("grant_types_supported", server.get("grant_types_supported")));
		}
		if (!grantTypes.contains("authorization_code")) {
			throw error("The authorization server metadata 'grant_types_supported' does not include 'authorization_code', so the Company does not offer Direct Sign-In",
				args("grant_types_supported", server.get("grant_types_supported")));
		}
		if (grantTypes.contains("urn:ietf:params:oauth:grant-type:device_code") && !server.has("device_authorization_endpoint")) {
			throw error("The authorization server metadata advertises the device_code grant but has no 'device_authorization_endpoint', which Poppy requires in that case",
				args("grant_types_supported", server.get("grant_types_supported")));
		}

		for (String required : List.of("token_endpoint", "authorization_endpoint", "revocation_endpoint")) {
			if (!server.has(required)) {
				throw error("The authorization server metadata is missing '" + required + "', which Poppy requires for Direct Sign-In",
					args("missing", required));
			}
		}

		List<String> notHttps = new ArrayList<>();
		for (String key : server.keySet()) {
			if (key.equals("issuer") || key.equals("jwks_uri") || key.endsWith("_endpoint")) {
				JsonElement value = server.get(key);
				if (!OIDFJSON.isString(value) || !PoppyUtil.isHttpsUrl(OIDFJSON.getString(value))) {
					notHttps.add(key + " = " + value);
				}
			}
		}
		JsonElement protectedResources = server.get("protected_resources");
		if (protectedResources != null && protectedResources.isJsonArray()) {
			for (JsonElement entry : protectedResources.getAsJsonArray()) {
				if (!OIDFJSON.isString(entry) || !PoppyUtil.isHttpsUrl(OIDFJSON.getString(entry))) {
					notHttps.add("protected_resources entry " + entry);
				}
			}
		}
		if (!notHttps.isEmpty()) {
			throw error("The authorization server metadata contains URLs that are not HTTPS URLs; every URL the protocol defines MUST be an HTTPS URL",
				args("fields", notHttps));
		}

		logSuccess("The authorization server metadata is valid for Direct Sign-In");
		return env;
	}

	private static List<String> stringList(JsonElement element) {
		List<String> result = new ArrayList<>();
		if (element != null && element.isJsonArray()) {
			JsonArray array = element.getAsJsonArray();
			for (JsonElement entry : array) {
				if (OIDFJSON.isString(entry)) {
					result.add(OIDFJSON.getString(entry));
				}
			}
		}
		return result;
	}
}
