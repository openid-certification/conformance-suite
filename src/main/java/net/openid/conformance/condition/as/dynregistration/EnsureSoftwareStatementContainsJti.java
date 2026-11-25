package net.openid.conformance.condition.as.dynregistration;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * Only checks that a jti is present: detecting reuse needs registrations from more than one test run.
 */
public class EnsureSoftwareStatementContainsJti extends AbstractCondition {

	@Override
	@PreEnvironment(required = "software_statement")
	public Environment evaluate(Environment env) {
		String jti = env.getString("software_statement", "claims.jti");
		if (Strings.isNullOrEmpty(jti)) {
			throw error("The software statement does not contain a unique identifier (jti) claim",
				args("claims", env.getElementFromObject("software_statement", "claims")));
		}
		logSuccess("The software statement contains a unique identifier (jti) claim", args("jti", jti));
		return env;
	}
}
