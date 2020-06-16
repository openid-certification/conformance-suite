package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jwt.SignedJWT;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.text.ParseException;

public class ChangeIdTokenToAlgNone extends AbstractCondition {
	private static final Base64URL ALG_NONE_HEADER = Base64URL.encode("{\"alg\": \"none\"}");

	@Override
	@PreEnvironment(required = "id_token")
	@PostEnvironment(required = "id_token")
	public Environment evaluate(Environment env) {
		String idToken = env.getString("id_token", "value");
		if (Strings.isNullOrEmpty(idToken)) {
			throw error("Couldn't find id_token");
		}
		try {
			SignedJWT idTokenParsed = SignedJWT.parse(idToken);
			Base64URL[] idTokenParsedParts = idTokenParsed.getParsedParts();
			String jwt =  ALG_NONE_HEADER + "." + idTokenParsedParts[1] + ".";
			JsonObject idTokenObj = env.getObject("id_token");
			idTokenObj.addProperty("value", jwt);
			logSuccess("Changed id_token to be 'signed' with 'alg: none'", args("id_token", jwt));
			return env;
		} catch (ParseException e) {
			throw error("Couldn't parse JWT", e, args("id_token", idToken));
		}
	}
}
