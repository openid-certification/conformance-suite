package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public abstract class AbstractScopeAddingCondition extends AbstractCondition {

	@Override
	@PreEnvironment(required = "client")
	public Environment evaluate(Environment env) {
		JsonObject client = env.getObject("client");

		JsonElement scopeElement = client.get("scope");
		String scope = "";
		if(scopeElement != null) {
			scope = OIDFJSON.getString(scopeElement).concat(" ");
		}

		scope = scope.concat(newScope());

		client.addProperty("scope", scope);

		logSuccess("Added scope of '" +scope+ "' to client's scope", client);
		return env;
	}

	protected abstract String newScope();

}
