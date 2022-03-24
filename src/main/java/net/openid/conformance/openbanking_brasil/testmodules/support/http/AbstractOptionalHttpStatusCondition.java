package net.openid.conformance.openbanking_brasil.testmodules.support.http;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;

public abstract class AbstractOptionalHttpStatusCondition extends AbstractCondition {

	@Override
	@PostEnvironment(required = "allowedHttpStatuses")
	public Environment evaluate(Environment env) {
		JsonObject allowedStatuses = env.getObject("allowedHttpStatuses");
		if(allowedStatuses == null) {
			allowedStatuses = new JsonObject();
			allowedStatuses.add("statuses", new JsonArray());
			env.putObject("allowedHttpStatuses", allowedStatuses);
		}
		JsonArray jsonElements = (JsonArray) allowedStatuses.get("statuses");
		int status = getStatus();
		logSuccess("Allowing HTTP status to be " + status);
		jsonElements.add(status);
		return env;
	}

	protected abstract int getStatus();

}
