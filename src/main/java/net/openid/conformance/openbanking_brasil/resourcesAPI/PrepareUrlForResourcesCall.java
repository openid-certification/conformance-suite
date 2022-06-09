package net.openid.conformance.openbanking_brasil.resourcesAPI;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class PrepareUrlForResourcesCall extends AbstractCondition {
	@Override
	@PreEnvironment(required = "config")
	@PostEnvironment(strings = "protected_resource_url")
	public Environment evaluate(Environment env) {
		// Takes the consents URL, leaves its base and attaches the resources endpoint
		String consentUrl = env.getString("config", "resource.consentUrl");
		String resourcesApiUrl = consentUrl.replaceAll("/consents/v1/consents", "/resources/v1/resources");

		String protectedResourceUrl = env.getString("protected_resource_url");
		if(!Strings.isNullOrEmpty(protectedResourceUrl)){
			env.putString("base_resource_url", protectedResourceUrl);
		}
		env.putString("protected_resource_url", resourcesApiUrl);
		log("Prepared URL for Resources API call");
		return env;
	}
}
