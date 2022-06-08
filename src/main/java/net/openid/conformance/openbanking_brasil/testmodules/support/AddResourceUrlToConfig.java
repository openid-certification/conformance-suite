package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.Map;

public class AddResourceUrlToConfig extends AbstractCondition {
	@Override
	@PreEnvironment(required = "config")
	public Environment evaluate(Environment env) {
		String consentUrl = OIDFJSON.getString(env.getElementFromObject("config", "resource.consentUrl"));
		String resourceUrl = consentUrl.replace("/consents", "/pix/payments");
		env.putString("config", "resource.resourceUrl", resourceUrl);
		logSuccess("resourceUrl was added to the config", Map.of("resourceUrl", resourceUrl));
		return env;
	}
}
