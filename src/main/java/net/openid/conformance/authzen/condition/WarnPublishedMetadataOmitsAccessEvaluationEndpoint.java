package net.openid.conformance.authzen.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * Section 9.1.1 makes access_evaluation_endpoint REQUIRED, and Section 9.1.3 lets a PEP ignore
 * signed metadata. When the PDP metadata document as published, which also carries
 * signed_metadata, has no access_evaluation_endpoint, such a PEP finds no Access Evaluation
 * endpoint. Whether the parameter may be carried by the signed metadata alone is not settled, so
 * this is intended to be called at WARNING; the merged values are still required to have it.
 */
public class WarnPublishedMetadataOmitsAccessEvaluationEndpoint extends AbstractCondition {

	@Override
	@PreEnvironment(required = "server")
	public Environment evaluate(Environment env) {
		JsonObject server = env.getObject("server");
		if (!server.has("access_evaluation_endpoint")) {
			throw error("The PDP metadata as published has no access_evaluation_endpoint, which Section 9.1.1 makes "
				+ "REQUIRED. A PEP that ignores signed_metadata, as Section 9.1.3 allows, would find no Access "
				+ "Evaluation endpoint.");
		}
		logSuccess("The PDP metadata as published has an access_evaluation_endpoint");
		return env;
	}
}
