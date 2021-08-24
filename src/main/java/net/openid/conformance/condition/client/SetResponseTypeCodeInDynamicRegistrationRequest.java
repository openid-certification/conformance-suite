package net.openid.conformance.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class SetResponseTypeCodeInDynamicRegistrationRequest extends AbstractSetResponseTypeInDynamicRegistrationRequest {

	@Override
	protected String responseType() {
		return "code";
	}

}
