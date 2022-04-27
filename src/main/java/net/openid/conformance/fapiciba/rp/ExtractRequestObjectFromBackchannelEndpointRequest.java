package net.openid.conformance.fapiciba.rp;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.JWTUtil;

import java.text.ParseException;

public class ExtractRequestObjectFromBackchannelEndpointRequest extends AbstractCondition
{
	@Override
	@PreEnvironment(required = {"backchannel_endpoint_http_request", "client"})
	@PostEnvironment(required = "backchannel_request_object")
	public Environment evaluate(Environment env) {
		String requestObjectString = env.getString("backchannel_endpoint_http_request", "body_form_params.request");
		processRequestObjectString(requestObjectString, env);

		return env;
	}

	public Environment processRequestObjectString(String requestObjectString, Environment env) {

		if (Strings.isNullOrEmpty(requestObjectString)) {
			throw error("Could not find request object in request parameters");
		}

		try {
			JsonObject jsonObjectForJwt = JWTUtil.jwtStringToJsonObjectForEnvironment(requestObjectString);

			if(jsonObjectForJwt == null) {
				throw error("Couldn't extract request object", args("request", requestObjectString));
			}
			env.putObject("backchannel_request_object", jsonObjectForJwt);

			logSuccess("Parsed request object", args("request_object", jsonObjectForJwt));

			return env;

		} catch (ParseException e) {
			throw error("Couldn't parse request object", e, args("request", requestObjectString));
		}
	}
}
