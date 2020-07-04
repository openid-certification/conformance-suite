package net.openid.conformance.condition.as.dynregistration;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

//We were ignoring what the client requested and overriding it with the expected method but
// as discussed on slack on 2020-07-02, we will deny registration
// if the requested method is set and different from the expected method
// Still allows registration if token_endpoint_auth_method is not set
public abstract class AbstractEnsureTokenEndPointAuthMethod extends AbstractCondition {
	@Override
	public Environment evaluate(Environment env) {
		String expectedMethod = expectedTokenEndPointAuthMethod();
		JsonObject client = env.getObject("client");
		if(!client.has("token_endpoint_auth_method")) {
			log("token_endpoint_auth_method is not set, client will be registered using '"+expectedMethod+
				"' as required by this test");
			return env;
		}
		String method = env.getString("client", "token_endpoint_auth_method");
		if(expectedMethod.equals(method)) {
			logSuccess("token_endpoint_auth_method is '"+expectedMethod+"' as expected");
			return env;
		}
		throw error("token_endpoint_auth_method is set to '"+method+"' but this test requires '"+expectedMethod+"'",
			args("expected", expectedMethod, "actual", method));
	}

	protected abstract String expectedTokenEndPointAuthMethod();
}
