package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonArray;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.Map;

/**
 * OID4VCI 1.0 Final section 4.1.1, authorization_server in either grant: "OPTIONAL string that the
 * Wallet can use to identify the Authorization Server to use with this grant type when
 * authorization_servers parameter in the Credential Issuer metadata has multiple entries. It MUST
 * NOT be used otherwise."
 *
 * Fails when an offer carries authorization_server although the metadata has no
 * authorization_servers, or only one entry.
 */
public class VCIEnsureCredentialOfferAuthorizationServerOnlyWithMultipleAuthorizationServers extends AbstractVCICredentialOfferAuthorizationServerCondition {

	@Override
	@PreEnvironment(required = "vci")
	public Environment evaluate(Environment env) {
		Map<String, String> offered = authorizationServersInCredentialOffer(env);
		if (offered.isEmpty()) {
			logSuccess("The credential offer does not contain authorization_server in any grant");
			return env;
		}

		JsonArray listed = authorizationServersInMetadata(env);
		if (listed == null) {
			throw error("The credential offer contains authorization_server, but the credential issuer metadata has no authorization_servers. "
					+ "authorization_server is only to be used when authorization_servers has multiple entries.",
				args("authorization_server", offered));
		}
		if (listed.size() < 2) {
			throw error("The credential offer contains authorization_server, but authorization_servers in the credential issuer metadata has fewer than two entries. "
					+ "authorization_server is only to be used when authorization_servers has multiple entries.",
				args("authorization_server", offered, "authorization_servers", listed));
		}

		logSuccess("The credential offer uses authorization_server and the credential issuer metadata lists multiple authorization servers",
			args("authorization_server", offered, "authorization_servers", listed));
		return env;
	}
}
