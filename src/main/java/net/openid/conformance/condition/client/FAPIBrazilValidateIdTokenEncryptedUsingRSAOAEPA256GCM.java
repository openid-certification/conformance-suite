package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class FAPIBrazilValidateIdTokenEncryptedUsingRSAOAEPA256GCM extends AbstractCondition {

	@Override
	@PreEnvironment(required = "id_token")
	public Environment evaluate(Environment env) {
		JsonElement jweHeaderElement = env.getElementFromObject("id_token", "jwe_header");
		if (jweHeaderElement == null) {
			throw error("ID Token was not encrypted");
		}

		JsonObject jweHeader = jweHeaderElement.getAsJsonObject();
		String alg = OIDFJSON.getString(jweHeader.get("alg"));
		String enc = OIDFJSON.getString(jweHeader.get("enc"));

		if (!"RSA-OAEP".equals(alg)) {
			throw error("ID Token must be encrypted using RSA-OAEP algorithm", args("actual_alg", alg));
		}
		if (!"A256GCM".equals(enc)) {
			throw error("ID Token must be encrypted using A256GCM 'enc' value", args("actual_enc", enc));
		}

		logSuccess("ID Token was encrypted using RSA-OAEP and A256GCM", args("jwe_header", jweHeaderElement));
		return env;
	}

}
