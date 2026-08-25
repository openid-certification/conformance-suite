package net.openid.conformance.util.validation;

import com.google.gson.JsonObject;
import com.networknt.schema.Error;
import com.networknt.schema.path.NodePath;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class JsonSchemaValidationResult {

	private final List<Error> validationMessages;

	// Lazily computed on first use by structuralErrors()/unknownPropertyErrors(); not synchronised,
	// so a result must not be shared across threads (conditions evaluate on a single thread).
	private Partition partition;

	public JsonSchemaValidationResult(List<Error> validationMessages) {
		this.validationMessages = validationMessages;
	}

	public boolean isValid() {
		return validationMessages.isEmpty();
	}

	public List<Error> getValidationMessages() {
		return validationMessages;
	}

	/**
	 * The validation errors that are not attributable to unknown properties: everything except
	 * direct {@code additionalProperties}/{@code unevaluatedProperties} errors and oneOf/anyOf
	 * failures whose only cause is unknown properties (see {@link #unknownPropertyErrors()}).
	 * Empty iff the input would validate against a schema whose unknown-property strictness was
	 * removed.
	 */
	public JsonSchemaValidationResult structuralErrors() {
		return new JsonSchemaValidationResult(partition().structural);
	}

	/**
	 * The validation errors attributable to unknown properties in the input. A flat filter on the
	 * {@code additionalProperties}/{@code unevaluatedProperties} keywords is not enough: when a
	 * oneOf/anyOf fails, the error list contains every branch's errors, so the branch the input
	 * was really aimed at contributes its unknown-property errors while the sibling branches
	 * reject perfectly well-known properties as "additional". This classifies errors by the
	 * oneOf/anyOf branch recorded in their evaluation path: a composite failure is attributed to
	 * unknown properties only when some branch fails purely because of them (recursively), and
	 * then only that branch's unknown-property errors are reported; a composite where every
	 * branch has a genuine structural error is reported via {@link #structuralErrors()} instead.
	 *
	 * <p>"Some branch fails purely on unknown properties" is a heuristic for "that branch is the
	 * one the input was aimed at", not an invariant: if a payload can make an <em>unintended</em>
	 * branch fail on unknown properties alone (e.g. one mixing well-known members of two oneOf
	 * branches whose only difference is which members they reject as additional), that branch's
	 * rejections of well-known properties are misreported as unknown ones - and any genuine
	 * structural error in the intended branch is dropped. The suite's schemas avoid that shape by
	 * giving every composition branch a discriminator that fails structurally (a required/type
	 * mismatch, or {@code false} property schemas for the other branches' members), which stops an
	 * unintended branch being attributable at all.</p>
	 *
	 * <p>Composite applications are grouped by schema evaluation path, not by instance location, so
	 * two instances failing the same oneOf are classified together; a genuine structural error in
	 * one instance then also suppresses the attribution for the other. With several instances under
	 * one composite the attributed branch is chosen for the group, so a known property rejected by
	 * the branch a <em>different</em> instance was aimed at could in principle be reported - the
	 * structural discriminators above prevent this too.</p>
	 */
	public JsonSchemaValidationResult unknownPropertyErrors() {
		return new JsonSchemaValidationResult(partition().unknown);
	}

	private static boolean isUnknownPropertyError(Error m) {
		String type = m.getKeyword();
		return "additionalProperties".equals(type) || "unevaluatedProperties".equals(type);
	}

	private Partition partition() {
		if (partition == null) {
			partition = new Partition();
			classify(validationMessages, 0, partition);
		}
		return partition;
	}

	private static final class Partition {
		private final List<Error> structural = new ArrayList<>();
		private final List<Error> unknown = new ArrayList<>();
	}

	private static void classify(List<Error> errors, int fromIndex, Partition partition) {
		Map<NodePath, List<Error>> compositeGroups = new LinkedHashMap<>();
		for (Error error : errors) {
			NodePath composite = compositePrefix(error, fromIndex);
			if (composite != null) {
				compositeGroups.computeIfAbsent(composite, k -> new ArrayList<>()).add(error);
			} else if (isUnknownPropertyError(error)) {
				partition.unknown.add(error);
			} else {
				partition.structural.add(error);
			}
		}
		for (Map.Entry<NodePath, List<Error>> group : compositeGroups.entrySet()) {
			classifyComposite(group.getKey().getNameCount(), group.getValue(), partition);
		}
	}

	private static void classifyComposite(int prefixLength, List<Error> errors, Partition partition) {
		// The composite's own summary error (oneOf reports "must be valid to one and only one
		// schema"; anyOf produces no summary in this validator) vs the per-branch errors, keyed
		// by the branch index that follows the composite keyword in the evaluation path.
		List<Error> summaryErrors = new ArrayList<>();
		Map<Object, List<Error>> branches = new LinkedHashMap<>();
		for (Error error : errors) {
			NodePath path = error.getEvaluationPath();
			if (path.getNameCount() == prefixLength) {
				summaryErrors.add(error);
			} else {
				branches.computeIfAbsent(path.getElement(prefixLength), k -> new ArrayList<>()).add(error);
			}
		}
		List<Partition> branchPartitions = new ArrayList<>();
		for (List<Error> branchErrors : branches.values()) {
			Partition branchPartition = new Partition();
			classify(branchErrors, prefixLength + 1, branchPartition);
			branchPartitions.add(branchPartition);
		}
		Partition matchable = null;
		for (Partition candidate : branchPartitions) {
			if (candidate.structural.isEmpty() && (matchable == null || candidate.unknown.size() < matchable.unknown.size())) {
				matchable = candidate;
			}
		}
		if (matchable != null) {
			// Some branch fails purely on unknown properties: the input was aimed at that branch,
			// so attribute its unknown properties and drop the sibling branches' artefacts.
			partition.unknown.addAll(matchable.unknown);
		} else {
			partition.structural.addAll(summaryErrors);
			for (Partition candidate : branchPartitions) {
				partition.structural.addAll(candidate.structural);
				partition.structural.addAll(candidate.unknown);
			}
		}
	}

	/**
	 * The evaluation path of the outermost oneOf/anyOf applicator (at or after {@code fromIndex})
	 * this error was produced under, or null for an error outside any composite. The path prefix
	 * includes the composite keyword itself but not the branch index.
	 */
	private static NodePath compositePrefix(Error error, int fromIndex) {
		NodePath path = error.getEvaluationPath();
		int count = path.getNameCount();
		for (int i = fromIndex; i < count; i++) {
			Object element = path.getElement(i);
			if (!"oneOf".equals(element) && !"anyOf".equals(element)) {
				continue;
			}
			if (i > 0 && "properties".equals(path.getElement(i - 1))) {
				// a property literally named oneOf/anyOf, not the applicator
				continue;
			}
			if (i == count - 1) {
				if (element.equals(error.getKeyword())) {
					return path;
				}
			} else if (path.getElement(i + 1) instanceof Integer) {
				return prefixOf(path, i + 1);
			}
		}
		return null;
	}

	private static NodePath prefixOf(NodePath path, int length) {
		NodePath prefix = path;
		for (int i = path.getNameCount(); i > length; i--) {
			prefix = prefix.getParent();
		}
		return prefix;
	}

	public List<JsonObject> getPropertyErrors() {
		List<JsonObject> propertyErrorsWithPaths = new ArrayList<>();
		for (Error error : validationMessages) {
			JsonObject propertyError = new JsonObject();
			propertyError.addProperty("error", error.getMessage());
			if (error.getProperty() != null) {
				propertyError.addProperty("property", error.getProperty());
			}
			propertyError.addProperty("path", JsonSchemaValidation.toInstancePropertyPath(error.getInstanceLocation(), error.getProperty()));
			propertyErrorsWithPaths.add(propertyError);
		}
		return propertyErrorsWithPaths;
	}
}
