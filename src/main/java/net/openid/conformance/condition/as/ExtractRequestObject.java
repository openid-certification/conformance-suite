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

public class ExtractRequestObject extends AbstractExtractRequestObject {

	@Override
	@PreEnvironment(required = {"authorization_endpoint_http_request_params", "client", "server_jwks"})
	@PostEnvironment(required = "authorization_request_object")
	public Environment evaluate(Environment env) {
		String requestObjectString = env.getString("authorization_endpoint_http_request_params", "request");
		processRequestObjectString(requestObjectString, env);
		return env;
	}

}
