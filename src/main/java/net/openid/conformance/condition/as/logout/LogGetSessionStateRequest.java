package net.openid.conformance.condition.as.logout;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class LogGetSessionStateRequest extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		JsonObject sessionStateData = env.getObject("session_state_data");
		if(sessionStateData!=null) {
			log("OP iframe received postMessage request from RP iframe", args("returning_response", sessionStateData));
		} else {
			log("OP iframe received postMessage request from RP iframe but the user is not logged in");
		}
		return env;
	}
}
