package net.openid.conformance.condition.client;

import org.multipaz.documenttype.knowntypes.DrivingLicense;

import java.util.Set;

/**
 * Checks that an issued mdoc credential with docType org.iso.18013.5.1.mDL contains all the
 * data elements that ISO/IEC 18013-5:2021 §7.2.1 Table 5 marks as mandatory in the
 * org.iso.18013.5.1 namespace. The check passes without doing anything for other docTypes.
 */
public class EnsureMdocMdlMandatoryDataElementsPresent
		extends AbstractEnsureMdocMandatoryDataElementsPresent {

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
	protected String getDocType() {
		return DrivingLicense.MDL_DOCTYPE;
	}

	@Override
	protected String getNamespace() {
		return DrivingLicense.MDL_NAMESPACE;
	}

	@Override
	protected Set<String> getRequiredElements() {
		return MANDATORY_ELEMENTS;
	}

	@Override
	protected String getCredentialName() {
		return "mDL";
	}

	@Override
	protected String getSpecificationName() {
		return "ISO/IEC 18013-5";
	}
}
