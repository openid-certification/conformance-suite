package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.MdocUtil;
import org.multipaz.cbor.Cbor;
import org.multipaz.cbor.DataItem;

import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Base class for checks that an issued mdoc credential of a particular docType contains all the
 * data elements its defining specification marks as mandatory. Subclasses supply the docType, the
 * namespace the mandatory elements live in, and the element list. The check passes without doing
 * anything for other docTypes.
 *
 * This check only applies at issuance — in presentations selective disclosure legitimately
 * omits elements the verifier did not request.
 *
 * Reads the docType stored in 'mdoc_doctype' and the raw CBOR stored as standard base64 in
 * 'mdoc_credential_cbor' by ParseMdocCredentialFromVCIIssuance.
 */
public abstract class AbstractEnsureMdocMandatoryDataElementsPresent extends AbstractCondition {

	/** The docType this check applies to; the condition no-ops for any other docType. */
	protected abstract String getDocType();

	/** The namespace the mandatory data elements are defined in. */
	protected abstract String getNamespace();

	/** The data element identifiers the defining specification marks as mandatory. */
	protected abstract Set<String> getMandatoryElements();

	/** Human readable name of the credential, used in log messages, e.g. "mDL". */
	protected abstract String getCredentialName();

	/** The specification the mandatory element list comes from, used in log messages. */
	protected abstract String getSpecificationName();

	@Override
	@PreEnvironment(strings = { "mdoc_credential_cbor", "mdoc_doctype" })
	public Environment evaluate(Environment env) {

		String docType = env.getString("mdoc_doctype");
		if (!getDocType().equals(docType)) {
			log("The credential's docType is not " + getDocType()
				+ " so the " + getCredentialName() + " mandatory data element check does not apply",
				args("doctype", docType));
			return env;
		}

		Set<String> presentElements = new TreeSet<>();
		try {
			byte[] bytes = Base64.getDecoder().decode(env.getString("mdoc_credential_cbor"));
			List<DataItem> items = MdocUtil.getIssuerSignedItems(Cbor.INSTANCE.decode(bytes))
				.getOrDefault(getNamespace(), List.of());
			for (DataItem issuerSignedItemBytes : items) {
				DataItem issuerSignedItem = issuerSignedItemBytes.getAsTaggedEncodedCbor();
				presentElements.add(issuerSignedItem.getOrNull("elementIdentifier").getAsTstr());
			}
		} catch (MdocUtil.MdocParseException e) {
			throw error(e.getMessage(), e);
		} catch (Exception e) {
			throw error("Failed to parse the mdoc credential", e);
		}

		Set<String> missingElements = new TreeSet<>(getMandatoryElements());
		missingElements.removeAll(presentElements);

		if (!missingElements.isEmpty()) {
			throw error("The issued " + getCredentialName() + " does not contain all the data elements in the '"
					+ getNamespace() + "' namespace that " + getSpecificationName() + " defines as mandatory",
				args("missing_elements", missingElements, "present_elements", presentElements));
		}

		logSuccess("The issued " + getCredentialName() + " contains all the data elements that "
				+ getSpecificationName() + " defines as mandatory",
			args("present_elements", presentElements));

		return env;
	}
}
