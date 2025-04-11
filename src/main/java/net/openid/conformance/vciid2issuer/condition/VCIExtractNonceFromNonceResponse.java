package net.openid.conformance.vciid2issuer.condition;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class VCIExtractNonceFromNonceResponse extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		String nonceResponseJson = OIDFJSON.getString(env.getElementFromObject("endpoint_response", "body"));
		JsonObject nonceResponseObject = JsonParser.parseString(nonceResponseJson).getAsJsonObject();
		if (!nonceResponseObject.has("c_nonce")) {
			throw error("Could not find c_nonce in NonceResponse", args("nonce_response", nonceResponseObject));
		}

		String cnonce = OIDFJSON.getString(nonceResponseObject.get("c_nonce"));

		env.putString("vci", "c_nonce", cnonce);

		logSuccess("Extracted nonce from NonceResponse", args("nonce", cnonce));
		return env;
	}
}
