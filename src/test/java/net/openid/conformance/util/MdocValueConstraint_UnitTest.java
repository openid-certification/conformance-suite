package net.openid.conformance.util;

import org.junit.jupiter.api.Test;
import org.multipaz.cbor.CborBuilder;
import org.multipaz.cbor.CborMap;
import org.multipaz.cbor.DataItem;
import org.multipaz.cbor.DataItemExtensionsKt;
import org.multipaz.cbor.MapBuilder;
import org.multipaz.cbor.Tagged;
import org.multipaz.cbor.Tstr;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Grammar tests for the date constraints. java.time's ISO parsers deviate from RFC 3339 in both
 * directions (accepting offsets with seconds, a missing seconds field and expanded years;
 * rejecting leap seconds and fractions longer than nine digits), so these pin the RFC 3339
 * behaviour the constraints are meant to have.
 */
public class MdocValueConstraint_UnitTest {

	private static DataItem tdate(String text) {
		return new Tagged(Tagged.DATE_TIME_STRING, new Tstr(text));
	}

	private static DataItem fullDate(String text) {
		return new Tagged(Tagged.FULL_DATE_STRING, new Tstr(text));
	}

	@Test
	public void testTdate_acceptsRfc3339DateTimes() {
		List<String> valid = List.of(
			"2030-01-01T00:00:00Z",
			"2030-01-01t00:00:00z", // RFC 3339 permits lower case 't' and 'z'
			"2030-01-01T00:00:00+05:30",
			"2030-01-01T00:00:00-00:00", // unknown local offset, RFC 3339 4.3
			"1990-12-31T23:59:60Z", // leap second
			"2030-01-01T00:00:00+18:01", // beyond java.time's ZoneOffset cap but RFC 3339-valid
			"2030-01-01T00:00:00-23:59", // offset hour goes up to 23
			"2030-01-01T00:00:00.123Z",
			"2030-01-01T00:00:00.1234567890123Z"); // fraction is 1*DIGIT, unbounded
		for (String text : valid) {
			assertNull(MdocValueConstraint.tdateOrFullDate().check(tdate(text)), text);
		}
	}

	@Test
	public void testTdate_rejectsTextThatIsNotAnRfc3339DateTime() {
		List<String> invalid = List.of(
			"2030-01-01T00:00:00+01:02:03", // RFC 3339 numeric offsets are only HH:MM
			"2030-01-01T00:00:00+24:00", // offset hour is 00-23
			"2030-01-01T00:00:00+18:60", // offset minute is 00-59
			"2030-01-01T00:00Z", // seconds are mandatory
			"2030-01-01T00:00:00+0100", // offset requires the colon
			"2030-01-01T00:00:00", // offset is mandatory
			"2030-01-01T00:60:00Z", // no such minute
			"2030-01-01T24:00:00Z", // time-hour is 00-23
			"2030-13-01T00:00:00Z", // no such month
			"2030-01-01 00:00:00Z", // 'T' separator required (RFC 8949 tag 0 follows RFC 4287/3339)
			"2030-01-01", // tag 0 holds a date-time, not a date
			"not a date");
		for (String text : invalid) {
			assertNotNull(MdocValueConstraint.tdateOrFullDate().check(tdate(text)), text);
		}
	}

	@Test
	public void testFullDate_acceptsRfc3339FullDates() {
		for (String text : List.of("1985-03-15", "2026-02-28")) {
			assertNull(MdocValueConstraint.fullDate().check(fullDate(text)), text);
			assertNull(MdocValueConstraint.tdateOrFullDate().check(fullDate(text)), text);
		}
	}

	@Test
	public void testFullDate_rejectsTextThatIsNotAnRfc3339FullDate() {
		List<String> invalid = List.of(
			"2026-99-99", // no such month or day
			"2026-02-29", // not a leap year
			"+12030-01-01", // expanded years are java.time, not RFC 3339's 4DIGIT
			"1985-3-15", // months are 2DIGIT
			"1985-03-15T00:00:00Z", // a full-date is just the date
			"not a date");
		for (String text : invalid) {
			assertNotNull(MdocValueConstraint.fullDate().check(fullDate(text)), text);
		}
	}

	private static DataItem birthDateStructure(DataItem birthDate, DataItem approximateMask) {
		MapBuilder<CborBuilder> map = CborMap.Companion.builder();
		if (birthDate != null) {
			map.put("birth_date", birthDate);
		}
		if (approximateMask != null) {
			map.put("approximate_mask", approximateMask);
		}
		return map.end().build();
	}

