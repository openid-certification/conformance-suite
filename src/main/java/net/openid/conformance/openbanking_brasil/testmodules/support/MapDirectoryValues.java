package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class MapDirectoryValues extends AbstractCondition {

    @Override
	public Environment evaluate(Environment env) {
		
		env.mapKey("config", "directory_config");
        env.mapKey("server", "directory_server");
        env.mapKey("client", "directory_client");
        env.mapKey("discovery_endpoint_response", "directory_discovery_endpoint_response");
		
		return env;
	}
}