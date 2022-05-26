package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class SaveConsentsAccessToken extends AbstractCondition {

	@Override
	@PreEnvironment(required = "access_token")
	@PostEnvironment(required = "consents_access_token")
	public Environment evaluate(Environment env) {
		JsonObject o = new JsonObject();
		o.addProperty("value", env.getString("access_token", "value"));
		o.addProperty("type", env.getString("access_token", "type"));

		env.putObject("consents_access_token", o);
		return env;
	}
}
