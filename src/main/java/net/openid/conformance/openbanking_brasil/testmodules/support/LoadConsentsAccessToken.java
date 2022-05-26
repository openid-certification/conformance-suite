package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class LoadConsentsAccessToken extends AbstractCondition {
	@Override
	@PreEnvironment(required = "consents_access_token")
	@PostEnvironment(required = "access_token")
	public Environment evaluate(Environment env) {
		JsonObject o = new JsonObject();
		o.addProperty("value", env.getString("consents_access_token", "value"));
		o.addProperty("type", env.getString("consents_access_token", "type"));

		env.putObject("access_token", o);
		return env;
	}
}
