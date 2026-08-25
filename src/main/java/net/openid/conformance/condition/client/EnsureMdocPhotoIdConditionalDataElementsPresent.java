package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.MdocUtil;
import net.openid.conformance.util.PhotoIdDataElements;
import org.multipaz.cbor.Cbor;
import org.multipaz.cbor.DataItem;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Checks the conditionally required data elements of an issued photo ID, from the rows
 * ISO/IEC DTS 23220-4:2025 Annex C marks with presence "C":
 *
 * - Table C.3: dg1, dg2 and sod "shall be present when this namespace exists", so if the
 *   org.iso.23220.datagroups.1 namespace is used at all, all three must be in it.
 * - Table C.2: travel_document_type and travel_document_mrz "shall be present if dg1 data element
 *   from table C.3 exists".
 *
 * Only applies at issuance: in a presentation selective disclosure legitimately omits elements
 * the verifier did not ask for, so nothing can be concluded from an absent element.
 */
public class EnsureMdocPhotoIdConditionalDataElementsPresent extends AbstractCondition {

	@Override
	@PreEnvironment(strings = { "mdoc_credential_cbor", "mdoc_doctype" })
	public Environment evaluate(Environment env) {

		String docType = env.getString("mdoc_doctype");
		if (!PhotoIdDataElements.PHOTO_ID_DOCTYPE.equals(docType)) {
			log("The credential's docType is not " + PhotoIdDataElements.PHOTO_ID_DOCTYPE
				+ " so the photo ID conditional data element check does not apply",
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

		if (!namespaces.containsKey(PhotoIdDataElements.DATAGROUPS_NAMESPACE)) {
			logSuccess("The photo ID does not use the " + PhotoIdDataElements.DATAGROUPS_NAMESPACE
				+ " namespace, so no conditionally required data elements apply");
			return env;
		}

		Set<String> dataGroupElements = elementNames(namespaces, PhotoIdDataElements.DATAGROUPS_NAMESPACE);

		Set<String> missing = new TreeSet<>(PhotoIdDataElements.CONDITIONAL_DATAGROUP_ELEMENTS);
		missing.removeAll(dataGroupElements);
		if (!missing.isEmpty()) {
			throw error("The photo ID uses the " + PhotoIdDataElements.DATAGROUPS_NAMESPACE
					+ " namespace, so ISO/IEC TS 23220-4 Table C.3 requires dg1, dg2 and sod to be present",
				args("missing_elements", missing, "present_elements", dataGroupElements));
		}

		Set<String> photoIdElements = elementNames(namespaces, PhotoIdDataElements.PHOTO_ID_NAMESPACE);
		Set<String> missingTravel = new TreeSet<>(PhotoIdDataElements.ELEMENTS_REQUIRED_WHEN_DG1_PRESENT);
		missingTravel.removeAll(photoIdElements);
		if (!missingTravel.isEmpty()) {
			throw error("The photo ID contains the dg1 data element, so ISO/IEC TS 23220-4 Table C.2 "
					+ "requires travel_document_type and travel_document_mrz to be present",
				args("missing_elements", missingTravel, "present_elements", photoIdElements));
		}

		logSuccess("The photo ID contains the conditionally required data elements",
			args("datagroup_elements", dataGroupElements, "photo_id_elements", photoIdElements));

		return env;
	}

	private Set<String> elementNames(Map<String, List<DataItem>> namespaces, String namespace) {
		Set<String> names = new TreeSet<>();
		for (DataItem issuerSignedItemBytes : namespaces.getOrDefault(namespace, List.of())) {
			names.add(issuerSignedItemBytes.getAsTaggedEncodedCbor()
				.getOrNull("elementIdentifier").getAsTstr());
		}
		return names;
	}
}
