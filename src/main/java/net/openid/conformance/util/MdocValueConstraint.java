package net.openid.conformance.util;

import org.multipaz.cbor.Bstr;
import org.multipaz.cbor.CborArray;
import org.multipaz.cbor.CborInt;
import org.multipaz.cbor.CborMap;
import org.multipaz.cbor.DataItem;
import org.multipaz.cbor.Simple;
import org.multipaz.cbor.Tagged;
import org.multipaz.cbor.Tstr;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A constraint on the value of an mdoc data element, as given by the "Encoding format" column and
 * the definitions of ISO/IEC 18013-5 Table 20, ISO/IEC TS 23220-2 and the EUDI PID Rulebook.
 *
 * Each constraint returns null when the value is acceptable, or a description of what is wrong.
 */
@FunctionalInterface
public interface MdocValueConstraint {

	/** Maximum length all three specifications give for data elements encoded as tstr. */
	int MAX_TSTR_LENGTH = 150;

	/** ISO 3166-1 alpha-2 country codes are two upper case letters. */
	Pattern ALPHA2_COUNTRY_CODE = Pattern.compile("[A-Z]{2}");

	/** ISO 3166-1 alpha-2 or alpha-3 country codes: two or three upper case letters. */
	Pattern ALPHA2_OR_3_COUNTRY_CODE = Pattern.compile("[A-Z]{2,3}");

	/** RFC 3339 full-date is exactly 4DIGIT-2DIGIT-2DIGIT; java.time alone also accepts expanded years. */
	Pattern RFC3339_FULL_DATE = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");

	/**
	 * RFC 3339 date-time grammar: seconds are mandatory, a numeric offset is only HH:MM with an
	 * hour of 00-23 and a minute of 00-59, the fraction is any number of digits, a seconds value
	 * of 60 (a leap second) is permitted, and 'T' and 'Z' may be lower case. java.time's parser
	 * deviates on all of these (ZoneOffset also caps offsets at +/-18:00), so the shape and the
	 * offset are checked against this pattern and the parse in checkDateTimeText only checks the
	 * date and time field ranges.
	 */
	Pattern RFC3339_DATE_TIME = Pattern.compile(
		"(\\d{4}-\\d{2}-\\d{2})[Tt](\\d{2}:\\d{2}):(\\d{2})(\\.\\d+)?"
			+ "([Zz]|[+-](?:[01]\\d|2[0-3]):[0-5]\\d)");

	/**
	 * The approximate_mask of the ISO/IEC TS 23220-2 birth_date structure: "an 8 digit flag to
	 * denote the location of the mask in YYYYMMDD format. 1 denotes mask."
	 */
	Pattern APPROXIMATE_MASK = Pattern.compile("[01]{8}");

	/** The keys the PID Rulebook 3.1.4 defines for the place_of_birth map. */
	Set<String> PLACE_OF_BIRTH_KEYS = Set.of("country", "region", "locality");

	/**
	 * A specification that forbids fractions of seconds and local UTC offsets in the tdate values
	 * of its data elements.
	 */
	record TdateRestriction(String specification, String subject) {
		static final TdateRestriction MDL = new TdateRestriction("ISO/IEC 18013-5", "mDL data elements");
		static final TdateRestriction PID = new TdateRestriction("the PID Rulebook", "PID attributes");
	}

	String check(DataItem value);

	static String describe(DataItem value) {
		return value == null ? "absent" : value.getClass().getSimpleName();
	}

	/** A text string, which all three specifications limit to 150 characters. */
	static MdocValueConstraint tstr() {
		return value -> {
			if (!(value instanceof Tstr)) {
				return "expected a text string (tstr) but found " + describe(value);
			}
			int length = value.getAsTstr().length();
			return length > MAX_TSTR_LENGTH
				? "text is " + length + " characters, which exceeds the maximum of " + MAX_TSTR_LENGTH
				: null;
		};
	}

	/** An unsigned integer. */
	static MdocValueConstraint uint() {
		return value -> value instanceof CborInt && value.getAsNumber() >= 0
			? null
			: "expected an unsigned integer (uint) but found " + describe(value);
	}

