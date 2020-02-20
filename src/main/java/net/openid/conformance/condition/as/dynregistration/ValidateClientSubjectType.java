package net.openid.conformance.condition.as.dynregistration;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.Set;

/**
 * subject_type
 * OPTIONAL. subject_type requested for responses to this Client. The subject_types_supported
 * Discovery parameter contains a list of the supported subject_type values for this server.
 * Valid types include pairwise and public.
 */
public class ValidateClientSubjectType extends AbstractClientValidationCondition
{
	public static final Set<String> ALLOWED_SUBJECT_TYPES = Set.of("public", "pairwise");

	@Override
	@PreEnvironment(required = { "client"})
	public Environment evaluate(Environment env) {
		this.client = env.getObject("client");

		String subjectType = getSubjectType();
		if(subjectType==null) {
			logSuccess("A subject_type was not provided");
			return env;
		}

		if(ALLOWED_SUBJECT_TYPES.contains(subjectType)) {
			logSuccess("subject_type is valid", args("subject_type", subjectType));
			return env;
		}
		throw error("Unexpected subject_type", args("subject_type", subjectType));
	}
}
