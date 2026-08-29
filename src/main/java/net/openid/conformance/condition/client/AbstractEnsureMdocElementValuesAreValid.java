package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.MdocUtil;
import net.openid.conformance.util.MdocValueConstraint;
import org.multipaz.cbor.Cbor;
import org.multipaz.cbor.DataItem;
import org.multipaz.cbor.Tstr;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Base class for checks that an mdoc's data element values match the encoding and the value
 * constraints its defining specification gives.
 *
 * This works for both an issued credential and a presented one: ParseMdocCredentialFromVCIIssuance
 * and ParseCredentialAsMdoc both store the IssuerSigned CBOR in 'mdoc_credential_cbor', and in a
 * presentation that contains exactly the elements the wallet disclosed.
 *
 * Only elements in a namespace the specification defines are checked. Elements in an issuer's own
 * namespace are outside the specification's scope, and elements the specification does not define
 * are reported by the separate {@link AbstractEnsureMdocElementsAreDefined} check.
 */
public abstract class AbstractEnsureMdocElementValuesAreValid extends AbstractCondition {

	protected abstract String getExpectedDocType();

	protected abstract String getCredentialName();

	protected abstract String getSpecificationName();

	protected abstract boolean isKnownNamespace(String namespace);

	/** The constraint for the element, or null if the specification gives none. */
	protected abstract MdocValueConstraint getValueConstraint(String namespace, String elementIdentifier);

	@Override
	@PreEnvironment(strings = "mdoc_credential_cbor")
	public Environment evaluate(Environment env) {

		String docType = env.getString("mdoc_doctype");
		if (docType == null) {
			docType = env.getString("mdoc", "docType");
		}
		if (!getExpectedDocType().equals(docType)) {
			log("The credential's docType is not " + getExpectedDocType() + " so the "
				+ getCredentialName() + " data element value check does not apply",
				args("doctype", docType));
			return env;
		}

		Map<String, List<DataItem>> namespaces;
		try {
			byte[] bytes = Base64.getDecoder().decode(env.getString("mdoc_credential_cbor"));
			namespaces = MdocUtil.getIssuerSignedItems(Cbor.INSTANCE.decode(bytes));
		} catch (MdocUtil.MdocParseException e) {
			throw error(e.getMessage(), e);
		} catch (Exception e) {
			throw error("Failed to parse the mdoc credential", e);
		}

		Map<String, String> problems = new TreeMap<>();
		int checked = 0;

		for (Map.Entry<String, List<DataItem>> entry : namespaces.entrySet()) {
			String namespace = entry.getKey();
			if (!isKnownNamespace(namespace)) {
				continue;
			}
			for (DataItem issuerSignedItemBytes : entry.getValue()) {
				DataItem issuerSignedItem = issuerSignedItemBytes.getAsTaggedEncodedCbor();
				String element = issuerSignedItem.getOrNull("elementIdentifier").getAsTstr();
				DataItem value = issuerSignedItem.getOrNull("elementValue");

				MdocValueConstraint constraint = getValueConstraint(namespace, element);
				if (constraint == null && value instanceof Tstr) {
					// Both specifications limit every tstr data element to 150 characters, so this
					// applies even where the element itself has no other constraint.
					constraint = MdocValueConstraint.tstr();
				}
				if (constraint == null) {
					continue;
				}
				checked++;
				String problem = constraint.check(value);
				if (problem != null) {
					problems.put(namespace + " " + element, problem);
				}
			}
		}

		if (!problems.isEmpty()) {
			throw error("The " + getCredentialName() + " contains data element values that do not match "
					+ "what " + getSpecificationName() + " defines for them",
				args("problems", problems));
		}

		logSuccess("The " + getCredentialName() + " data element values match what "
			+ getSpecificationName() + " defines for them", args("elements_checked", checked));

		return env;
	}
}
