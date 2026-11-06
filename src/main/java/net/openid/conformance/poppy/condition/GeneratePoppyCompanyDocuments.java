package net.openid.conformance.poppy.condition;

import com.google.common.base.Strings;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.poppy.PoppyConstants;
import net.openid.conformance.testmodule.Environment;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Generates the documents the suite serves when it acts as a Company: poppy.json, the protected
 * resource metadata and the authorization server metadata.
 */
public class GeneratePoppyCompanyDocuments extends AbstractCondition {

	private static final Pattern HOST_NAME = Pattern.compile("[A-Za-z0-9]([A-Za-z0-9-]*[A-Za-z0-9])?(\\.[A-Za-z0-9]([A-Za-z0-9-]*[A-Za-z0-9])?)*");
	private static final String SCOPE = "orders:read";

	private static JsonArray array(String... values) {
		JsonArray array = new JsonArray();
		for (String value : values) {
			array.add(value);
		}
		return array;
	}

	@Override
	@PreEnvironment(required = "config", strings = "base_url")
	@PostEnvironment(required = {"poppy_json", "poppy_protected_resource_metadata", "server"}, strings = {"issuer", "poppy_domain"})
	public Environment evaluate(Environment env) {
		String configured = env.getString("config", "poppy.company_domain");
		if (Strings.isNullOrEmpty(configured) || configured.isBlank()) {
			throw error("'Company domain' field is missing from the 'Poppy' section in the test configuration");
		}
		String trimmed = configured.trim();
		if (!HOST_NAME.matcher(trimmed).matches()) {
			throw error("'Company domain' in the 'Poppy' section of the test configuration must be only a host name, with no scheme, port, path, query or fragment",
				args("company_domain", configured));
		}
		String domain = trimmed.toLowerCase(Locale.ROOT);

		String issuer = env.getString("base_url");
		while (issuer.endsWith("/")) {
			issuer = issuer.substring(0, issuer.length() - 1);
		}
		String origin = "https://" + domain;

		JsonObject organization = new JsonObject();
		organization.addProperty("name", "OpenID Foundation conformance suite");
		organization.addProperty("domain", domain);

		JsonObject scopes = new JsonObject();
		scopes.addProperty(SCOPE, "See your orders");
		JsonObject hint = new JsonObject();
		hint.addProperty("changes", "none");
		hint.addProperty("personal_data", true);
		JsonObject scopeHints = new JsonObject();
		scopeHints.add(SCOPE, hint);
		JsonObject auth = new JsonObject();
		auth.add("scopes", scopes);
		auth.add("scope_hints", scopeHints);

		JsonObject api = new JsonObject();
		api.addProperty("type", "openapi");
		api.addProperty("url", issuer + "/openapi.json");
		api.addProperty("description", "Conformance suite test API");
		JsonArray apis = new JsonArray();
		apis.add(api);

		JsonObject poppyJson = new JsonObject();
		poppyJson.addProperty("protocol_version", PoppyConstants.PROTOCOL_VERSION);
		poppyJson.add("organization", organization);
		poppyJson.add("auth", auth);
		poppyJson.add("apis", apis);

		JsonObject resourceMetadata = new JsonObject();
		resourceMetadata.addProperty("resource", origin);
		resourceMetadata.add("authorization_servers", array(issuer));

		JsonObject server = new JsonObject();
		server.addProperty("issuer", issuer);
		server.addProperty("authorization_endpoint", issuer + "/authorize");
		server.addProperty("token_endpoint", issuer + "/token");
		server.addProperty("revocation_endpoint", issuer + "/revoke");
		server.add("grant_types_supported", array("authorization_code", "refresh_token"));
		server.add("scopes_supported", array(SCOPE));
		server.add("protected_resources", array(origin));
		server.add("token_endpoint_auth_methods_supported", array("private_key_jwt"));
		server.add("token_endpoint_auth_signing_alg_values_supported", array("ES256", "PS384"));
		server.add("code_challenge_methods_supported", array("S256"));
		server.add("response_types_supported", array("code"));

		env.putString("issuer", issuer);
		env.putString("poppy_domain", domain);
		env.putObject("poppy_json", poppyJson);
		env.putObject("poppy_protected_resource_metadata", resourceMetadata);
		env.putObject("server", server);

		logSuccess("Generated the Company documents", args("issuer", issuer, "poppy_domain", domain,
			"poppy_json", poppyJson, "poppy_protected_resource_metadata", resourceMetadata, "server", server));
		return env;
	}
}
