package net.openid.conformance.condition.client.lote;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.List;

/**
 * Base of the conditions that require a trust source for one certificate chain ({@link TrustPurpose})
 * where the selected profile mandates validating it.
 */
public abstract class AbstractEnsureTrustSourceConfigured extends AbstractCondition {

	protected abstract TrustPurpose purpose();

	/** Why a trust source is required, completing "... is set in the test configuration. ". */
	protected abstract String requiredBecause();

	@Override
	@PreEnvironment(required = "config")
	public Environment evaluate(Environment env) {
		TrustPurpose purpose = purpose();
		List<String> sources = purpose.configuredSources(env);
		if (sources.isEmpty()) {
			throw error("None of the trust source fields for the " + purpose.certificate + " (" + purpose.fieldsDescription()
				+ ") is set in the test configuration. " + requiredBecause());
		}
		logSuccess("A trust source for the " + purpose.certificate + " is configured", args("configured", sources));
		return env;
	}
}
