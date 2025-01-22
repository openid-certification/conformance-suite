package net.openid.conformance.openid.federation;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class ExtractRegisteredClaimsFromFederationResponse extends AbstractCondition {

	@Override
	@PreEnvironment(required = "federation_response_jwt")
	public Environment evaluate(Environment env) {

		env.removeNativeValue("federation_response_iss");
		env.removeNativeValue("federation_response_sub");
		env.removeNativeValue("federation_response_iat");
		env.removeNativeValue("federation_response_exp");

		JsonElement issElement = env.getElementFromObject("federation_response_jwt", "claims.iss");
		JsonElement subElement = env.getElementFromObject("federation_response_jwt", "claims.sub");
		JsonElement iatElement = env.getElementFromObject("federation_response_jwt", "claims.iat");
		JsonElement expElement = env.getElementFromObject("federation_response_jwt", "claims.exp");

		String iss = issElement == null ? null : OIDFJSON.getString(issElement);
		String sub = subElement == null ? null : OIDFJSON.getString(subElement);
		Long iat = iatElement == null ? null : OIDFJSON.getLong(iatElement);
		Long exp = expElement == null ? null : OIDFJSON.getLong(expElement);

		if (iss != null) {
			env.putString("federation_response_iss", iss);
		}
		if (sub != null) {
			env.putString("federation_response_sub", sub);
		}
		if (iat != null) {
			env.putLong("federation_response_iat", iat);
		}
		if (exp != null) {
			env.putLong("federation_response_exp", exp);
		}

		if (iss == null || sub == null || iat == null || exp == null) {
			throw error("Missing registered claims in entity statement",
				args("iss", issElement, "sub", subElement, "iat", iatElement, "exp", expElement));
		}

		logSuccess("Extracted basic claims from entity statement", args("iss", iss, "sub", sub, "iat", iat, "exp", exp));

		return env;
	}

}
