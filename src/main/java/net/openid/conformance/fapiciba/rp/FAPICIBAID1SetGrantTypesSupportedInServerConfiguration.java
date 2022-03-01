package net.openid.conformance.fapiciba.rp;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class FAPICIBAID1SetGrantTypesSupportedInServerConfiguration extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"server"})
	@PostEnvironment(required = {"server"})
	public Environment evaluate(Environment env) {
		JsonArray grantTypes = new JsonArray();
		grantTypes.add("authorization_code");
		grantTypes.add("implicit");
		grantTypes.add("client_credentials");
		grantTypes.add("refresh_token");
		grantTypes.add("urn:openid:params:grant-type:ciba");
		JsonObject server = env.getObject("server");
		server.add("grant_types_supported", grantTypes);

		log("Successfully set grant_types_supported", args("server", server));
		return env;
	}

}
