package net.openid.conformance.openbanking_brasil.resourcesAPI.v2;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.Objects;

public class SaveUnavailableResourceData extends AbstractSaveResourceData {

	@Override
	protected JsonObject searchResource(JsonArray data) {
		for (JsonElement resourceElement : data) {
			JsonObject resource = resourceElement.getAsJsonObject();
			String status = OIDFJSON.getString(resource.get("status"));

			if (Objects.equals(status, "UNAVAILABLE") || Objects.equals(status, "TEMPORARILY_UNAVAILABLE")) {
				return resource;
			}
		}

		throw error("No UNAVAILABLE/TEMPORARILY_UNAVAILABLE resource found.");
	}
}
