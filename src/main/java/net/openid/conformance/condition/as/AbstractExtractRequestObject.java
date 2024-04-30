package net.openid.conformance.condition.as;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import com.nimbusds.jose.JOSEException;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.JWTUtil;

import java.text.ParseException;

public abstract class AbstractExtractRequestObject extends AbstractCondition {

	public Environment processRequestObjectString(String requestObjectString, Environment env) {

		if (Strings.isNullOrEmpty(requestObjectString)) {
			throw error("Could not find request object in request parameters");
		}

		try {
			JsonObject client = env.getObject("client");
			JsonObject serverEncKeys = env.getObject("server_encryption_keys");
			JsonObject jsonObjectForJwt = JWTUtil.jwtStringToJsonObjectForEnvironment(requestObjectString, client, serverEncKeys);

			if(jsonObjectForJwt==null) {
				throw error("Couldn't extract request object", args("request", requestObjectString));
			}
			env.putObject("authorization_request_object", jsonObjectForJwt);

			logSuccess("Parsed request object", args("request_object", jsonObjectForJwt));

			return env;

		} catch (ParseException e) {
			throw error("Couldn't parse request object: " + e.getMessage(), e, args("request", requestObjectString));
		} catch (JOSEException e) {
			throw error("Request object decryption failed", e, args("request", requestObjectString));
		}
	}

}
