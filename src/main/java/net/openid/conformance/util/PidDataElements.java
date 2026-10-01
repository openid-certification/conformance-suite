package net.openid.conformance.util;

import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * The attributes the EUDI PID Rulebook defines for a PID in ISO/IEC 18013-5 format
 * (eu.europa.ec.eudi.pid.1).
 *
 * Transcribed from PID Rulebook v1.7 (2026-07-17): the presence of each attribute is given by
 * sections 2.2 to 2.6 and the encoding by the table in section 3.1.2. Sections 2.2 to 2.5
 * reproduce the annex of Commission Implementing Regulation (EU) 2024/2977; section 2.6 adds two
 * optional attributes of the Rulebook's own.
 *
 * portrait is listed in section 2.2 as mandatory, but "mandatory inclusion of the portrait
 * attribute shall apply as of 24 months after entry into force of the Regulation amending CIR
 * 2024/2977" and the user may opt out, so it is not in {@link #MANDATORY_ELEMENTS}.
 */
public final class PidDataElements {

	private PidDataElements() {
		// constants only
	}

	public static final String PID_DOCTYPE = "eu.europa.ec.eudi.pid.1";

	/** Section 3.1.1 uses the same identifier for the namespace as for the attestation type. */
	public static final String PID_NAMESPACE = "eu.europa.ec.eudi.pid.1";

	/** Sections 2.2 (attributes) and 2.4 (metadata), using the attribute identifiers of 3.1.2. */
	public static final Set<String> MANDATORY_ELEMENTS = Set.of(
		"family_name",
		"given_name",
		"birth_date",
		"place_of_birth",
		"nationality",
		"issuing_authority",
		"issuing_country");

	/** Section 2.3: "starting with the '+' symbol ... followed by numbers only". */
	private static final Pattern MOBILE_PHONE_NUMBER = Pattern.compile("\\+\\d+");

	/**
	 * Every attribute in the section 3.1.2 table with its encoding, plus the value restrictions
	 * the definitions in sections 2.2 to 2.5 add.
	 */
	private static final Map<String, MdocValueConstraint> VALUE_CONSTRAINTS = Map.ofEntries(
		Map.entry("family_name", MdocValueConstraint.tstr()),
		Map.entry("given_name", MdocValueConstraint.tstr()),
		Map.entry("birth_date", MdocValueConstraint.fullDate()),
		Map.entry("place_of_birth", MdocValueConstraint.placeOfBirth()),
		Map.entry("nationality", MdocValueConstraint.alpha2CountryCodeArray()),
		Map.entry("resident_address", MdocValueConstraint.tstr()),
		Map.entry("resident_country", MdocValueConstraint.alpha2CountryCode()),
		Map.entry("resident_state", MdocValueConstraint.tstr()),
		Map.entry("resident_city", MdocValueConstraint.tstr()),
		Map.entry("resident_postal_code", MdocValueConstraint.tstr()),
		Map.entry("resident_street", MdocValueConstraint.tstr()),
		Map.entry("personal_administrative_number", MdocValueConstraint.tstr()),
		// empty when the user has opted out of the portrait
		Map.entry("portrait", MdocValueConstraint.bstr()),
		Map.entry("family_name_birth", MdocValueConstraint.tstr()),
		Map.entry("given_name_birth", MdocValueConstraint.tstr()),
		Map.entry("sex", MdocValueConstraint.uintOneOf(Set.of(0L, 1L, 2L, 3L, 4L, 5L, 6L, 9L))),
		Map.entry("email_address", MdocValueConstraint.tstr()),
		Map.entry("mobile_phone_number", MdocValueConstraint.tstrMatching(MOBILE_PHONE_NUMBER,
			"a '+' followed by digits only")),
		Map.entry("expiry_date", MdocValueConstraint.pidTdateOrFullDate()),
		Map.entry("issuing_authority", MdocValueConstraint.tstr()),
		Map.entry("issuing_country", MdocValueConstraint.alpha2CountryCode()),
		Map.entry("document_number", MdocValueConstraint.tstr()),
		Map.entry("issuing_jurisdiction", MdocValueConstraint.tstr()),
		Map.entry("issuance_date", MdocValueConstraint.pidTdateOrFullDate()),
		Map.entry("trust_anchor", MdocValueConstraint.tstr()),
		Map.entry("attestation_legal_category", MdocValueConstraint.tstr()));

	/** The constraint for an attribute, or null if the Rulebook does not define the attribute. */
	public static MdocValueConstraint getValueConstraint(String namespace, String elementIdentifier) {
		return PID_NAMESPACE.equals(namespace) ? VALUE_CONSTRAINTS.get(elementIdentifier) : null;
	}

	/**
	 * True for the one namespace the Rulebook defines. Domestic namespaces
	 * (eu.europa.ec.eudi.pid.[country code]...) hold attributes a Member State defines itself,
	 * which this class knows nothing about.
	 */
	public static boolean isKnownNamespace(String namespace) {
		return PID_NAMESPACE.equals(namespace);
	}

	/** True if the section 3.1.2 table lists the attribute. */
	public static boolean isDefined(String namespace, String elementIdentifier) {
		return PID_NAMESPACE.equals(namespace) && VALUE_CONSTRAINTS.containsKey(elementIdentifier);
	}
}
