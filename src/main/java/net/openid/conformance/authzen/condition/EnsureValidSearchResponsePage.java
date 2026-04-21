package net.openid.conformance.authzen.condition;

import com.google.common.base.Strings;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class EnsureValidSearchResponsePage extends AbstractCondition {

	@Override
	@PreEnvironment(required = "authzen_search_endpoint_response")
	public Environment evaluate(Environment env) {
		JsonObject searchResponse = env.getObject("authzen_search_endpoint_response");
		JsonElement pageElem = searchResponse.get("page");
		env.removeObject("authzen_search_endpoint_request_page_token");  // clear old page token
		if (null == pageElem) {
			logSuccess("Search results does not contain a page element");
			return env;
		}
		if(!pageElem.isJsonObject()) {
			throw error("Search results page element is not an object", args("authzen_search_endpoint_response", searchResponse));
		}
		JsonObject pageObj = pageElem.getAsJsonObject();
		if(!pageObj.has("next_token")) {
			throw error("Search results page object does not have next_token", args("authzen_search_endpoint_response", searchResponse));
		}
		JsonElement nextTokenElem  = pageObj.get("next_token");
		if(!nextTokenElem.isJsonPrimitive() || !nextTokenElem.getAsJsonPrimitive().isString()) {
			throw error("The next_token element in the Search results page object is not a string", args("authzen_search_endpoint_response", searchResponse));
		} else {
			if(!Strings.isNullOrEmpty(OIDFJSON.getString(nextTokenElem))) {
				env.putString("authzen_search_endpoint_request_page_token", OIDFJSON.getString(nextTokenElem));
			}
		}

		String[] keys = {"count", "total"};
		for(String key : keys) {
			if(pageObj.has(key)) {
				JsonElement keyElem = pageObj.get(key);
				if(!keyElem.isJsonPrimitive() || !keyElem.getAsJsonPrimitive().isNumber()) {
					throw error("An element in the Search results page object is not a number", args("authzen_search_endpoint_response", searchResponse, "element name", key));
				}
				if(OIDFJSON.getInt(keyElem) < 0) {
					throw error("An element in the Search results page object is not a non-negative number", args("authzen_search_endpoint_response", searchResponse, "element name", key));
				}
			}
		}
		if(pageObj.has("properties")) {
			if(!pageObj.get("properties").isJsonObject()) {
				throw error("The properties in the Search results page object is not an object", args("authzen_search_endpoint_response", searchResponse));
			}
		}
		logSuccess("Search response page object is valid");
		return env;

	}

}
