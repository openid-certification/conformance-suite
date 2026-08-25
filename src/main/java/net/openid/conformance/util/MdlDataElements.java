package net.openid.conformance.util;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * The data elements ISO/IEC 18013-5 13.4.2 Table 20 defines for the mDL document type
 * (org.iso.18013.5.1.mDL), in the org.iso.18013.5.1 namespace.
 *
 * 13.4.1 says "Within this NameSpace, only data elements defined in 13.4.2 may be used", and lets
 * an issuing authority define its own namespace for domestic data instead.
 */
public final class MdlDataElements {

	private MdlDataElements() {
		// constants only
	}

	public static final String MDL_DOCTYPE = "org.iso.18013.5.1.mDL";

	public static final String MDL_NAMESPACE = "org.iso.18013.5.1";

	/** Age attestation identifiers, age_over_00 to age_over_99. See ISO/IEC 18013-5 13.4.6. */
	private static final Pattern AGE_OVER_NN = Pattern.compile("age_over_\\d\\d");

	/**
	 * Biometric template identifiers. 13.4.7 defines the format as biometric_template_xx, where xx
	 * is an abstract value name from ISO/IEC 19785-3:2020 Table 7, lowercased with non alphanumeric
	 * characters replaced by underscores, for example biometric_template_face and
	 * biometric_template_signature_sign. That table is not reproduced in 18013-5, so this matches
	 * the shape of the identifier rather than a fixed list of suffixes.
	 */
	private static final Pattern BIOMETRIC_TEMPLATE = Pattern.compile("biometric_template_[a-z0-9]+(_[a-z0-9]+)*");

	/** Table 20, presence "M". */
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

	/** Table 20, presence "O". */
	private static final Set<String> OPTIONAL_ELEMENTS = Set.of(
		"administrative_number",
		"sex",
		"height",
		"weight",
		"eye_colour",
		"hair_colour",
		"birth_place",
		"resident_address",
		"portrait_capture_date",
		"age_in_years",
		"age_birth_year",
		"issuing_jurisdiction",
		"nationality",
		"resident_street",
		"resident_city",
		"resident_state",
		"resident_postal_code",
		"resident_country",
		"family_name_national_character",
		"given_name_national_character",
		"signature_usual_mark");

	/** True if the namespace is the one 18013-5 defines for mDL data elements. */
	public static boolean isKnownNamespace(String namespace) {
		return MDL_NAMESPACE.equals(namespace);
	}

	/** True if ISO/IEC 18013-5 Table 20 defines the element in the given namespace. */
	public static boolean isDefined(String namespace, String elementIdentifier) {
		if (!MDL_NAMESPACE.equals(namespace)) {
			return false;
		}
		return MANDATORY_ELEMENTS.contains(elementIdentifier)
			|| OPTIONAL_ELEMENTS.contains(elementIdentifier)
			|| AGE_OVER_NN.matcher(elementIdentifier).matches()
			|| BIOMETRIC_TEMPLATE.matcher(elementIdentifier).matches();
	}
}
