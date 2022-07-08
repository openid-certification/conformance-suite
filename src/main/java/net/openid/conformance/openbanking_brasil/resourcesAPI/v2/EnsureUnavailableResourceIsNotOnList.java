package net.openid.conformance.openbanking_brasil.resourcesAPI.v2;

import com.google.gson.*;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.Objects;

public class EnsureUnavailableResourceIsNotOnList extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"resource_endpoint_response_full", "resource_data"})
	public Environment evaluate(Environment env) {
		JsonObject resourceData = env.getObject("resource_data");
		String resourceId       = OIDFJSON.getString(resourceData.get("resourceId"));
		String idType           = OIDFJSON.getString(resourceData.get("id_type"));

		JsonObject response = env.getObject("resource_endpoint_response_full");
		String bodyString   = OIDFJSON.getString(response.get("body"));
		JsonObject body     = JsonParser.parseString(bodyString).getAsJsonObject();
		JsonArray data      = body.getAsJsonArray("data");

		for (JsonElement element : data) {

			if (Objects.equals(resourceId, OIDFJSON.getString(OIDFJSON.toObject(element).get(idType)))) {
				throw error("Unavailable resource should not be visible in the list.", args("Resource", element.getAsJsonObject()));
			}
		}

		logSuccess("Unavailable resource is not visible in the list.");
		return env;
	}
}
