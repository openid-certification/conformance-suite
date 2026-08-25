package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.PhotoIdDataElements;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Base class for checks that a photo ID contains no data element that ISO/IEC TS 23220-4 Annex C
 * does not define. Subclasses supply the docType and the namespace to element identifier mapping,
 * which differ between an issued credential and a presented one.
 *
 * Annex C explicitly permits issuers to use additional namespaces ("national, state/territory,
 * sectoral or international"), so elements in a namespace other than the three Annex C defines
 * are reported but not treated as a problem. Within those three namespaces an undefined element
 * identifier is flagged, since it is most likely a misspelling or an element the issuer should
 * have put in a namespace of its own.
 */
public abstract class AbstractEnsureMdocPhotoIdElementsAreDefined extends AbstractCondition {

	/** The elements to check, keyed by namespace. */
	protected abstract Map<String, List<String>> getElementsByNamespace(Environment env);

	/** Whether the mdoc being checked was issued to the wallet or presented by it. */
	protected abstract String getCredentialDescription();

	protected abstract String getDocType(Environment env);

	@Override
	public Environment evaluate(Environment env) {

		String docType = getDocType(env);
		if (!PhotoIdDataElements.PHOTO_ID_DOCTYPE.equals(docType)) {
			log("The credential's docType is not " + PhotoIdDataElements.PHOTO_ID_DOCTYPE
				+ " so the photo ID data element check does not apply", args("doctype", docType));
			return env;
		}

		Map<String, Set<String>> undefinedElements = new TreeMap<>();
		Set<String> additionalNamespaces = new TreeSet<>();

		for (Map.Entry<String, List<String>> entry : getElementsByNamespace(env).entrySet()) {
			String namespace = entry.getKey();
			if (!PhotoIdDataElements.isKnownNamespace(namespace)) {
				additionalNamespaces.add(namespace);
				continue;
			}
			for (String element : entry.getValue()) {
				if (!PhotoIdDataElements.isDefined(namespace, element)) {
					undefinedElements.computeIfAbsent(namespace, k -> new TreeSet<>()).add(element);
				}
			}
		}

		if (!additionalNamespaces.isEmpty()) {
			// Annex C requirement NS_dual permits these, so this is information rather than a finding.
			log("The " + getCredentialDescription() + " photo ID uses namespaces beyond the three "
					+ "ISO/IEC TS 23220-4 Annex C defines. Annex C permits this, so the data elements "
					+ "in them have not been checked",
				args("additional_namespaces", additionalNamespaces));
		}

		if (!undefinedElements.isEmpty()) {
			throw error("The " + getCredentialDescription() + " photo ID contains data elements that "
					+ "ISO/IEC TS 23220-4 Annex C does not define for the namespace they appear in. "
					+ "Elements not defined by Annex C belong in an issuer defined namespace",
				args("undefined_elements", undefinedElements));
		}

		logSuccess("Every data element in the " + getCredentialDescription()
			+ " photo ID is one ISO/IEC TS 23220-4 Annex C defines");

		return env;
	}
}
