package net.openid.conformance.util;

import org.multipaz.cbor.CborArray;
import org.multipaz.cbor.CborMap;
import org.multipaz.cbor.DataItem;

import java.util.List;
import java.util.Map;
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

	/**
	 * The value constraints of Table 20's "Encoding format" column and the element definitions.
	 * Elements not listed here are still subject to the generic tstr length limit when they are
	 * encoded as a text string. The latin1Tstr elements are those whose Table 20 definition says
	 * "The value shall only use latin1 characters".
	 */
	public static final Map<String, MdocValueConstraint> VALUE_CONSTRAINTS = Map.ofEntries(
		Map.entry("family_name", MdocValueConstraint.latin1Tstr()),
		Map.entry("given_name", MdocValueConstraint.latin1Tstr()),
		Map.entry("birth_date", MdocValueConstraint.fullDate()),
		Map.entry("issue_date", MdocValueConstraint.mdlTdateOrFullDate()),
		Map.entry("expiry_date", MdocValueConstraint.mdlTdateOrFullDate()),
		Map.entry("issuing_country", MdocValueConstraint.alpha2CountryCode()),
		Map.entry("issuing_authority", MdocValueConstraint.latin1Tstr()),
		Map.entry("document_number", MdocValueConstraint.latin1Tstr()),
		Map.entry("portrait", MdocValueConstraint.bstr()),
		Map.entry("driving_privileges", drivingPrivileges()),
		Map.entry("un_distinguishing_sign", MdocValueConstraint.tstr()),
		Map.entry("administrative_number", MdocValueConstraint.latin1Tstr()),
		// ISO/IEC 5218, with 18013-5 defining the meaning of 9 as "not specified"
		Map.entry("sex", MdocValueConstraint.uintOneOf(Set.of(0L, 1L, 2L, 9L))),
		Map.entry("height", MdocValueConstraint.uint()),
		Map.entry("weight", MdocValueConstraint.uint()),
		Map.entry("eye_colour", MdocValueConstraint.oneOf(Set.of(
			"black", "blue", "brown", "dichromatic", "grey", "green", "hazel", "maroon", "pink",
			"unknown"))),
		Map.entry("hair_colour", MdocValueConstraint.oneOf(Set.of(
			"bald", "black", "blond", "brown", "grey", "red", "auburn", "sandy", "white",
			"unknown"))),
		Map.entry("birth_place", MdocValueConstraint.latin1Tstr()),
		Map.entry("resident_address", MdocValueConstraint.latin1Tstr()),
		Map.entry("portrait_capture_date", MdocValueConstraint.mdlTdate()),
		Map.entry("age_in_years", MdocValueConstraint.uint()),
		Map.entry("age_birth_year", MdocValueConstraint.uint()),
		Map.entry("issuing_jurisdiction", MdocValueConstraint.tstr()),
		Map.entry("nationality", MdocValueConstraint.alpha2CountryCode()),
		Map.entry("resident_street", MdocValueConstraint.latin1Tstr()),
		Map.entry("resident_city", MdocValueConstraint.latin1Tstr()),
		Map.entry("resident_state", MdocValueConstraint.latin1Tstr()),
		Map.entry("resident_postal_code", MdocValueConstraint.latin1Tstr()),
		Map.entry("resident_country", MdocValueConstraint.alpha2CountryCode()),
		Map.entry("family_name_national_character", MdocValueConstraint.tstr()),
		Map.entry("given_name_national_character", MdocValueConstraint.tstr()),
		Map.entry("signature_usual_mark", MdocValueConstraint.bstr()));


	/**
	 * The driving_privileges structure of 7.2.4: an array of driving privilege maps, each with a
	 * mandatory vehicle_category_code text string, optional issue_date and expiry_date full-dates,
	 * and an optional codes array of maps each holding a mandatory code text string with optional
	 * sign and value text strings. The code values themselves come from ISO/IEC 18013-1 and
	 * ISO/IEC 18013-2 tables this suite does not transcribe.
	 */
	private static MdocValueConstraint drivingPrivileges() {
		return value -> {
			if (!(value instanceof CborArray array)) {
				return "expected an array of driving privilege maps but found "
					+ MdocValueConstraint.describe(value);
			}
			List<DataItem> privileges = array.getItems();
			for (int index = 0; index < privileges.size(); index++) {
				DataItem privilege = privileges.get(index);
				String where = "driving privilege " + index;
				if (!(privilege instanceof CborMap)) {
					return where + " is not a map";
				}
				String problem = MdocValueConstraint.tstr().check(privilege.getOrNull("vehicle_category_code"));
				if (problem != null) {
					return where + "'s vehicle_category_code is invalid: " + problem;
				}
				for (String dateField : List.of("issue_date", "expiry_date")) {
					DataItem date = privilege.getOrNull(dateField);
					if (date != null) {
						problem = MdocValueConstraint.fullDate().check(date);
						if (problem != null) {
							return where + "'s " + dateField + " is invalid: " + problem;
						}
					}
				}
				DataItem codes = privilege.getOrNull("codes");
				if (codes == null) {
					continue;
				}
				if (!(codes instanceof CborArray codesArray)) {
					return where + "'s codes is not an array";
				}
				for (DataItem code : codesArray.getItems()) {
					if (!(code instanceof CborMap)) {
						return where + " has a codes entry that is not a map";
					}
					problem = MdocValueConstraint.tstr().check(code.getOrNull("code"));
					if (problem != null) {
						return where + " has a codes entry whose code is invalid: " + problem;
					}
					for (String optional : List.of("sign", "value")) {
						DataItem field = code.getOrNull(optional);
						if (field != null) {
							problem = MdocValueConstraint.tstr().check(field);
							if (problem != null) {
								return where + " has a codes entry whose " + optional + " is invalid: " + problem;
							}
						}
					}
				}
			}
			return null;
		};
	}

	/**
	 * The constraint for an element, or null if the specification gives none beyond the generic
	 * tstr length limit. age_over_NN is a bool; biometric templates are byte strings.
	 */
	public static MdocValueConstraint getValueConstraint(String elementIdentifier) {
		MdocValueConstraint constraint = VALUE_CONSTRAINTS.get(elementIdentifier);
		if (constraint != null) {
			return constraint;
		}
		if (AGE_OVER_NN.matcher(elementIdentifier).matches()) {
			return MdocValueConstraint.bool();
		}
		if (BIOMETRIC_TEMPLATE.matcher(elementIdentifier).matches()) {
			return MdocValueConstraint.bstr();
		}
		return null;
	}

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
