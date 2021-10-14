package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class UnmapDirectoryValues extends AbstractCondition {

    @Override
	public Environment evaluate(Environment env) {
		
		env.unmapKey("server");
        env.unmapKey("client");
        env.unmapKey("discovery_endpoint_response");
        env.unmapKey("config");
		
		return env;
	}
}