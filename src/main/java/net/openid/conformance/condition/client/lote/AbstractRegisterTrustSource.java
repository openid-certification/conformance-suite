package net.openid.conformance.condition.client.lote;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.List;

/**
 * Base of the conditions that register the trust source of one certificate chain
 * ({@link TrustPurpose}): a configured PEM, or a list of trusted entities. A chain is validated
 * against exactly one, so a configuration that sets more than one is rejected, unless the purpose
 * lets a PEM accompany a list ({@link TrustPurpose#pemMayAccompanyLists}).
 */
public abstract class AbstractRegisterTrustSource extends AbstractCondition {

	protected abstract TrustPurpose purpose();

	@Override
	@PreEnvironment(required = "config")
	public Environment evaluate(Environment env) {
		TrustPurpose purpose = purpose();
		List<String> sources = purpose.configuredSources(env);
		String pem = purpose.configuredPem(env);
		// the PEM is the first source after any other source
		List<String> exclusive = purpose.pemMayAccompanyLists && pem != null ? sources.subList(1, sources.size()) : sources;
		if (exclusive.size() > 1) {
			// Silently dropping one loses a check the tester asked for: a list may fail to fetch,
			// leaving no trust source at all, while using both would validate the same chain
			// against two different trust sources.
			throw error("More than one trust source is configured for the " + purpose.certificate + ": " + String.join("; ", sources)
				+ ". The chain is validated against exactly one, so please clear all but one of them", args("configured", sources));
		}

		if (pem == null) {
			log("No '" + purpose.pemFieldLabel + "' configured, skipping registration",
				args("trust_source", sources.isEmpty() ? null : sources.get(0)));
		} else {
			purpose.register(env, pem);
			log("Registered the '" + purpose.pemFieldLabel + "' as the trust anchor for the " + purpose.certificate,
				args("trust_anchor_pem", pem));
		}
		return env;
	}
}