	/** A boolean. */
	static MdocValueConstraint bool() {
		return value -> value instanceof Simple && (value.equals(Simple.Companion.getTRUE())
				|| value.equals(Simple.Companion.getFALSE()))
			? null
			: "expected a boolean (bool) but found " + describe(value);
	}

	/** A byte string. */
	static MdocValueConstraint bstr() {
		return value -> value instanceof Bstr
			? null
			: "expected a byte string (bstr) but found " + describe(value);
	}

	/** A full-date, defined as #6.1004(tstr) holding an RFC 3339 full-date. */
	static MdocValueConstraint fullDate() {
		return value -> {
			if (!(value instanceof Tagged tagged) || tagged.getTagNumber() != Tagged.FULL_DATE_STRING) {
				return "expected a full-date, which is CBOR tag " + Tagged.FULL_DATE_STRING
					+ " containing a text string, but found " + describe(value);
			}
			if (!(tagged.getTaggedItem() instanceof Tstr)) {
				return "the full-date's tagged item is not a text string";
			}
			return checkFullDateText(tagged.getTaggedItem().getAsTstr());
		};
	}

	private static String checkFullDateText(String text) {
		if (RFC3339_FULL_DATE.matcher(text).matches()) {
			try {
				LocalDate.parse(text);
				return null;
			} catch (DateTimeParseException e) {
				// invalid month or day; fall through to the error
			}
		}
		return "the full-date's text '" + text + "' is not an RFC 3339 full-date";
	}

	private static String checkDateTimeText(String text, TdateRestriction restriction) {
		Matcher matcher = RFC3339_DATE_TIME.matcher(text);
		if (matcher.matches()) {
			// ISO/IEC 18013-5: for tdate in mDL data elements "fraction of seconds shall not be
			// used" and "no local offset from UTC shall be used, as indicated by ... \u201cZ\u201d".
			// The PID Rulebook 3.1.2 has the same two rules for PID attributes.
			if (restriction != null && matcher.group(4) != null) {
				return "the tdate's text '" + text + "' uses fractions of seconds, which "
					+ restriction.specification() + " does not permit in " + restriction.subject();
			}
			if (restriction != null && !"Z".equalsIgnoreCase(matcher.group(5))) {
				return "the tdate's text '" + text + "' uses a local UTC offset, but "
					+ restriction.specification() + " requires " + restriction.subject() + " to use 'Z'";
			}
			// Rebuild the shape-checked fields for java.time's range checking: a leap second
			// becomes :59, since java.time rejects :60 (whether a leap second really occurred at
			// that instant is out of scope, as it is for RFC 3339's own grammar), and the
			// fraction is capped at the nine digits java.time accepts.
			String seconds = "60".equals(matcher.group(3)) ? "59" : matcher.group(3);
			String fraction = matcher.group(4) == null ? "" : matcher.group(4);
			if (fraction.length() > 10) {
				fraction = fraction.substring(0, 10);
			}
			try {
				// The offset is fully validated by the pattern; parsing only the local date and
				// time avoids ZoneOffset, which would wrongly reject RFC 3339 offsets beyond its
				// +/-18:00 cap.
				LocalDateTime.parse(matcher.group(1) + "T" + matcher.group(2) + ":" + seconds + fraction);
				return null;
			} catch (DateTimeParseException e) {
				// an out of range field such as month 13 or minute 60; fall through to the error
			}
		}
		return "the tdate's text '" + text + "' is not an RFC 3339 date-time";
	}

	/** A tdate (CBOR tag 0) or a full-date (CBOR tag 1004). */
	static MdocValueConstraint tdateOrFullDate() {
		return tdateOrFullDateConstraint(null);
	}

	/**
	 * A full-date, or a tdate as ISO/IEC 18013-5 restricts it for mDL data elements: no fractions
	 * of seconds and a UTC offset of "Z".
	 */
	static MdocValueConstraint mdlTdateOrFullDate() {
		return tdateOrFullDateConstraint(TdateRestriction.MDL);
	}

