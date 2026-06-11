package net.openid.conformance.vci10issuer.condition;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.List;

/**
 * Warns if every credential issued in a batch references a different Token Status List URI.
 *
 * <p>Token Status List §12.2 / §12.5: a unique status list URI per credential removes herd privacy
 * and lets the URI itself correlate the credentials. The privacy-preserving shape is a status list
 * URI shared across many credentials (and holders), with the credentials differing only by an
 * unpredictable index. This is RECOMMENDED-strength (legitimate sharding across a few large lists
 * exists), so the caller raises it as a WARNING, and it is only assessed when there are enough
 * credentials for "all distinct" to be meaningful.
 */
public class VCIWarnBatchStatusListUrisProvideHerdPrivacy extends AbstractVCIBatchStatusReferenceCheck {

	private static final int MIN_REFS_TO_ASSESS = 3;

	@Override
	@PreEnvironment(required = "linkability_captures")
	public Environment evaluate(Environment env) {
		List<StatusRef> refs = extractStatusReferences(env);
		if (refs.size() < MIN_REFS_TO_ASSESS) {
			log("Fewer than " + MIN_REFS_TO_ASSESS + " credentials in the batch carry a status list reference, "
					+ "so status list herd privacy cannot be meaningfully assessed",
				args("status_reference_count", refs.size()));
			return env;
		}

		long distinctUris = refs.stream().map(StatusRef::uri).distinct().count();
		if (distinctUris == refs.size()) {
			throw error("Every credential in the batch references a different status list URI. A unique status "
					+ "list per credential removes herd privacy and lets the URI itself correlate the credentials; "
					+ "credentials should instead share a status list URI and differ only by an unpredictable index.",
				args("status_reference_count", refs.size(), "distinct_uri_count", distinctUris));
		}

		logSuccess("Credentials in the batch share status list URIs, providing herd privacy",
			args("status_reference_count", refs.size(), "distinct_uri_count", distinctUris));
		return env;
	}
}
