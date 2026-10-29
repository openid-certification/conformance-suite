package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * Uses the authorization_server of the credential offer's grant (OID4VCI 1.0 Final section 4.1.1)
 * to select the authorization server, replacing the one VCISelectOAuthorizationServer chose from
 * the metadata at configuration time.
 *
 * Only acts when 'Authorization Server URL Override' is empty in the test configuration (a configured one
 * is compared by {@link VCIEnsureCredentialOfferAuthorizationServerMatchesConfiguration}) and the
 * metadata lists several authorization servers, as section 4.1.1 says the parameter "MUST NOT be
 * used otherwise". A value that matches none of them stops the flow, per section 12.2.4: "the
 * Wallet MUST NOT proceed with the flow if the authorization_server Credential Offer parameter
 * value does not match any of the entries in the authorization_servers array."
 */
public class VCISelectAuthorizationServerFromCredentialOffer extends AbstractVCICredentialOfferAuthorizationServerCondition {

	private final String grantType;

	public VCISelectAuthorizationServerFromCredentialOffer(String grantType) {
		this.grantType = grantType;
	}

	@Override
	@PreEnvironment(required = {"vci", "server"})
	public Environment evaluate(Environment env) {
		String offered = authorizationServersInCredentialOffer(env).get(grantType);
		if (offered == null) {
			log("The credential offer does not contain authorization_server for the grant type used; keeping the selected authorization server",
				args("grant_type", grantType, "issuer", env.getString("server", "issuer")));
			return env;
		}

		String configured = configuredAuthorizationServer(env);
		if (configured != null) {
			log("'Authorization Server URL Override' is set in the test configuration, so it is used rather than the authorization_server in the credential offer",
				args("grant_type", grantType, "authorization_server", offered, "configured_authorization_server_url", configured));
			return env;
		}

		JsonArray listed = authorizationServersInMetadata(env);
		if (!listsMultipleAuthorizationServers(listed)) {
			log("authorization_server in the credential offer is not used, as the credential issuer metadata does not list multiple authorization servers",
				args("grant_type", grantType, "authorization_server", offered, "authorization_servers", listed));
			return env;
		}

		int index = indexOf(listed, offered);
		if (index < 0) {
			throw error("The authorization_server in the credential offer does not match any of the authorization servers in the credential issuer metadata, so the flow cannot proceed",
				args("grant_type", grantType, "authorization_server", offered, "authorization_servers", listed));
		}

		JsonElement serverData = env.getElementFromObject("vci", "authorization_servers.server" + index + ".authorization_server_metadata");
		if (serverData == null || !serverData.isJsonObject()) {
			throw error("No authorization server metadata found for the authorization server named in the credential offer",
				args("authorization_server", offered, "index", index));
		}

		if (serverData.getAsJsonObject().equals(env.getObject("server"))) {
			logSuccess("The authorization server named in the credential offer is the one already selected",
				args("grant_type", grantType, "authorization_server", offered));
			return env;
		}

		env.putObject("server", serverData.getAsJsonObject().deepCopy());
		logSuccess("Selected the authorization server named in the credential offer",
			args("grant_type", grantType, "authorization_server", offered, "authorization_server_metadata", serverData));
		return env;
	}
}
