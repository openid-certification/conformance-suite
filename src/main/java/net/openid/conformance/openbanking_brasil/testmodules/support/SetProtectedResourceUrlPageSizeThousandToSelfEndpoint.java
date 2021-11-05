package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.JsonUtils;

public class SetProtectedResourceUrlPageSizeThousandToSelfEndpoint extends AbstractCondition {

	private static Gson GSON = JsonUtils.createBigDecimalAwareGson();

	@Override
	@PostEnvironment(strings = "protected_resource_url")
	public Environment evaluate(Environment env) {

		String entityString = env.getString("resource_endpoint_response");
		JsonObject body = GSON.fromJson(entityString, JsonObject.class);

		JsonObject links = body.getAsJsonObject("links");
		String selfLink = OIDFJSON.getString(links.get("self"));
		if(selfLink.contains("page-size=")){
			if(selfLink.contains("&page-size")){
				String[] split = selfLink.split("page-size=");
				split[1] = "page-size=1000";
				selfLink = split[0] + split[1];
			} else {
				String[] split = selfLink.split("page-size=");
				String[] subsplit = split[1].split("&");
				subsplit[0] = "page-size=1000&";
				selfLink = split[0] + subsplit[0] + subsplit[1];
			}
			env.putString("protected_resource_url", selfLink);
		} else {
			env.putString("protected_resource_url", OIDFJSON.getString(links.get("self")).concat("?page-size=1000"));
		}


		log("Saving old environment values");
		return env;
	}
}

