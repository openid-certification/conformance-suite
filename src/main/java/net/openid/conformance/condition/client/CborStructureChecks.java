package net.openid.conformance.condition.client;

import org.multipaz.cbor.DataItem;
import org.multipaz.cbor.Tagged;

import java.math.BigInteger;
import java.util.List;

/**
 * Shared CBOR field-shape helpers for the VICAL and RICAL structure validation conditions
 * ({@link ValidateVicalStructure}, {@link ValidateRicalStructure}).
 */
final class CborStructureChecks {

	private CborStructureChecks() {
	}

	/** Returns the epoch millis of a tdate item, or null (adding a finding if required/mistyped). */
	static Long tdateMillis(DataItem item, String fieldName, List<String> findings, boolean required) {
		if (item == null) {
			if (required) {
				findings.add("required field '" + fieldName + "' is missing");
			}
			return null;
		}
		try {
			return item.getAsDateTimeString().toEpochMilliseconds();
		} catch (Exception e) {
			findings.add("'" + fieldName + "' is not a valid tdate (tag 0 RFC 3339 date-time string)");
			return null;
		}
	}

	/** Human-readable description of a CBOR item's shape, e.g. "tag 3 (negative bignum) around a byte string". */
	static String describeItem(DataItem item) {
		if (item instanceof Tagged) {
			long tag = ((Tagged) item).getTagNumber();
			String name;
			if (tag == Tagged.DATE_TIME_STRING) {
				name = "date-time string";
			} else if (tag == Tagged.UNSIGNED_BIGNUM) {
				name = "unsigned bignum";
			} else if (tag == Tagged.NEGATIVE_BIGNUM) {
				name = "negative bignum";
			} else if (tag == Tagged.ENCODED_CBOR) {
				name = "embedded CBOR";
			} else {
				name = "unrecognized tag";
			}
			return "tag " + tag + " (" + name + ") around " + describeItem(((Tagged) item).getTaggedItem());
		}
		String type = item.getClass().getSimpleName();
		switch (type) {
			case "Uint": return "an unsigned integer";
			case "Nint": return "a negative integer";
			case "Bstr": return "a byte string";
			case "Tstr": return "a text string";
			case "CborArray": return "an array";
			case "CborMap": return "a map";
			default: return "a " + type;
		}
	}

	/** Decodes a biguint (tag 2 byte string) or plain unsigned integer, or returns null. */
	static BigInteger biguintValue(DataItem item) {
		if (item instanceof Tagged && ((Tagged) item).getTagNumber() == Tagged.UNSIGNED_BIGNUM) {
			try {
				return new BigInteger(1, ((Tagged) item).getTaggedItem().getAsBstr());
			} catch (Exception e) {
				return null;
			}
		}
		// technically the CDDL requires biguint, but a plain uint encodes the same semantic value
		try {
			return BigInteger.valueOf(item.getAsNumber());
		} catch (Exception e) {
			return null;
		}
	}
}
