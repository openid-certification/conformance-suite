package net.openid.conformance.openid.federation;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.net.MalformedURLException;
import java.net.URL;

public class ValidateEntityIdentifier extends ValidateUrlRequirements {

	@Override
	@PreEnvironment(required = { "config" } )
	public Environment evaluate(Environment env) {
	    String label = "Entity identifier";
		String fieldName = "entity_identifier";
		JsonElement element = env.getElementFromObject("config", "federation.entity_identifier");
		return validateUrlRequirements(element, fieldName, label, env);
	}

}
