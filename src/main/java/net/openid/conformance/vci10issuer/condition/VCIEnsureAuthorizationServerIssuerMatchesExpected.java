package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

/**
 * Verifies that the {@code issuer} in a fetched authorization server metadata document is identical
 * to the authorization server identifier the document was retrieved for. RFC 8414 §3.3 requires the
 * returned {@code issuer} to match the identifier into which the well-known URI string was inserted
 * to build the metadata URL; otherwise a malicious or misconfigured server could claim a different
 * identity than the one the wallet located, enabling impersonation.
 *
 * <p>The trusted identifier comes from the credential issuer metadata: the
 * {@code authorization_servers[serverIndex]} entry, or - when no {@code authorization_servers} list
 * is present - the {@code credential_issuer} itself (OID4VCI uses the credential issuer as the
 * authorization server in that case). It must NOT be taken from the fetched {@code server.issuer},
 * which is the value under test.
 */
public class VCIEnsureAuthorizationServerIssuerMatchesExpected extends AbstractCondition {

	private final int serverIndex;

	public VCIEnsureAuthorizationServerIssuerMatchesExpected(int serverIndex) {
		this.serverIndex = serverIndex;
	}

	@Override
	@PreEnvironment(required = {"vci", "server"})
	public Environment evaluate(Environment env) {

		JsonObject credentialIssuerMetadata =
			env.getElementFromObject("vci", "credential_issuer_metadata").getAsJsonObject();
		String expectedIssuer = expectedAuthorizationServerIssuer(credentialIssuerMetadata);

		JsonElement issuerEl = env.getElementFromObject("server", "issuer");
		if (!OIDFJSON.isString(issuerEl)) {
			throw error("issuer is missing or not a string in the authorization server metadata",
				args("issuer", issuerEl));
		}
		String actualIssuer = OIDFJSON.getString(issuerEl);

		if (!stripTrailingSlash(actualIssuer).equals(stripTrailingSlash(expectedIssuer))) {
			throw error("The issuer in the authorization server metadata is not identical to the "
					+ "authorization server identifier the metadata was retrieved for. RFC 8414 §3.3 "
					+ "requires these to match to prevent impersonation attacks.",
				args("issuer", actualIssuer, "expected_issuer", expectedIssuer));
		}

		logSuccess("Authorization server metadata issuer matches the identifier it was retrieved for",
			args("issuer", actualIssuer));
		return env;
	}

	private String expectedAuthorizationServerIssuer(JsonObject credentialIssuerMetadata) {
		JsonElement authorizationServersEl = credentialIssuerMetadata.get("authorization_servers");
		if (authorizationServersEl != null && authorizationServersEl.isJsonArray()) {
			JsonArray authorizationServers = authorizationServersEl.getAsJsonArray();
			if (serverIndex < 0 || serverIndex >= authorizationServers.size()
					|| !OIDFJSON.isString(authorizationServers.get(serverIndex))) {
				throw error("Could not determine the expected authorization server issuer from the "
						+ "authorization_servers list in the credential issuer metadata",
					args("index", serverIndex, "authorization_servers", authorizationServersEl));
			}
			return OIDFJSON.getString(authorizationServers.get(serverIndex));
		}
		// No authorization_servers list: the credential issuer is the authorization server.
		JsonElement credentialIssuerEl = credentialIssuerMetadata.get("credential_issuer");
		if (!OIDFJSON.isString(credentialIssuerEl)) {
			throw error("credential_issuer is missing or not a string in the credential issuer metadata",
				args("credential_issuer", credentialIssuerEl));
		}
		return OIDFJSON.getString(credentialIssuerEl);
	}

	private static String stripTrailingSlash(String url) {
		return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
	}
}
