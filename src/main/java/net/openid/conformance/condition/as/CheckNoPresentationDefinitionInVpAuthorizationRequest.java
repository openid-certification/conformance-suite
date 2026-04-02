package net.openid.conformance.condition.as;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * VP1 Final tests in this suite use dcql_query to drive wallet behavior.
 * Legacy presentation_definition parameters no longer have meaning here, so
 * their presence is a sign that the verifier may be mixing draft and final
 * request construction.
 */
public class CheckNoPresentationDefinitionInVpAuthorizationRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = { CreateEffectiveAuthorizationRequestParameters.ENV_KEY })
	public Environment evaluate(Environment env) {
		JsonElement pd = env.getElementFromObject(
			CreateEffectiveAuthorizationRequestParameters.ENV_KEY, "presentation_definition");
		JsonElement pdUri = env.getElementFromObject(
			CreateEffectiveAuthorizationRequestParameters.ENV_KEY, "presentation_definition_uri");

		if (pd != null) {
			throw error("Authorization request contains legacy 'presentation_definition'. VP1 Final uses 'dcql_query' for credential selection, so sending both is unnecessary and may indicate draft and final request formats are being mixed.",
				args("presentation_definition", pd));
		}
		if (pdUri != null) {
			throw error("Authorization request contains legacy 'presentation_definition_uri'. VP1 Final uses 'dcql_query' for credential selection, so sending both is unnecessary and may indicate draft and final request formats are being mixed.",
				args("presentation_definition_uri", pdUri));
		}

		logSuccess("Authorization request does not contain presentation_definition or presentation_definition_uri");
		return env;
	}
}
