package net.openid.conformance.condition.client;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * {@link AbstractEnsureMostPreferredDcqlClaimSetReturned} for an mdoc presentation.
 */
public class EnsureMostPreferredMdocDcqlClaimSetReturned extends AbstractEnsureMostPreferredDcqlClaimSetReturned {

	@Override
	@PreEnvironment(required = {"mdoc", "dcql_query"}, strings = {"credential_id"})
	public Environment evaluate(Environment env) {
		return checkMostPreferredOptionReturned(env, DcqlQueryUtils.extractDisclosedMdocPaths(env)::containsAll);
	}
}
