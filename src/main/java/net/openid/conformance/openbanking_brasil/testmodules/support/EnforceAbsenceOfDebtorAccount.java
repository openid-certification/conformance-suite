package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.fasterxml.jackson.databind.util.ArrayIterator;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

import java.util.Iterator;

public class EnforceAbsenceOfDebtorAccount extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		JsonObject obj = env.getObject("config");
		Iterator<String> iter = new ArrayIterator<>("resource.brazilPaymentConsent.data".split("\\.")).iterator();
		obj = find("data", iter, obj);
		obj.remove("debtorAccount");
		logSuccess("Payment configuration has no debtor account");
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
}
