package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.Map;

public class ExtractNextLink extends AbstractJsonAssertingCondition {

	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	@PostEnvironment(strings = "extracted_link")
	public Environment evaluate(Environment env) {

		JsonElement body = bodyFrom(env);
		JsonElement nextLink = findByPath(body, "$.links.next");
		env.putString("extracted_link", OIDFJSON.getString(nextLink));

		logSuccess("Next link was extracted", Map.of("Link", nextLink));

		return env;
	}
}
