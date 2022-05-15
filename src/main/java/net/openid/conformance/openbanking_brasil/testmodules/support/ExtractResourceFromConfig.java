package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ExtractResourceFromConfig extends AbstractCondition {
	@PreEnvironment(required = "config")
	@Override
	public Environment evaluate(Environment env){
		JsonElement resourceElem= env.getElementFromObject("config", "resource");
		env.putObject("resource", (JsonObject) resourceElem );
		return env;
	}
}
