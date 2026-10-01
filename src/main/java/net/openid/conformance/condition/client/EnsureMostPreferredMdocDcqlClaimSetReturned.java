package net.openid.conformance.condition.client;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.List;
import java.util.Set;

/**
 * {@link AbstractEnsureMostPreferredDcqlClaimSetReturned} for an mdoc presentation.
 */
public class EnsureMostPreferredMdocDcqlClaimSetReturned extends AbstractEnsureMostPreferredDcqlClaimSetReturned {

	@Override
	@PreEnvironment(required = {"mdoc", "dcql_query"}, strings = {"credential_id"})
	public Environment evaluate(Environment env) {
		Set<List<String>> disclosedPaths = DcqlQueryUtils.extractDisclosedMdocPaths(env);
		return checkMostPreferredOptionReturned(env, disclosedPaths::containsAll, option -> option.containsAll(disclosedPaths));
	}
}
