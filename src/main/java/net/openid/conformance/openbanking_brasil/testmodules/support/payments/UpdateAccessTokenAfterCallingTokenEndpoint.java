package net.openid.conformance.openbanking_brasil.testmodules.support.payments;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.JsonObjectBuilder;

public class UpdateAccessTokenAfterCallingTokenEndpoint extends AbstractCondition {


	@Override
	@PreEnvironment(required = {"token_endpoint_response"})
	@PostEnvironment(required = "access_token")
	public Environment evaluate(Environment env) {
		JsonObject tokenEndpointResponse = env.getObject("token_endpoint_response");
		String accessToken = OIDFJSON.getString(tokenEndpointResponse.get("access_token"));
		String tokenType =  OIDFJSON.getString(tokenEndpointResponse.get("token_type"));
		JsonObjectBuilder accessTokenObject = new JsonObjectBuilder()
			.addField("value", accessToken)
			.addField("type", tokenType);
		env.putObject("access_token",accessTokenObject.build());
		logSuccess("Access token updated");
		return env;
	}
}
