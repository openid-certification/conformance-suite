package net.openid.conformance.vci10issuer.condition;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Checks that no two credentials issued in a batch share the same Token Status List reference (the
 * same {@code uri} and {@code idx}). Sharing a reference is a double allocation (Token Status List
 * §13.3) and lets verifiers link the credentials; HAIP §6.1 requires each credential to have its own
 * unique status list index.
 */
public class VCIEnsureBatchStatusReferencesAreDistinct extends AbstractVCIBatchStatusReferenceCheck {

	@Override
	@PreEnvironment(required = "linkability_captures")
	public Environment evaluate(Environment env) {
		List<StatusRef> refs = extractStatusReferences(env);
		if (refs.size() < 2) {
			log("Fewer than two credentials in the batch carry a status list reference; nothing to compare",
				args("status_reference_count", refs.size()));
			return env;
		}

		Map<String, Integer> seen = new HashMap<>();
		for (StatusRef ref : refs) {
			String key = ref.uri() + "#" + ref.idx();
			Integer firstIndex = seen.putIfAbsent(key, ref.credentialIndex());
			if (firstIndex != null) {
				throw error("Two credentials issued in the same batch share the same status list reference "
						+ "(same uri and index): they point at the same status bit and verifiers can use this to "
						+ "link them. Each credential must have its own unique status list index.",
					args("uri", ref.uri(), "idx", ref.idx(),
						"first_credential_index", firstIndex,
						"second_credential_index", ref.credentialIndex()));
			}
		}

		logSuccess("Every credential in the batch has a distinct status list reference",
			args("status_reference_count", refs.size()));
		return env;
	}
}
