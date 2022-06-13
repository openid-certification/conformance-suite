package net.openid.conformance.condition.as.par;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.UUID;

public class SetParResponseExpiresInToGreaterThan600 extends AbstractCondition {

	public static final int EXPIRES_IN = 601;

	@Override
	@PreEnvironment()
	@PostEnvironment(required = "par_endpoint_response")
	public Environment evaluate(Environment env) {

		JsonObject parEndpointResponse = env.getObject("par_endpoint_response");
		parEndpointResponse.addProperty("expires_in", EXPIRES_IN);
		env.putObject("par_endpoint_response", parEndpointResponse);
		logSuccess("Set PAR endpoint response expires_in to value > 600", parEndpointResponse);

		return env;
	}
}
