package net.openid.conformance.condition.common;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.Set;

public class FAPICheckKeyAlgInClientJWKs extends AbstractCheckKeyAlgInClientJWKs {

	@Override
	protected Set<String> getPermitted() {
		return Set.of( "PS256", "ES256" );
	}

}