	@Test
	public void testBirthDateStructure_acceptsValidMasks() {
		for (String mask : List.of("00000011", "00001111", "00000000")) {
			assertNull(MdocValueConstraint.fullDateOrBirthDateStructure().check(
				birthDateStructure(fullDate("1985-03-15"), new Tstr(mask))), mask);
		}
		// the mask itself is optional
		assertNull(MdocValueConstraint.fullDateOrBirthDateStructure().check(
			birthDateStructure(fullDate("1985-03-15"), null)));
		// and so is the whole structure — a plain full-date is fine
		assertNull(MdocValueConstraint.fullDateOrBirthDateStructure().check(fullDate("1985-03-15")));
	}

	/**
	 * ISO/IEC TS 23220-2: approximate_mask is "an 8 digit flag to denote the location of the mask
	 * in YYYYMMDD format. 1 denotes mask."
	 */
	@Test
	public void testBirthDateStructure_rejectsMalformedMasks() {
		for (String mask : List.of("123", "not-a-mask", "0000001", "000000111", "00000012", "1985-03-15")) {
			assertNotNull(MdocValueConstraint.fullDateOrBirthDateStructure().check(
				birthDateStructure(fullDate("1985-03-15"), new Tstr(mask))), mask);
		}
		// a mask that is not a text string at all
		assertNotNull(MdocValueConstraint.fullDateOrBirthDateStructure().check(
			birthDateStructure(fullDate("1985-03-15"), DataItemExtensionsKt.toDataItem(11))));
		// a structure with a mask but no birth_date
		assertNotNull(MdocValueConstraint.fullDateOrBirthDateStructure().check(
			birthDateStructure(null, new Tstr("00000011"))));
	}

	@Test
	public void testTdate_acceptsOnlyTagZeroDateTimes() {
		assertNull(MdocValueConstraint.tdate().check(tdate("2026-08-29T12:00:00Z")));
		// a full-date is not a tdate
		assertNotNull(MdocValueConstraint.tdate().check(fullDate("2026-08-29")));
		// nor is a bare text string
		assertNotNull(MdocValueConstraint.tdate().check(new Tstr("2026-08-29T12:00:00Z")));
		// and the tagged text must be a date-time
		assertNotNull(MdocValueConstraint.tdate().check(tdate("2026-08-29")));
	}

	/** ISO/IEC 18013-5 Table 20 note b: Latin1 is ISO/IEC 8859-1, Latin alphabet No. 1. */
	@Test
	public void testLatin1Tstr() {
		for (String text : List.of("Mustermann", "M\u00fcller-L\u00fcdenscheidt", "\u00c5se")) {
			assertNull(MdocValueConstraint.latin1Tstr().check(new Tstr(text)), text);
		}
		for (String text : List.of("\u041c\u0443\u0441\u0442\u0435\u0440\u043c\u0430\u043d\u043d", "\u5c71\u7530", "Sm\u0131th")) {
			assertNotNull(MdocValueConstraint.latin1Tstr().check(new Tstr(text)), text);
		}
	}

	/** ISO/IEC TS 23220-2 allows alpha-2 or alpha-3 for nationality and issuing_country. */
	@Test
	public void testAlpha2Or3CountryCode() {
		for (String text : List.of("US", "USA", "UT", "UTO")) {
			assertNull(MdocValueConstraint.alpha2Or3CountryCode().check(new Tstr(text)), text);
		}
		for (String text : List.of("usa", "U", "USAX", "Utopia", "U1")) {
			assertNotNull(MdocValueConstraint.alpha2Or3CountryCode().check(new Tstr(text)), text);
		}
	}

	/**
	 * ISO/IEC 18013-5: tdate in mDL data elements "shall not" use fractions of seconds or a
	 * local UTC offset. ISO/IEC TS 23220-2 has no such restriction, so plain tdate() allows both.
	 */
	@Test
	public void testMdlTdate_rejectsFractionsAndLocalOffsets() {
		assertNull(MdocValueConstraint.mdlTdate().check(tdate("2030-01-01T00:00:00Z")));
		assertNull(MdocValueConstraint.mdlTdate().check(tdate("2030-01-01t00:00:00z")));
		assertNotNull(MdocValueConstraint.mdlTdate().check(tdate("2030-01-01T00:00:00.123Z")));
		assertNotNull(MdocValueConstraint.mdlTdate().check(tdate("2030-01-01T00:00:00+05:30")));
		assertNotNull(MdocValueConstraint.mdlTdate().check(fullDate("2030-01-01")));

		assertNull(MdocValueConstraint.mdlTdateOrFullDate().check(fullDate("2030-01-01")));
		assertNull(MdocValueConstraint.mdlTdateOrFullDate().check(tdate("2030-01-01T00:00:00Z")));
		assertNotNull(MdocValueConstraint.mdlTdateOrFullDate().check(tdate("2030-01-01T00:00:00+05:30")));
	}
}
