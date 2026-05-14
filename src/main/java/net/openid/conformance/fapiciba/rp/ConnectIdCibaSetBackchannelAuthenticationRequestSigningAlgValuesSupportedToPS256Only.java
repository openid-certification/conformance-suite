package net.openid.conformance.fapiciba.rp;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ConnectIdCibaSetBackchannelAuthenticationRequestSigningAlgValuesSupportedToPS256Only extends AbstractCondition {

	@Override
	@PreEnvironment(required = "server")
	@PostEnvironment(required = "server")
	public Environment evaluate(Environment env) {
		JsonArray data = new JsonArray();
		data.add("PS256");

		JsonObject server = env.getObject("server");
		server.add("backchannel_authentication_request_signing_alg_values_supported", data);

		logSuccess("Set backchannel_authentication_request_signing_alg_values_supported to PS256 only",
			args("values", data));

		return env;
	}
}
