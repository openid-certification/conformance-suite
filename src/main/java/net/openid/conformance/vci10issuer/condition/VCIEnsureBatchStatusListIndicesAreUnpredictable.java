package net.openid.conformance.vci10issuer.condition;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Checks that the Token Status List indices assigned to the credentials issued in a batch are not a
 * predictable (arithmetic) sequence.
 *
 * <p>HAIP §6.1: "Each Credential MUST have its own unique, unpredictable status list index". Token
 * Status List §12.5.1 RECOMMENDS choosing non-sequential, pseudo-random or random indices, because a
 * predictable allocation strategy lets an observer infer that credentials were issued together / to
 * the same holder. The caller raises this as a FAILURE under HAIP and a WARNING otherwise.
 *
 * <p>Indices are grouped by status list URI; a group is flagged only when it has at least three
 * distinct indices that form an exact arithmetic progression (constant stride), which keeps the
 * false-positive rate negligible (three independent random indices almost never form one).
 */
public class VCIEnsureBatchStatusListIndicesAreUnpredictable extends AbstractVCIBatchStatusReferenceCheck {

	private static final int MIN_INDICES_TO_ASSESS = 3;

	@Override
	@PreEnvironment(required = "linkability_captures")
	public Environment evaluate(Environment env) {
		List<StatusRef> refs = extractStatusReferences(env);

		Map<String, List<Long>> indicesByUri = new LinkedHashMap<>();
		for (StatusRef ref : refs) {
			indicesByUri.computeIfAbsent(ref.uri(), k -> new ArrayList<>()).add(ref.idx());
		}

		boolean assessedAny = false;
		for (Map.Entry<String, List<Long>> entry : indicesByUri.entrySet()) {
			List<Long> distinctSorted = entry.getValue().stream().distinct().sorted().toList();
			if (distinctSorted.size() < MIN_INDICES_TO_ASSESS) {
				continue;
			}
			assessedAny = true;
			if (isArithmeticProgression(distinctSorted)) {
				long stride = distinctSorted.get(1) - distinctSorted.get(0);
				throw error("The status list indices assigned to the credentials in the batch form a predictable "
						+ "arithmetic sequence (a constant stride), so an observer can infer that they were issued "
						+ "together / to the same holder. Status list indices must be unpredictable (e.g. random).",
					args("uri", entry.getKey(), "indices", distinctSorted.toString(), "stride", stride));
			}
		}

		if (!assessedAny) {
			log("No status list URI has at least " + MIN_INDICES_TO_ASSESS + " credentials in the batch, so "
					+ "index predictability cannot be meaningfully assessed",
				args("status_reference_count", refs.size()));
			return env;
		}

		logSuccess("The status list indices assigned in the batch do not form a predictable sequence",
			args("status_reference_count", refs.size()));
		return env;
	}

	private boolean isArithmeticProgression(List<Long> sortedDistinct) {
		long stride = sortedDistinct.get(1) - sortedDistinct.get(0);
		if (stride == 0) {
			return false;
		}
		for (int i = 2; i < sortedDistinct.size(); i++) {
			if (sortedDistinct.get(i) - sortedDistinct.get(i - 1) != stride) {
				return false;
			}
		}
		return true;
	}
}
