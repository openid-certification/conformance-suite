package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class ValidateLogoutTokenFromBackchannelLogoutRequestEncryption extends AbstractVerifyJweEncryption {

	@Override
	@PreEnvironment(required = { "backchannel_logout_request", "client_jwks" })
	public Environment evaluate(Environment env) {

		JsonObject clientJwks = env.getObject("client_jwks");

		JsonElement tokenElement = env.getElementFromObject("backchannel_logout_request", "body_form_params.logout_token");
		if (tokenElement == null || !tokenElement.isJsonPrimitive()) {
			throw error("Couldn't find logout_token in backchannel_logout_request.body_form_params");
		}

		String idToken = OIDFJSON.getString(tokenElement);

		if (verifyJweEncryption(idToken, clientJwks, "logout_token")) {
			logSuccess("The client has a valid asymmetric key to decrypt the logout token");
		}
		else {
			logSuccess("The logout token is not encrypted using an asymmetric encryption algorithm");
		}

		return env;
	}

}
