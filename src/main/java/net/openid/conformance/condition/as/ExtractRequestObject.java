package net.openid.conformance.condition.as;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.KeyLengthException;
import com.nimbusds.jwt.JWT;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.JWTUtil;

import java.text.ParseException;

public class ExtractRequestObject extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"authorization_endpoint_http_request_params", "client", "server_jwks"})
	@PostEnvironment(required = "authorization_request_object")
	public Environment evaluate(Environment env) {
		String requestObjectString = env.getString("authorization_endpoint_http_request_params", "request");

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
			throw error("Couldn't parse request object", e, args("request", requestObjectString));
		} catch (JOSEException e) {
			throw error("Request object decryption failed", e, args("request", requestObjectString));
		}


	}

}
