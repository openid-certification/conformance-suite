package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.testmodule.Environment;

/**
 * Builds the environment the credential offer authorization_server conditions read.
 */
final class VCICredentialOfferAuthorizationServerTestEnvironment {

	static final String ISSUER = "https://issuer.example.com";
	static final String AS1 = "https://as1.example.com";
	static final String AS2 = "https://as2.example.com";

	private VCICredentialOfferAuthorizationServerTestEnvironment() {
	}

	/**
	 * @param authorizationServersJson JSON array for authorization_servers, or null to omit it
	 * @param grantsJson JSON object for the offer's grants
	 */
	static Environment env(String authorizationServersJson, String grantsJson) {
		Environment env = new Environment();
		JsonObject metadata = new JsonObject();
		metadata.addProperty("credential_issuer", ISSUER);
		if (authorizationServersJson != null) {
			metadata.add("authorization_servers", JsonParser.parseString(authorizationServersJson));
		}
		env.putObject("vci", "credential_issuer_metadata", metadata);

		JsonObject offer = new JsonObject();
		offer.addProperty("credential_issuer", ISSUER);
		offer.add("grants", JsonParser.parseString(grantsJson));
		env.putObject("vci", "credential_offer", offer);

		env.putObject("config", new JsonObject());
		return env;
	}

	static String authCodeGrant(String authorizationServer) {
		return "{\"authorization_code\": {\"authorization_server\": \"" + authorizationServer + "\"}}";
	}

	static String preAuthGrant(String authorizationServer) {
		return "{\"urn:ietf:params:oauth:grant-type:pre-authorized_code\": {\"pre-authorized_code\": \"abc\", \"authorization_server\": \""
			+ authorizationServer + "\"}}";
	}
}
