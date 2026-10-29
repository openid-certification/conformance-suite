package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.Map;

/**
 * OID4VCI 1.0 Final section 4.1.1, authorization_server in either grant: "The value of this
 * parameter MUST match with one of the values in the authorization_servers array obtained from
 * the Credential Issuer metadata."
 *
 * When the metadata omits authorization_servers, section 12.2.4 makes the credential issuer its
 * own authorization server, so the credential issuer identifier is the only matching value.
 * Whether the parameter may be present at all in that case is checked separately by
 * {@link VCIEnsureCredentialOfferAuthorizationServerOnlyWithMultipleAuthorizationServers}.
 */
public class VCIEnsureCredentialOfferAuthorizationServerIsListedInMetadata extends AbstractVCICredentialOfferAuthorizationServerCondition {

	@Override
	@PreEnvironment(required = "vci")
	public Environment evaluate(Environment env) {
		Map<String, String> offered = authorizationServersInCredentialOffer(env);
		if (offered.isEmpty()) {
			logSuccess("The credential offer does not contain authorization_server in any grant");
			return env;
		}

		JsonArray inMetadata = authorizationServersInMetadata(env);
		JsonElement credentialIssuer = env.getElementFromObject("vci", "credential_issuer_metadata.credential_issuer");
		JsonArray listed = inMetadata;
		if (listed == null) {
			listed = new JsonArray();
			if (credentialIssuer != null && OIDFJSON.isString(credentialIssuer)) {
				listed.add(credentialIssuer);
			}
		}

		for (Map.Entry<String, String> entry : offered.entrySet()) {
			if (indexOf(listed, entry.getValue()) < 0) {
				throw error("The authorization_server in the credential offer does not match any of the authorization servers in the credential issuer metadata",
					args("grant_type", entry.getKey(), "authorization_server", entry.getValue(),
						"authorization_servers", inMetadata, "credential_issuer", credentialIssuer));
			}
		}

		logSuccess("Each authorization_server in the credential offer matches an authorization server in the credential issuer metadata",
			args("authorization_server", offered, "authorization_servers", listed));
		return env;
	}
}
