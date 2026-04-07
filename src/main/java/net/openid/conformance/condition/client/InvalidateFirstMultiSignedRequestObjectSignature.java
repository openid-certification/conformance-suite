package net.openid.conformance.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.nimbusds.jose.util.Base64URL;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class InvalidateFirstMultiSignedRequestObjectSignature extends AbstractCondition {

	@Override
	@PreEnvironment(required = "request_object_json")
	@PostEnvironment(required = "request_object_json")
	public Environment evaluate(Environment env) {
		JsonObject requestObjectJson = env.getObject("request_object_json").deepCopy();

		JsonArray signatures = requestObjectJson.getAsJsonArray("signatures");
		if (signatures == null || signatures.size() < 2) {
			throw error("Expected at least two signatures in multi-signed request object",
				args("signature_count", signatures == null ? 0 : signatures.size()));
		}

		// Only corrupt the first signature, leaving the second valid
		JsonObject firstSig = signatures.get(0).getAsJsonObject();
		String signatureStr = OIDFJSON.getString(firstSig.get("signature"));

		byte[] bytes = Base64URL.from(signatureStr).decode();
		for (int j = 0; j < bytes.length; j++) {
			bytes[j] ^= 0x5A;
		}
		firstSig.addProperty("signature", Base64URL.encode(bytes).toString());

		env.putObject("request_object_json", requestObjectJson);

		logSuccess("Invalidated first signature in multi-signed request object, leaving second signature valid",
			args("signature_count", signatures.size(), "request_object_json", requestObjectJson));

		return env;
	}

}
