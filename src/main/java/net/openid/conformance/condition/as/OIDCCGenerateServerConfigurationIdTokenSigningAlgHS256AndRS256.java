package net.openid.conformance.condition.as;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class OIDCCGenerateServerConfigurationIdTokenSigningAlgHS256AndRS256 extends OIDCCGenerateServerConfiguration {

	@Override
	protected void addIdTokenSigningAlgValuesSupported(JsonObject server) {
		JsonArray values = new JsonArray();
		values.add("HS256");
		// RS256 MUST be included as per OIDCD-3, even though this test only ever signs with HS256
		values.add("RS256");
		server.add("id_token_signing_alg_values_supported", values);
	}
}
