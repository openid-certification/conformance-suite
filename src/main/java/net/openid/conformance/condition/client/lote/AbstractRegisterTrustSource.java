package net.openid.conformance.condition.client.lote;

import net.openid.conformance.condition.AbstractCondition;

import java.util.List;

/**
 * Base of the conditions that register the trust source of one certificate chain: a configured
 * PEM, or a list of trusted entities ({@link LoteTrustAnchors}). A chain is validated against
 * exactly one, so a configuration that sets more than one is rejected.
 */
public abstract class AbstractRegisterTrustSource extends AbstractCondition {

	/**
	 * @param sources the configured sources, as from {@link LoteTrustAnchors#configuredSources}
	 * @param certificate the certificate the chain leads from, for the message ("credential signing certificate")
	 */
	protected void requireAtMostOneTrustSource(List<String> sources, String certificate) {
		if (sources.size() > 1) {
			// Silently dropping one loses a check the tester asked for: a list may fail to fetch,
			// leaving no trust source at all, while using both would validate the same chain
			// against two different trust sources.
			throw error("More than one trust source is configured for the " + certificate + ": " + String.join("; ", sources)
				+ ". The chain is validated against exactly one, so please clear all but one of them", args("configured", sources));
		}
	}
}
