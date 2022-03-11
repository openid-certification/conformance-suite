package net.openid.conformance.fapiciba.rp;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class FAPIBrazilEnsureBackchannelRequestObjectEncryptedUsingRSAOAEPA256GCM extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"backchannel_request_object"})
	public Environment evaluate(Environment env) {
		JsonElement jweHeaderElement = env.getElementFromObject("backchannel_request_object", "jwe_header");
		if(jweHeaderElement == null) {
			throw error("Request object was not encrypted");
		} else {
			JsonObject jweHeader = jweHeaderElement.getAsJsonObject();
			String alg = OIDFJSON.getString(jweHeader.get("alg"));
			String enc = OIDFJSON.getString(jweHeader.get("enc"));
			if(!"RSA-OAEP".equals(alg)) {
				throw error("Request object must be encrypted using RSA-OAEP algorithm", args("actual_alg", alg));
			}
			if(!"A256GCM".equals(enc)) {
				throw error("Request object must be encrypted using A256GCM 'enc' value", args("actual_enc", enc));
			}
			logSuccess("Request object was encrypted using RSA-OAEP and A256GCM", args("jwe_header", jweHeaderElement));
		}
		return env;
	}

}
