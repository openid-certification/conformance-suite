package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Access to the authorization_server parameters in the grants of the received credential offer
 * (OID4VCI 1.0 Final section 4.1.1) and to the authorization_servers of the credential issuer
 * metadata (section 12.2.4).
 */
public abstract class AbstractVCICredentialOfferAuthorizationServerCondition extends AbstractCondition {

	public static final String AUTHORIZATION_CODE_GRANT = "authorization_code";

	public static final String PRE_AUTHORIZED_CODE_GRANT = "urn:ietf:params:oauth:grant-type:pre-authorized_code";

	/**
	 * @return grant type to authorization_server, for each grant in the offer that has one
	 */
	protected Map<String, String> authorizationServersInCredentialOffer(Environment env) {
		Map<String, String> result = new LinkedHashMap<>();
		JsonElement grantsEl = env.getElementFromObject("vci", "credential_offer.grants");
		if (grantsEl == null || !grantsEl.isJsonObject()) {
			return result;
		}
		for (Map.Entry<String, JsonElement> grant : grantsEl.getAsJsonObject().entrySet()) {
			if (!grant.getValue().isJsonObject()) {
				continue;
			}
			JsonElement authorizationServer = grant.getValue().getAsJsonObject().get("authorization_server");
			if (authorizationServer == null) {
				continue;
			}
			if (!OIDFJSON.isString(authorizationServer)) {
				throw error("authorization_server in the credential offer is not a string",
					args("grant_type", grant.getKey(), "authorization_server", authorizationServer));
			}
			result.put(grant.getKey(), OIDFJSON.getString(authorizationServer));
		}
		return result;
	}

	/**
	 * @return the authorization_servers array of the credential issuer metadata, or null when absent
	 */
	protected JsonArray authorizationServersInMetadata(Environment env) {
		JsonElement el = env.getElementFromObject("vci", "credential_issuer_metadata.authorization_servers");
		if (el == null) {
			return null;
		}
		if (!el.isJsonArray()) {
			throw error("authorization_servers in the credential issuer metadata is not an array",
				args("authorization_servers", el));
		}
		return el.getAsJsonArray();
	}

	/**
	 * Section 4.1.1 only lets the offer's authorization_server be used when this is true.
	 */
	protected static boolean listsMultipleAuthorizationServers(JsonArray authorizationServers) {
		return authorizationServers != null && authorizationServers.size() > 1;
	}

	/**
	 * An emptied field in the configuration form is stored as an empty string.
	 */
	protected static String configuredAuthorizationServer(Environment env) {
		String configured = env.getString("config", "vci.authorization_server");
		return configured == null || configured.isEmpty() ? null : configured;
	}

	protected static int indexOf(JsonArray array, String value) {
		return array.asList().indexOf(new JsonPrimitive(value));
	}
}
