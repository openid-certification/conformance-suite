package net.openid.conformance.openbanking_brasil.resourcesAPI.v2;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.openbanking_brasil.testmodules.support.resource.ResourceBuilder;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class PrepareUrlForSavedResourceCall extends ResourceBuilder {

	@Override
	@PreEnvironment(required = "resource_data")
	public Environment evaluate(Environment env) {
		setAllowDifferentBaseUrl(true);

		JsonObject resource = env.getObject("resource_data");
		setApi(OIDFJSON.getString(resource.get("resource_api")));
		setEndpoint(OIDFJSON.getString(resource.get("resource_endpoint")));

		return super.evaluate(env);
	}
}
