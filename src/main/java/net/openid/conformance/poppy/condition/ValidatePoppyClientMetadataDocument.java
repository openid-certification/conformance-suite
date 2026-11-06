package net.openid.conformance.poppy.condition;

import com.google.common.net.InetAddresses;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

public class ValidatePoppyClientMetadataDocument extends AbstractCondition {

	private static String stringMember(JsonObject doc, String name) {
		JsonElement element = doc.get(name);
		if (element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
			return OIDFJSON.getString(element);
		}
		return null;
	}

	private static String hostOf(String url) {
		try {
			String host = new URI(url).getHost();
			return host == null ? null : host.toLowerCase(Locale.ROOT);
		} catch (URISyntaxException e) {
			return null;
		}
	}

	/**
	 * RFC 8252 section 7.3: a loopback redirect URI uses http with an IP literal, not the name localhost.
	 */
	private static boolean isLoopbackRedirect(String value) {
		try {
			URI uri = new URI(value);
			String host = uri.getHost();
			if (!"http".equals(uri.getScheme()) || host == null) {
				return false;
			}
			if (host.startsWith("[") && host.endsWith("]")) {
				host = host.substring(1, host.length() - 1);
			}
			return InetAddresses.isInetAddress(host) && InetAddresses.forString(host).isLoopbackAddress();
		} catch (URISyntaxException e) {
			return false;
		}
	}

	@Override
	@PreEnvironment(required = "client", strings = "poppy_incoming_client_id")
	public Environment evaluate(Environment env) {
		JsonObject doc = env.getObject("client");
		String expectedClientId = env.getString("poppy_incoming_client_id");

		String clientId = stringMember(doc, "client_id");
		if (clientId == null || !clientId.equals(expectedClientId)) {
			throw error("The client metadata document's client_id MUST equal the URL the document was fetched from",
				args("client_id", clientId, "fetched_from", expectedClientId));
		}
		String clientHost = hostOf(clientId);
		if (clientHost == null) {
			throw error("The client_id is not a URL with a host", args("client_id", clientId));
		}

		String jwksUri = stringMember(doc, "jwks_uri");
		if (jwksUri == null) {
			throw error("The client metadata document has no jwks_uri; the Personal Agent's keys are published there");
		}
		if (!PoppyUtil.isHttpsUrl(jwksUri) || !clientHost.equals(hostOf(jwksUri))) {
			throw error("The client metadata document's jwks_uri MUST be an HTTPS URL on the same domain as client_id",
				args("jwks_uri", jwksUri, "client_id", clientId));
		}

		JsonElement redirectUris = doc.get("redirect_uris");
		if (redirectUris == null || !redirectUris.isJsonArray() || redirectUris.getAsJsonArray().isEmpty()) {
			throw error("The client metadata document has no redirect_uris; they list where Direct Sign-In may send the User back to",
				args("redirect_uris", redirectUris));
		}
		JsonArray uris = redirectUris.getAsJsonArray();
		for (JsonElement entry : uris) {
			String uri = entry.isJsonPrimitive() && entry.getAsJsonPrimitive().isString() ? OIDFJSON.getString(entry) : null;
			boolean sameDomainHttps = uri != null && PoppyUtil.isHttpsUrl(uri) && clientHost.equals(hostOf(uri));
			if (!sameDomainHttps && (uri == null || !isLoopbackRedirect(uri))) {
				throw error("Each redirect_uris entry MUST be an HTTPS URL on the same domain as client_id, or a loopback redirect URI with an IP literal host (RFC 8252 section 7.3)",
					args("redirect_uri", entry, "client_id", clientId));
			}
		}

		JsonElement authMethod = doc.get("token_endpoint_auth_method");
		if (authMethod != null && !(authMethod.isJsonPrimitive() && "private_key_jwt".equals(stringMember(doc, "token_endpoint_auth_method")))) {
			throw error("The client metadata document's token_endpoint_auth_method, when present, MUST be private_key_jwt",
				args("token_endpoint_auth_method", authMethod));
		}

		logSuccess("The client metadata document satisfies the Poppy requirements", args("client", doc));
		return env;
	}
}