	/**
	 * A full-date, or a tdate as the PID Rulebook 3.1.2 restricts it for PID attributes: no
	 * fractions of seconds and a UTC offset of "Z".
	 */
	static MdocValueConstraint pidTdateOrFullDate() {
		return tdateOrFullDateConstraint(TdateRestriction.PID);
	}

	private static MdocValueConstraint tdateOrFullDateConstraint(TdateRestriction restriction) {
		return value -> {
			if (value instanceof Tagged tagged
					&& (tagged.getTagNumber() == Tagged.FULL_DATE_STRING
						|| tagged.getTagNumber() == Tagged.DATE_TIME_STRING)) {
				if (!(tagged.getTaggedItem() instanceof Tstr)) {
					return "the tagged item is not a text string";
				}
				String text = tagged.getTaggedItem().getAsTstr();
				return tagged.getTagNumber() == Tagged.FULL_DATE_STRING
					? checkFullDateText(text)
					: checkDateTimeText(text, restriction);
			}
			return "expected a tdate (CBOR tag " + Tagged.DATE_TIME_STRING + ") or a full-date (CBOR tag "
				+ Tagged.FULL_DATE_STRING + ") but found " + describe(value);
		};
	}

	/** A tdate, defined as #6.0(tstr) holding an RFC 3339 date-time. */
	static MdocValueConstraint tdate() {
		return tdateConstraint(null);
	}

	/**
	 * A tdate as ISO/IEC 18013-5 restricts it for mDL data elements: no fractions of seconds and
	 * a UTC offset of "Z".
	 */
	static MdocValueConstraint mdlTdate() {
		return tdateConstraint(TdateRestriction.MDL);
	}

	private static MdocValueConstraint tdateConstraint(TdateRestriction restriction) {
		return value -> {
			if (!(value instanceof Tagged tagged) || tagged.getTagNumber() != Tagged.DATE_TIME_STRING) {
				return "expected a tdate, which is CBOR tag " + Tagged.DATE_TIME_STRING
					+ " containing a text string, but found " + describe(value);
			}
			if (!(tagged.getTaggedItem() instanceof Tstr)) {
				return "the tdate's tagged item is not a text string";
			}
			return checkDateTimeText(tagged.getTaggedItem().getAsTstr(), restriction);
		};
	}

	/**
	 * A text string restricted to Latin1, which ISO/IEC 18013-5 Table 20 note b and the
	 * "Latin1 characters" elements of ISO/IEC TS 23220-2 define as ISO/IEC 8859-1.
	 */
	static MdocValueConstraint latin1Tstr() {
		return value -> {
			String problem = tstr().check(value);
			if (problem != null) {
				return problem;
			}
			return value.getAsTstr().chars().allMatch(c -> c <= 0xFF)
				? null
				: "the text contains characters outside Latin1 (ISO/IEC 8859-1)";
		};
	}

	/** A two letter country code, as ISO 3166-1 alpha-2 defines them. */
	static MdocValueConstraint alpha2CountryCode() {
		return value -> {
			String problem = tstr().check(value);
			if (problem != null) {
				return problem;
			}
			String text = value.getAsTstr();
			return ALPHA2_COUNTRY_CODE.matcher(text).matches()
				? null
				: "expected an ISO 3166-1 alpha-2 country code, which is two upper case letters, but found '"
					+ text + "'";
		};
	}

	/** A two or three letter country code, as ISO 3166-1 alpha-2 and alpha-3 define them. */
	static MdocValueConstraint alpha2Or3CountryCode() {
		return value -> {
			String problem = tstr().check(value);
			if (problem != null) {
				return problem;
			}
			String text = value.getAsTstr();
			return ALPHA2_OR_3_COUNTRY_CODE.matcher(text).matches()
				? null
				: "expected an ISO 3166-1 alpha-2 or alpha-3 country code, which is two or three upper "
					+ "case letters, but found '" + text + "'";
		};
	}

	/** A text string matching a pattern; the description completes "expected ...". */
	static MdocValueConstraint tstrMatching(Pattern pattern, String description) {
		return value -> {
			String problem = tstr().check(value);
			if (problem != null) {
				return problem;
			}
			String text = value.getAsTstr();
			return pattern.matcher(text).matches()
				? null
				: "expected " + description + " but found '" + text + "'";
		};
	}

