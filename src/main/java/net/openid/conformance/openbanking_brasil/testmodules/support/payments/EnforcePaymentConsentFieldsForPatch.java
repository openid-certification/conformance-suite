package net.openid.conformance.openbanking_brasil.testmodules.support.payments;

import com.fasterxml.jackson.databind.util.ArrayIterator;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;


import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class EnforcePaymentConsentFieldsForPatch extends AbstractCondition {
	@Override
	public Environment evaluate(Environment env) {
		JsonObject obj = env.getObject("config");
		Iterator<String> iter = new ArrayIterator<>("resource.brazilPaymentConsent.data".split("\\.")).iterator();
		obj = find("data", iter, obj);
		List<String> elements = Arrays.asList("debtorAccount","loggedUser","businessEntity");
		for (String element: elements){
			checkObjectExits(element,obj);
		}
		logSuccess("Payment configuration has required fields");
		return env;
	}

	private JsonObject find(String elementName, Iterator<String> iter, JsonObject obj) {
		if(obj.has(elementName)) {
			return obj.getAsJsonObject(elementName);
		}
		if(!iter.hasNext()) {
			throw error("Unable to find " + elementName);
		}
		String nextElement = iter.next();
		obj = obj.getAsJsonObject(nextElement);
		return find(elementName, iter, obj);
	}

	private void checkObjectExits(String elementName, JsonObject object){
		if(!object.has(elementName)) {
			throw error("Configuration does not have a " + elementName + " entry in the payment consent section");
		}
		logSuccess(elementName + "Successfully found on payment consent");
	}
}
