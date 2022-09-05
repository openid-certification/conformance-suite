package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.Map;

public class AddSpecifiedPageSizeParameterToProtectedResourceUrl extends AbstractCondition {

	@Override
	@PreEnvironment(strings = {"protected_resource_url"})
	public Environment evaluate(Environment env) {
		String baseUrl = env.getString("protected_resource_url");
		Integer requiredPageSize = env.getInteger("required_page_size");
		if (requiredPageSize != null) {
			String format = "%s?page-size=%d";
			if(baseUrl.contains("?")){
				format = "%s&page-size=%d";
			}
			String url = String.format(format, baseUrl, requiredPageSize);
			env.putString("protected_resource_url", url);
			logSuccess("Parameters were added to the resource URL", Map.of("URL", url));
		}else {
			throw error("required_page_size was not found in the environment. This is bug");

		}
		return env;
	}

}
