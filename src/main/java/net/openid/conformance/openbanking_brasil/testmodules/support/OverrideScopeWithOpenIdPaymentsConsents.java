package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class OverrideScopeWithOpenIdPaymentsConsents extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		JsonObject client = (JsonObject) env.getElementFromObject("config", "client");
		client.addProperty("scope", "openid payments consents");
		return env;
	}
}
