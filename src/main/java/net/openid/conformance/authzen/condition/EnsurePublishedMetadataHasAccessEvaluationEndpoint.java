package net.openid.conformance.authzen.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * Section 9.1.1 makes access_evaluation_endpoint REQUIRED, and Section 9.1.3 lets a PEP ignore
 * signed metadata. Such a PEP has only the PDP metadata document as published, so that document
 * must carry the parameter even when its signed_metadata does: signed metadata is provided "in
 * addition to JSON elements" (Section 9.1.3), not in place of them. The specification does not say
 * this in so many words; openid/authzen#804 asks the working group. Runs only when the document
 * carries signed_metadata; without it, {@link CheckPDPServerConfiguration} requires the parameter
 * in the published values.
 */
public class EnsurePublishedMetadataHasAccessEvaluationEndpoint extends AbstractCondition {

	@Override
	@PreEnvironment(required = "server")
	public Environment evaluate(Environment env) {
		JsonObject server = env.getObject("server");
		if (!server.has("access_evaluation_endpoint")) {
			throw error("The PDP metadata as published has no access_evaluation_endpoint, which is required. "
				+ "A PEP that ignores signed_metadata, which it may, has only the published document, so the document "
				+ "must carry it even when signed_metadata does.");
		}
		logSuccess("The PDP metadata as published has an access_evaluation_endpoint");
		return env;
	}
}
