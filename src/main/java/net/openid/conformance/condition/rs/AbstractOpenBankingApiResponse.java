package net.openid.conformance.condition.rs;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;

public abstract class AbstractOpenBankingApiResponse extends AbstractCondition {

	protected JsonObject createResponse(JsonObject data) {
		JsonObject response = new JsonObject();

		response.add("Data", data);

		JsonObject meta = new JsonObject();
		meta.addProperty("TotalPages", 1);
		response.add("Meta", meta);

		return response;
	}

}
