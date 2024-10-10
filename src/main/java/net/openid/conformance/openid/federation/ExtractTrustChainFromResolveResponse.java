package net.openid.conformance.openid.federation;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ExtractTrustChainFromResolveResponse extends AbstractCondition {

	@Override
	@PreEnvironment(required = "entity_statement_body")
	@PostEnvironment(required = { "trust_chain_from_resolver" })
	public Environment evaluate(Environment env) {

		JsonArray trustChain = env.getElementFromObject("entity_statement_body", "trust_chain").getAsJsonArray();
		JsonObject trustChainFromResolver = new JsonObject();
		trustChainFromResolver.add("trust_chain", trustChain);
		env.putObject("trust_chain_from_resolver", trustChainFromResolver);

		logSuccess("Extracted trust chain from resolve response", args("trust_chain", trustChain));

		return env;
	}

}
