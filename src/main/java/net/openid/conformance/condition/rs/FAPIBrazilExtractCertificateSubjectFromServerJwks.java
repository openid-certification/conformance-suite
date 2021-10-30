package net.openid.conformance.condition.rs;

import com.google.common.base.Strings;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractFAPIBrazilExtractCertificateSubject;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class FAPIBrazilExtractCertificateSubjectFromServerJwks extends AbstractFAPIBrazilExtractCertificateSubject
{

	@Override
	@PreEnvironment(required = "server_jwks")
	@PostEnvironment(required = "rs_certificate_subject")
	public Environment evaluate(Environment env) {
		JsonElement keysElement = env.getElementFromObject("server_jwks", "keys");
		String certString = null;
		JsonArray keys = keysElement.getAsJsonArray();
		for(JsonElement keyElement : keys) {
			JsonObject key = keyElement.getAsJsonObject();
			String keyUse = OIDFJSON.getString(key.get("use"));
			if(keyUse==null || "sig".equals(keyUse)) {
				JsonElement x5cElement = key.get("x5c");
				if(x5cElement!=null && x5cElement.isJsonArray()){
					JsonArray x5cArray = x5cElement.getAsJsonArray();
					if(x5cArray.size()<1) {
						throw error("Signing key must contain the certificate in its x5c element", args("key", keyElement));
					}
					certString = OIDFJSON.getString(x5cArray.get(0));
					//Always uses the first key
					break;
				} else {
					throw error("Signing key must contain a x5c element (a json array) which contains the" +
						" certificate as the first element. You can use the server jwks from the example configuration provided at " +
						" https://gitlab.com/openid/conformance-suite/-/wikis/Brazil-RP-Testing-Instructions-and-Example-Configuration.",
						args("key", keyElement));
				}
			}
		}

		if (Strings.isNullOrEmpty(certString)) {
			throw error("Couldn't find organization signing certificate in server jwks. Server jwks must contain the organization certificate. " +
				"Please use the server jwks provided in example configuration.");
		}

		JsonObject certificateSubject = extractSubject(certString, "Server jwks must contain a BRCAC profile certificate where the subjectdn contains a UID");

		env.putObject("rs_certificate_subject", certificateSubject);

		logSuccess("Extracted subject from the certificate included in server jwks", certificateSubject);

		return env;
	}

}
