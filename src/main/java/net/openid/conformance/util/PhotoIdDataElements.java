package net.openid.conformance.util;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * The data elements ISO/IEC TS 23220-4 Annex C defines for the photo ID document type
 * (org.iso.23220.photoid.1).
 *
 * The lists here are transcribed from Annex C Tables C.1, C.2 and C.3 of ISO/IEC DTS 23220-4:2025
 * (WG4 N 4871, 2025-12-08), with the presence column
 * "M" mandatory, "R" recommended, "O" optional and "C" conditional. Annex C is nominally an
 * informative annex, but it is the only definition of the photo ID profile implementers have,
 * so this suite treats it as normative.
 *
 * Table C.1 uses the ISO/IEC TS 23220-2 ed.2 identifiers, which dropped the "_unicode" suffix the
 * previous edition used (family_name_unicode, given_name_unicode, issuing_authority_unicode,
 * resident_address_unicode, resident_city_unicode). Earlier 23220-4 drafts, for example WG4 N 4583
 * of 2024, still show the "_unicode" names, and also put the ICAO data groups of Table C.3 in an
 * "org.iso.23220.dtc.1" namespace with "dtc_" prefixed identifiers.
 */
public final class PhotoIdDataElements {

	private PhotoIdDataElements() {
		// constants only
	}

	public static final String PHOTO_ID_DOCTYPE = "org.iso.23220.photoid.1";

	/** Table 1's namespace, the generic eID data elements of ISO/IEC TS 23220-2. */
	public static final String ISO_23220_2_NAMESPACE = "org.iso.23220.1";

	/** Table 2's namespace, the elements defined specifically for photo ID. */
	public static final String PHOTO_ID_NAMESPACE = "org.iso.23220.photoid.1";

	/** Table C.3's namespace, the ICAO 9303 part 10 data groups. */
	public static final String DATAGROUPS_NAMESPACE = "org.iso.23220.datagroups.1";

	/** Age attestation identifiers, age_over_00 to age_over_99. ISO/IEC TS 23220-2 6.3.2.2. */
	private static final Pattern AGE_OVER_NN = Pattern.compile("age_over_\\d\\d");

	/** Table C.1, presence "M". */
	public static final Set<String> MANDATORY_ELEMENTS = Set.of(
		"family_name",
		"given_name",
		"birth_date",
		"portrait",
		"issue_date",
		"expiry_date",
		"issuing_authority",
		"issuing_country",
		"age_over_18");

	/**
	 * Table C.1, presence "R". age_over_NN is also recommended, but age_over_18 is already
	 * mandatory so it is not repeated here.
	 */
	public static final Set<String> RECOMMENDED_ELEMENTS = Set.of(
		"age_in_years",
		"age_birth_year");

	/** Table C.1, presence "O". */
	private static final Set<String> OPTIONAL_ISO_23220_2_ELEMENTS = Set.of(
		"family_name_viz",
		"given_name_viz",
		"enrolment_portrait_image",
		"portrait_capture_date",
		"birthplace",
		"name_at_birth",
		"resident_address",
		"resident_city",
		"resident_postal_code",
		"resident_country",
		"resident_city_latin1",
		"sex",
		"nationality",
		"document_number",
		"issuing_subdivision",
		"family_name_latin1",
		"given_name_latin1");

	/** Table C.2, presence "O". */
	private static final Set<String> OPTIONAL_PHOTO_ID_ELEMENTS = Set.of(
		"person_id",
		"birth_country",
		"birth_state",
		"birth_city",
		"administrative_number",
		"resident_street",
		"resident_house_number",
		"resident_state");

	/**
	 * Table C.2, presence "C": travel_document_type and travel_document_mrz "shall be present if
	 * dg1 data element from table C.3 exists but optional otherwise". travel_document_number is
	 * also marked "C", but its definition gives the condition as "if associated or derived from a
	 * travel document", which is not something this suite can determine, so it is not enforced.
	 */
	public static final Set<String> ELEMENTS_REQUIRED_WHEN_DG1_PRESENT = Set.of(
		"travel_document_type",
		"travel_document_mrz");

	private static final Set<String> CONDITIONAL_PHOTO_ID_ELEMENTS = Set.of(
		"travel_document_type",
		"travel_document_number",
		"travel_document_mrz");

	/**
	 * Table C.3, presence "C": "The data element shall be present when this namespace exists".
	 */
	public static final Set<String> CONDITIONAL_DATAGROUP_ELEMENTS = Set.of(
		"dg1",
		"dg2",
		"sod");

	/** Table C.3, presence "O". */
	private static final Set<String> OPTIONAL_DATAGROUP_ELEMENTS = Set.of(
		"version",
		"dg3", "dg4", "dg5", "dg6", "dg7", "dg8", "dg9",
		"dg10", "dg11", "dg12", "dg13", "dg14", "dg15", "dg16");

	/** True if the namespace is one of the three Annex C defines. */
	public static boolean isKnownNamespace(String namespace) {
		return ISO_23220_2_NAMESPACE.equals(namespace)
			|| PHOTO_ID_NAMESPACE.equals(namespace)
			|| DATAGROUPS_NAMESPACE.equals(namespace);
	}

	/**
	 * True if Annex C defines the element in the given namespace. Only meaningful for the three
	 * namespaces {@link #isKnownNamespace} recognises — Annex C explicitly permits issuers to use
	 * additional namespaces, whose contents this class knows nothing about.
	 */
	public static boolean isDefined(String namespace, String elementIdentifier) {
		if (ISO_23220_2_NAMESPACE.equals(namespace)) {
			return MANDATORY_ELEMENTS.contains(elementIdentifier)
				|| RECOMMENDED_ELEMENTS.contains(elementIdentifier)
				|| OPTIONAL_ISO_23220_2_ELEMENTS.contains(elementIdentifier)
				|| AGE_OVER_NN.matcher(elementIdentifier).matches();
		}
		if (PHOTO_ID_NAMESPACE.equals(namespace)) {
			return OPTIONAL_PHOTO_ID_ELEMENTS.contains(elementIdentifier)
				|| CONDITIONAL_PHOTO_ID_ELEMENTS.contains(elementIdentifier);
		}
		if (DATAGROUPS_NAMESPACE.equals(namespace)) {
			return CONDITIONAL_DATAGROUP_ELEMENTS.contains(elementIdentifier)
				|| OPTIONAL_DATAGROUP_ELEMENTS.contains(elementIdentifier);
		}
		return false;
	}
}
