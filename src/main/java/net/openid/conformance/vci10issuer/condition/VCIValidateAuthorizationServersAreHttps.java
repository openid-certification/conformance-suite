package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.ArrayList;
import java.util.List;

/**
 * Each entry in {@code authorization_servers} (per OID4VCI 1.0 Final section 12.2.3) is an
 * authorization-server issuer identifier — equivalent to the RFC 8414 section 2 {@code issuer}
 * URL: HTTPS only, no fragment, no query. {@link VCIEnsureHttpsUrlsMetadata} only walks a
 * fixed list of single-string endpoint fields, so this array is not covered by it.
 */
public class VCIValidateAuthorizationServersAreHttps extends AbstractCondition {

	@Override
	@PreEnvironment(required = "vci")
	public Environment evaluate(Environment env) {

		JsonObject metadata = env.getElementFromObject("vci", "credential_issuer_metadata").getAsJsonObject();
		JsonElement authServersEl = metadata.get("authorization_servers");
		if (authServersEl == null) {
			logSuccess("credential_issuer_metadata has no authorization_servers entry (OPTIONAL); nothing to validate");
			return env;
		}
		if (!authServersEl.isJsonArray()) {
			throw error("authorization_servers is not a JSON array", args("authorization_servers", authServersEl));
		}

		JsonArray authServers = authServersEl.getAsJsonArray();
		if (authServers.isEmpty()) {
			throw error("authorization_servers is an empty array; if present it must contain at least one issuer identifier (OID4VCI 1.0 Final section 12.2.3)");
		}
		List<String> issues = new ArrayList<>();
		for (int i = 0; i < authServers.size(); i++) {
			JsonElement entry = authServers.get(i);
			if (!OIDFJSON.isString(entry)) {
				issues.add(String.format("authorization_servers[%d]: expected string, got %s", i, entry));
				continue;
			}
			VciIssuerUrlValidation.validate(OIDFJSON.getString(entry), String.format("authorization_servers[%d]", i), issues);
		}

		if (!issues.isEmpty()) {
			throw error("Invalid authorization_servers entry/entries", args("issues", issues));
		}

		logSuccess("All authorization_servers entries are valid HTTPS issuer URLs");
		return env;
	}
}