	/**
	 * The nationalities type of the PID Rulebook 3.1.3: a non-empty array of ISO 3166-1 alpha-2
	 * country codes.
	 */
	static MdocValueConstraint alpha2CountryCodeArray() {
		return value -> {
			if (!(value instanceof CborArray array)) {
				return "expected an array of country codes but found " + describe(value);
			}
			if (array.getItems().isEmpty()) {
				return "the array of country codes is empty, but it must contain at least one";
			}
			for (DataItem item : array.getItems()) {
				String problem = alpha2CountryCode().check(item);
				if (problem != null) {
					return "an entry in the array of country codes is invalid: " + problem;
				}
			}
			return null;
		};
	}

	/**
	 * The place_of_birth type of the PID Rulebook 3.1.4: a map with at least one of "country" (an
	 * ISO 3166-1 alpha-2 country code), "region" and "locality" (text strings), and no other key.
	 */
	static MdocValueConstraint placeOfBirth() {
		return value -> {
			if (!(value instanceof CborMap map)) {
				return "expected the place_of_birth map but found " + describe(value);
			}
			if (map.getItems().isEmpty()) {
				return "the place_of_birth map is empty, but it must contain at least one of "
					+ new TreeSet<>(PLACE_OF_BIRTH_KEYS);
			}
			for (Map.Entry<DataItem, DataItem> entry : map.getItems().entrySet()) {
				if (!(entry.getKey() instanceof Tstr) || !PLACE_OF_BIRTH_KEYS.contains(entry.getKey().getAsTstr())) {
					return "the place_of_birth map has a key other than " + new TreeSet<>(PLACE_OF_BIRTH_KEYS);
				}
				String key = entry.getKey().getAsTstr();
				MdocValueConstraint constraint = "country".equals(key) ? alpha2CountryCode() : tstr();
				String problem = constraint.check(entry.getValue());
				if (problem != null) {
					return "the place_of_birth map's '" + key + "' entry is invalid: " + problem;
				}
			}
			return null;
		};
	}

	/** A text string from a fixed set of values. */
	static MdocValueConstraint oneOf(Set<String> permitted) {
		return value -> {
			String problem = tstr().check(value);
			if (problem != null) {
				return problem;
			}
			String text = value.getAsTstr();
			return permitted.contains(text)
				? null
				: "value '" + text + "' is not one of the permitted values " + new TreeSet<>(permitted);
		};
	}

	/** An unsigned integer from a fixed set of values. */
	static MdocValueConstraint uintOneOf(Set<Long> permitted) {
		return value -> {
			String problem = uint().check(value);
			if (problem != null) {
				return problem;
			}
			long number = value.getAsNumber();
			return permitted.contains(number)
				? null
				: "value " + number + " is not one of the permitted values " + new TreeSet<>(permitted);
		};
	}

	/**
	 * Either a full-date, or the birth_date structure of ISO/IEC TS 23220-2 6.3.1.1.3, a map with a
	 * "birth_date" full-date and an optional "approximate_mask" text string, used when part of the
	 * date of birth is unknown.
	 */
	static MdocValueConstraint fullDateOrBirthDateStructure() {
		return value -> {
			if (value instanceof CborMap) {
				DataItem birthDate = value.getOrNull("birth_date");
				if (birthDate == null) {
					return "the birth_date structure has no 'birth_date' entry";
				}
				String problem = fullDate().check(birthDate);
				if (problem != null) {
					return "the birth_date structure's 'birth_date' entry is invalid: " + problem;
				}
				DataItem mask = value.getOrNull("approximate_mask");
				if (mask != null) {
					if (!(mask instanceof Tstr)) {
						return "the birth_date structure's 'approximate_mask' entry is not a text string";
					}
					if (!APPROXIMATE_MASK.matcher(mask.getAsTstr()).matches()) {
						return "the birth_date structure's 'approximate_mask' entry '" + mask.getAsTstr()
							+ "' is not 8 digits of 0 and 1 marking the masked YYYYMMDD positions";
					}
				}
				return null;
			}
			return fullDate().check(value);
		};
	}
}
