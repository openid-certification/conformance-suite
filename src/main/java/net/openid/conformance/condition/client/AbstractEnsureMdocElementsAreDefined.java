package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Base class for checks that an mdoc contains no data element its defining specification does not
 * define. Subclasses supply the docType, the specification's element inventory, and the namespace
 * to element identifier mapping, which differs between an issued credential and a presented one.
 *
 * Both specifications this is used for let the issuer put its own data in a namespace of its own
 * (ISO/IEC TS 23220-4 Annex C requirement NS_dual, ISO/IEC 18013-5 13.4.9), so elements in an
 * unrecognised namespace are reported but not treated as a problem. Within a namespace the
 * specification does define, an undefined element identifier is flagged, since it is most likely a
 * misspelling or an element that belongs in a namespace of the issuer's own.
 */
public abstract class AbstractEnsureMdocElementsAreDefined extends AbstractCondition {

	/** The elements to check, keyed by namespace. */
	protected abstract Map<String, List<String>> getElementsByNamespace(Environment env);

	/** Whether the mdoc being checked was issued to the wallet or presented by it. */
	protected abstract String getCredentialDescription();

	/** The docType read from wherever this subclass's data comes from. */
	protected abstract String getDocType(Environment env);

	/** The docType this check applies to; it no-ops for any other. */
	protected abstract String getExpectedDocType();

	/** Human readable name of the credential, used in log messages, e.g. "mDL". */
	protected abstract String getCredentialName();

	/** The specification defining the elements, used in log messages. */
	protected abstract String getSpecificationName();

	/** True if the specification defines data elements for the namespace. */
	protected abstract boolean isKnownNamespace(String namespace);

	/** True if the specification defines the element in the namespace. */
	protected abstract boolean isDefined(String namespace, String elementIdentifier);

	@Override
	public Environment evaluate(Environment env) {

		String docType = getDocType(env);
		if (!getExpectedDocType().equals(docType)) {
			log("The credential's docType is not " + getExpectedDocType() + " so the "
				+ getCredentialName() + " data element check does not apply", args("doctype", docType));
			return env;
		}

		Map<String, Set<String>> undefinedElements = new TreeMap<>();
		Set<String> additionalNamespaces = new TreeSet<>();

		for (Map.Entry<String, List<String>> entry : getElementsByNamespace(env).entrySet()) {
			String namespace = entry.getKey();
			if (!isKnownNamespace(namespace)) {
				additionalNamespaces.add(namespace);
				continue;
			}
			for (String element : entry.getValue()) {
				if (!isDefined(namespace, element)) {
					undefinedElements.computeIfAbsent(namespace, k -> new TreeSet<>()).add(element);
				}
			}
		}

		if (!additionalNamespaces.isEmpty()) {
			// The specification permits these, so this is information rather than a finding.
			log("The " + getCredentialDescription() + " " + getCredentialName() + " uses namespaces "
					+ getSpecificationName() + " does not define. That is permitted, so the data "
					+ "elements in them have not been checked",
				args("additional_namespaces", additionalNamespaces));
		}

		if (!undefinedElements.isEmpty()) {
			throw error("The " + getCredentialDescription() + " " + getCredentialName() + " contains "
					+ "data elements that " + getSpecificationName() + " does not define for the "
					+ "namespace they appear in. Such elements belong in an issuer defined namespace",
				args("undefined_elements", undefinedElements));
		}

		logSuccess("Every data element in the " + getCredentialDescription() + " " + getCredentialName()
			+ " is one " + getSpecificationName() + " defines");

		return env;
	}
}
