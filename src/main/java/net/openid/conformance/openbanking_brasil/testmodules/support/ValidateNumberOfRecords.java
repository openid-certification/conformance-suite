package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.List;

public class ValidateNumberOfRecords extends AbstractJsonAssertingCondition{
	@Override
	public Environment evaluate(Environment env) {

		return env;
	}

	protected Environment executeNumberOfPageCheck(Environment env, String page){

		if(!page.equals("first") && !page.equals("last")) {
			throw  error("Specified page not found");
		}

		JsonElement apiResponse = bodyFrom(env);

		if (!JsonHelper.ifExists(apiResponse, "$.data")) {
			throw  error("No data field found in resource_endpoint_response");
		}

		JsonElement dataElement = findByPath(apiResponse, "$.data");
		JsonArray dataArray = dataElement.getAsJsonArray();
		int pX = dataArray.size();

		if (!JsonHelper.ifExists(apiResponse, "$.links." + page)) {
			throw  error("No first field found in self-links");
		}

		JsonElement firstElement = findByPath(apiResponse, "$.links." + page);
		String firstLink = OIDFJSON.getString(firstElement);

		int lastIndexOf = firstLink.lastIndexOf("page-size=");
		int pY;

		if(lastIndexOf < 0){
			pY = 25;
		} else{
			pY = Integer.parseInt(firstLink.substring(lastIndexOf).replaceAll("[^0-9]", ""));
		}

		if(page.equals("first")) {
			env.putString("P1_X", Integer.toString(pX));
			if(pX != pY) {
				throw  error("Number of records returned is different from specified in page-size");
			}
			logSuccess("Number of records match accordingly");

		} else if(page.equals("last")){
			env.putString("P2_X", Integer.toString(pX));
			if(pX > pY) {
				throw  error("Number of records returned is different from specified in page-size");
			}

			if (!JsonHelper.ifExists(apiResponse, "$.meta")) {
				throw  error("No meta field found in resource_endpoint_response");
			}

			String p1XString = env.getString("P1_X");

			if(p1XString == null) {
				throw error("Number of records for page 1 not found");
			}
			int p1X = Integer.parseInt(p1XString);

			JsonElement totElement = findByPath(apiResponse, "$.meta.totalRecords");
			int tot = OIDFJSON.getInt(totElement);

			if(tot < p1X + pX) {
				throw error("Total records is less than records counts in page 1 and 2");
			}

			logSuccess("Number of records match accordingly");
		}

		return env;
	}
}
