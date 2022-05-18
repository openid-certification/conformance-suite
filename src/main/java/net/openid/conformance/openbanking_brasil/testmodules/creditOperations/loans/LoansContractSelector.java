package net.openid.conformance.openbanking_brasil.testmodules.creditOperations.loans;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class LoansContractSelector extends AbstractCondition {

	@Override
	@PostEnvironment(strings = "contractId")
	public Environment evaluate(Environment env) {
		String entityString = env.getString("resource_endpoint_response");
		JsonObject contractList = JsonParser.parseString(entityString).getAsJsonObject();

		JsonArray data = contractList.getAsJsonArray("data");
		if(data.size() <= 0) {
			throw error("Data field is empty, no further processing required.");
		}

		JsonObject firstContract = data.get(0).getAsJsonObject();
		String contractId = OIDFJSON.getString(firstContract.get("contractId"));
		env.putString("contractId", contractId);
		logSuccess("Specific contract selected");
		return env;
	}

}
