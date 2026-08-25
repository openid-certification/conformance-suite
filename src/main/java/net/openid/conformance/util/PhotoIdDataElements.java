package net.openid.conformance.util;

import java.util.Map;
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

	/**
	 * The value constraints ISO/IEC TS 23220-2 gives for the Table C.1 elements. Elements not
	 * listed are still subject to the generic tstr length limit when encoded as a text string,
	 * which ISO/IEC TS 23220-4 states applies to every tstr data element.
	 *
	 * Unlike ISO/IEC 18013-5, 23220-2 encodes issue_date and expiry_date as full_date only,
	 * portrait_capture_date as tdate only, and allows alpha-3 as well as alpha-2 codes for
	 * issuing_country and nationality (but not for resident_country or Table C.2's birth_country).
	 */
	private static final Map<String, MdocValueConstraint> ISO_23220_2_VALUE_CONSTRAINTS = Map.ofEntries(
		Map.entry("family_name", MdocValueConstraint.tstr()),
		Map.entry("given_name", MdocValueConstraint.tstr()),
		Map.entry("family_name_viz", MdocValueConstraint.tstr()),
		Map.entry("given_name_viz", MdocValueConstraint.tstr()),
		Map.entry("family_name_latin1", MdocValueConstraint.latin1Tstr()),
		Map.entry("given_name_latin1", MdocValueConstraint.latin1Tstr()),
		Map.entry("birth_date", MdocValueConstraint.fullDateOrBirthDateStructure()),
		Map.entry("portrait", MdocValueConstraint.bstr()),
		Map.entry("enrolment_portrait_image", MdocValueConstraint.bstr()),
		Map.entry("issue_date", MdocValueConstraint.fullDate()),
		Map.entry("expiry_date", MdocValueConstraint.fullDate()),
		Map.entry("issuing_authority", MdocValueConstraint.tstr()),
		Map.entry("issuing_country", MdocValueConstraint.alpha2Or3CountryCode()),
		Map.entry("issuing_subdivision", MdocValueConstraint.tstr()),
		Map.entry("age_in_years", MdocValueConstraint.uint()),
		Map.entry("age_birth_year", MdocValueConstraint.uint()),
		Map.entry("portrait_capture_date", MdocValueConstraint.tdate()),
		Map.entry("birthplace", MdocValueConstraint.tstr()),
		Map.entry("name_at_birth", MdocValueConstraint.tstr()),
		Map.entry("resident_address", MdocValueConstraint.tstr()),
		Map.entry("resident_city", MdocValueConstraint.tstr()),
		Map.entry("resident_city_latin1", MdocValueConstraint.latin1Tstr()),
		Map.entry("resident_postal_code", MdocValueConstraint.tstr()),
		Map.entry("resident_country", MdocValueConstraint.alpha2CountryCode()),
		// ISO/IEC 5218, with Table C.1 adding that 9 shall be used for X
		Map.entry("sex", MdocValueConstraint.uintOneOf(Set.of(0L, 1L, 2L, 9L))),
		Map.entry("nationality", MdocValueConstraint.alpha2Or3CountryCode()),
		Map.entry("document_number", MdocValueConstraint.tstr()));

	/** The value constraints Table C.2 gives, all of which are text strings bar the country code. */
	private static final Map<String, MdocValueConstraint> PHOTO_ID_VALUE_CONSTRAINTS = Map.ofEntries(
		Map.entry("person_id", MdocValueConstraint.tstr()),
		Map.entry("birth_country", MdocValueConstraint.alpha2CountryCode()),
		Map.entry("birth_state", MdocValueConstraint.tstr()),
		Map.entry("birth_city", MdocValueConstraint.tstr()),
		Map.entry("administrative_number", MdocValueConstraint.tstr()),
		Map.entry("resident_street", MdocValueConstraint.tstr()),
		Map.entry("resident_house_number", MdocValueConstraint.tstr()),
		// the one Table C.2 element whose definition says "shall only use latin1 characters"
		Map.entry("resident_state", MdocValueConstraint.latin1Tstr()),
		Map.entry("travel_document_type", MdocValueConstraint.tstr()),
		Map.entry("travel_document_number", MdocValueConstraint.tstr()),
		Map.entry("travel_document_mrz", MdocValueConstraint.tstr()));

	/**
	 * The constraint for an element, or null if the specification gives none beyond the generic
	 * tstr length limit. Table C.3's data groups are byte strings apart from version.
	 */
	public static MdocValueConstraint getValueConstraint(String namespace, String elementIdentifier) {
		if (ISO_23220_2_NAMESPACE.equals(namespace)) {
			MdocValueConstraint constraint = ISO_23220_2_VALUE_CONSTRAINTS.get(elementIdentifier);
			if (constraint != null) {
				return constraint;
			}
			return AGE_OVER_NN.matcher(elementIdentifier).matches() ? MdocValueConstraint.bool() : null;
		}
		if (PHOTO_ID_NAMESPACE.equals(namespace)) {
			return PHOTO_ID_VALUE_CONSTRAINTS.get(elementIdentifier);
		}
		if (DATAGROUPS_NAMESPACE.equals(namespace)) {
			if ("version".equals(elementIdentifier)) {
				return MdocValueConstraint.tstr();
			}
			return isDefined(namespace, elementIdentifier) ? MdocValueConstraint.bstr() : null;
		}
		return null;
	}

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
