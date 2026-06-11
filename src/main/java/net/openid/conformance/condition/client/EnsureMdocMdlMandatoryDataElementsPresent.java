package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.MdocUtil;
import org.multipaz.cbor.Cbor;
import org.multipaz.cbor.DataItem;
import org.multipaz.documenttype.knowntypes.DrivingLicense;

import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Checks that an issued mdoc credential with docType org.iso.18013.5.1.mDL contains all the
 * data elements that ISO/IEC 18013-5:2021 §7.2.1 Table 5 marks as mandatory in the
 * org.iso.18013.5.1 namespace. The check passes without doing anything for other docTypes
 * (this suite defines no mandatory element list for them).
 *
 * This check only applies at issuance — in presentations selective disclosure legitimately
 * omits elements the verifier did not request.
 *
 * Reads the docType stored in 'mdoc_doctype' and the raw CBOR stored as standard base64 in
 * 'mdoc_credential_cbor' by ParseMdocCredentialFromVCIIssuance.
 */
public class EnsureMdocMdlMandatoryDataElementsPresent extends AbstractCondition {

	/** ISO/IEC 18013-5:2021 §7.2.1 Table 5, elements with presence "M". */
	public static final Set<String> MANDATORY_ELEMENTS = Set.of(
		"family_name",
		"given_name",
		"birth_date",
		"issue_date",
		"expiry_date",
		"issuing_country",
		"issuing_authority",
		"document_number",
		"portrait",
		"driving_privileges",
		"un_distinguishing_sign");

	@Override
	@PreEnvironment(strings = { "mdoc_credential_cbor", "mdoc_doctype" })
	public Environment evaluate(Environment env) {

		String docType = env.getString("mdoc_doctype");
		if (!DrivingLicense.MDL_DOCTYPE.equals(docType)) {
			log("The credential's docType is not " + DrivingLicense.MDL_DOCTYPE
				+ " so the mDL mandatory data element check does not apply", args("doctype", docType));
			return env;
		}

		Set<String> presentElements = new TreeSet<>();
		try {
			byte[] bytes = Base64.getDecoder().decode(env.getString("mdoc_credential_cbor"));
			List<DataItem> mdlItems = MdocUtil.getIssuerSignedItems(Cbor.INSTANCE.decode(bytes))
				.getOrDefault(DrivingLicense.MDL_NAMESPACE, List.of());
			for (DataItem issuerSignedItemBytes : mdlItems) {
				DataItem issuerSignedItem = issuerSignedItemBytes.getAsTaggedEncodedCbor();
				presentElements.add(issuerSignedItem.getOrNull("elementIdentifier").getAsTstr());
			}
		} catch (MdocUtil.MdocParseException e) {
			throw error(e.getMessage(), e);
		} catch (Exception e) {
			throw error("Failed to parse the mdoc credential", e);
		}

		Set<String> missingElements = new TreeSet<>(MANDATORY_ELEMENTS);
		missingElements.removeAll(presentElements);

		if (!missingElements.isEmpty()) {
			throw error("The issued mDL does not contain all the data elements in the '" + DrivingLicense.MDL_NAMESPACE
					+ "' namespace that ISO/IEC 18013-5 defines as mandatory",
				args("missing_elements", missingElements, "present_elements", presentElements));
		}

		logSuccess("The issued mDL contains all the data elements that ISO/IEC 18013-5 defines as mandatory",
			args("present_elements", presentElements));

		return env;
	}
}
