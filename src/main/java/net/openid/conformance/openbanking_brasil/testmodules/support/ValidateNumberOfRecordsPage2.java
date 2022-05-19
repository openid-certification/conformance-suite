package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.testmodule.Environment;

public class ValidateNumberOfRecordsPage2 extends ValidateNumberOfRecords{
	@Override
	public Environment evaluate(Environment env) {

		return executeNumberOfPageCheck(env, "last");
	}

}

