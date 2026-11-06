package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonArray;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * When 'Authorization Server URL Override' is set in the test configuration, the test uses that
 * authorization server. If the authorization_server the credential offer gives for the grant the
 * test uses (OID4VCI 1.0 Final section 4.1.1) names another one, the issuer wants the wallet to use
 * a different authorization server than the test is configured for.
 *
 * Only applies when authorization_servers in the metadata has multiple entries: otherwise
 * section 4.1.1 says the parameter "MUST NOT be used".
 */
public class VCIEnsureCredentialOfferAuthorizationServerMatchesConfiguration extends AbstractVCICredentialOfferAuthorizationServerCondition {

	private final String grantType;

	public VCIEnsureCredentialOfferAuthorizationServerMatchesConfiguration(String grantType) {
		this.grantType = grantType;
	}

	@Override
	@PreEnvironment(required = {"vci", "config"})
	public Environment evaluate(Environment env) {
		String configured = configuredAuthorizationServer(env);
		if (configured == null) {
			logSuccess("No 'Authorization Server URL Override' in the test configuration to compare with the credential offer");
			return env;
		}

		String offered = authorizationServersInCredentialOffer(env).get(grantType);
		if (offered == null) {
			logSuccess("The credential offer does not contain authorization_server for the grant type used",
				args("grant_type", grantType));
			return env;
		}

		JsonArray listed = authorizationServersInMetadata(env);
		if (!listsMultipleAuthorizationServers(listed)) {
			logSuccess("authorization_server in the credential offer is not used, as the credential issuer metadata does not list multiple authorization servers",
				args("grant_type", grantType, "authorization_server", offered, "authorization_servers", listed));
			return env;
		}

		if (!offered.equals(configured)) {
			throw error("The credential offer asks for a different authorization server than the 'Authorization Server URL Override' in the test configuration. "
					+ "Change 'Authorization Server URL Override' to the authorization server named in the offer.",
				args("grant_type", grantType, "credential_offer_authorization_server", offered,
					"configured_authorization_server_url", configured));
		}

		logSuccess("authorization_server in the credential offer matches the 'Authorization Server URL Override' in the test configuration",
			args("grant_type", grantType, "authorization_server", offered));
		return env;
	}
}
