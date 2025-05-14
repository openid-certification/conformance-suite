package net.openid.conformance.vciid2issuer.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.List;

public class VCICreateCredentialOffer extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		String credentialIssuer = env.getString("server", "discoveryIssuer");

		// TODO extract configurationIds from request
		JsonArray configurationIds = OIDFJSON.convertListToJsonArray(List.of());
		// TODO extract issuerState from request
		String issuerState = "1234";

		JsonObject credentialOffer = new JsonObject();
		credentialOffer.addProperty("credential_issuer", credentialIssuer);
		credentialOffer.add("credential_configuration_ids", configurationIds);

		JsonObject authorizationCodeObject = new JsonObject();
		authorizationCodeObject.addProperty("issuer_state", issuerState);

		JsonObject grantsObject = new JsonObject();
		grantsObject.add("authorization_code", authorizationCodeObject);

		credentialOffer.add("grants", grantsObject);

		// TODO handle credential offer
		/*
		credential_issuer: (use value from the test config)
credential_configuration_ids: accept set from the issuer (query parameter) and use it in the test, otherwise credential_id can come from the scopes or RAR
grants:
- authorization_code

issuer_state:
# authorization_server: use the instance we are "faking"
		 */

		logSuccess("Created Credential Offer", args("credential_offer", credentialOffer));


		return env;
	}
}
