package net.openid.conformance.condition.client;

import com.google.gson.JsonArray;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.multipaz.cbor.Cbor;
import org.multipaz.cbor.CborArray;
import org.multipaz.cbor.CborMap;
import org.multipaz.cbor.DataItem;
import org.multipaz.cbor.Tagged;
import org.multipaz.cbor.Tstr;
import org.multipaz.cose.CoseSign1;
import org.multipaz.crypto.X509Cert;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Validates the VICAL payload against the CDDL profile in ISO/IEC 18013-5 Annex C.1.7.1: required
 * fields, plausible dates, per-CertificateInfo consistency with the embedded certificate, and no
 * undefined fields (the CDDL reserves unknown keys for future extensions, so an unknown field is
 * most likely a misspelling — this condition is expected to be called as a WARNING).
 */
public class ValidateVicalStructure extends AbstractVicalCondition {

	// includes the second-edition draft's optional top-level notAfter and vicalURL
	private static final Set<String> KNOWN_VICAL_KEYS = Set.of(
		"version", "vicalProvider", "date", "vicalIssueID", "nextUpdate", "notAfter", "vicalURL",
		"certificateInfos", "extensions");

	private static final Set<String> KNOWN_CERTIFICATE_INFO_KEYS = Set.of(
		"certificate", "serialNumber", "ski", "docType", "certificateProfile", "issuingAuthority",
		"issuingCountry", "stateOrProvinceName", "issuer", "subject", "notBefore", "notAfter", "extensions");

	// C.1.6.4 requires asserted times to be accurate within three minutes; allow that much skew
	private static final long CLOCK_SKEW_MILLIS = 3 * 60 * 1000;

	// sanity bounds: no VICAL predates the ISO 18013-5 era, and dates absurdly far in the
	// future indicate a nonsensical value rather than a real schedule
	private static final long EARLIEST_PLAUSIBLE_MILLIS = java.time.Instant.parse("2019-01-01T00:00:00Z").toEpochMilli();
	private static final long FIFTY_YEARS_MILLIS = 50L * 365 * 24 * 3600 * 1000;

	// cap the reported findings so a large broken VICAL doesn't produce an enormous log entry
	private static final int MAX_REPORTED_FINDINGS = 50;

	@Override
	@PreEnvironment(required = "vical")
	public Environment evaluate(Environment env) {

		byte[] vicalBytes = getVicalBytes(env);

		CoseSign1 coseSign1 = getVicalCoseSign1(vicalBytes);
		DataItem vical;
		try {
			vical = Cbor.INSTANCE.decode(coseSign1.getPayload());
		} catch (Exception e) {
			throw error("Failed to parse the VICAL COSE_Sign1 payload as CBOR", e);
		}

		if (!(vical instanceof CborMap)) {
			throw error("The VICAL payload is not a CBOR map");
		}

		List<String> findings = new ArrayList<>();

		checkTopLevel(vical, findings);

		DataItem certificateInfos = vical.getOrNull("certificateInfos");
		if (certificateInfos instanceof CborArray) {
			List<DataItem> entries = certificateInfos.getAsArray();
			for (int i = 0; i < entries.size(); i++) {
				checkCertificateInfo(entries.get(i), i, findings);
			}
		}

		if (!findings.isEmpty()) {
			List<String> reported = findings.size() > MAX_REPORTED_FINDINGS
				? findings.subList(0, MAX_REPORTED_FINDINGS) : findings;
			JsonArray findingsJson = new JsonArray();
			reported.forEach(findingsJson::add);
			String suffix = findings.size() > MAX_REPORTED_FINDINGS
				? "; ... " + (findings.size() - MAX_REPORTED_FINDINGS) + " further findings not shown" : "";
			throw error("VICAL structure problems found: " + String.join("; ", reported) + suffix,
				args("findings", findingsJson,
					"total_findings", findings.size()));
		}

		logSuccess("VICAL structure matches the ISO/IEC 18013-5 Annex C.1.7.1 profile");

		return env;
	}

	private void checkTopLevel(DataItem vical, List<String> findings) {
		long now = System.currentTimeMillis();

		if (!(vical.getOrNull("version") instanceof Tstr)) {
			findings.add("required field 'version' is missing or not a text string");
		}
		if (!(vical.getOrNull("vicalProvider") instanceof Tstr)) {
			findings.add("required field 'vicalProvider' is missing or not a text string");
		}

		Long date = tdateMillis(vical.getOrNull("date"), "date", findings, true);
		if (date != null && date > now + CLOCK_SKEW_MILLIS) {
			findings.add("'date' (VICAL issuance) is in the future");
		}
		if (date != null && date < EARLIEST_PLAUSIBLE_MILLIS) {
			findings.add("'date' (VICAL issuance) is implausibly old");
		}
		Long nextUpdate = tdateMillis(vical.getOrNull("nextUpdate"), "nextUpdate", findings, false);
		if (nextUpdate != null && nextUpdate < now - CLOCK_SKEW_MILLIS) {
			findings.add("'nextUpdate' is in the past - the VICAL is overdue for an update");
		}
		if (nextUpdate != null && nextUpdate > now + FIFTY_YEARS_MILLIS) {
			findings.add("'nextUpdate' is implausibly far in the future");
		}
		Long notAfter = tdateMillis(vical.getOrNull("notAfter"), "notAfter", findings, false);
		if (notAfter != null && notAfter < now - CLOCK_SKEW_MILLIS) {
			findings.add("'notAfter' is in the past - the VICAL is no longer valid");
		}
		if (notAfter != null && notAfter > now + FIFTY_YEARS_MILLIS) {
			findings.add("'notAfter' is implausibly far in the future");
		}

		DataItem certificateInfos = vical.getOrNull("certificateInfos");
		if (!(certificateInfos instanceof CborArray)) {
			findings.add("required field 'certificateInfos' is missing or not an array");
		}

		for (DataItem key : vical.getAsMap().keySet()) {
			if (!(key instanceof Tstr)) {
				findings.add("VICAL map contains a non-text-string key");
				continue;
			}
			String keyName = key.getAsTstr();
			if (!KNOWN_VICAL_KEYS.contains(keyName)) {
				findings.add("unknown field '" + keyName + "' in the VICAL (the CDDL reserves unknown keys for future use, so this is most likely a mistake)");
			}
		}
	}

	private void checkCertificateInfo(DataItem certInfo, int index, List<String> findings) {
		String prefix = "certificateInfos[" + index + "]: ";

		if (!(certInfo instanceof CborMap)) {
			findings.add(prefix + "entry is not a CBOR map");
			return;
		}

		X509Cert cert = null;
		DataItem certItem = certInfo.getOrNull("certificate");
		if (certItem == null) {
			findings.add(prefix + "required field 'certificate' is missing");
		} else {
			try {
				cert = X509Cert.Companion.fromDataItem(certItem);
				// name the entry so a finding can be matched to a certificate without
				// counting array entries in the raw CBOR
				prefix = "certificateInfos[" + index + "] (" + cert.getSubject().getName() + "): ";
			} catch (Exception e) {
				findings.add(prefix + "'certificate' could not be parsed as a DER-encoded X.509 certificate");
			}
		}

		DataItem serialItem = certInfo.getOrNull("serialNumber");
		if (serialItem == null) {
			findings.add(prefix + "required field 'serialNumber' is missing");
		} else if (cert != null) {
			BigInteger listed = biguintValue(serialItem);
			if (listed == null) {
				String hint = "";
				if (serialItem instanceof Tagged && ((Tagged) serialItem).getTagNumber() == Tagged.NEGATIVE_BIGNUM) {
					hint = " - a serial number whose first byte is 0x80 or above has probably been encoded with the negative-bignum tag by mistake";
				}
				findings.add(prefix + "'serialNumber' must be a biguint (a tag 2 unsigned-bignum byte string, or a plain unsigned integer) but is " + describeItem(serialItem) + hint);
			} else {
				BigInteger actual = new BigInteger(1, cert.getSerialNumber().getValue());
				if (!listed.equals(actual)) {
					findings.add(prefix + "'serialNumber' does not match the serial number of the embedded certificate");
				}
			}
		}

		DataItem skiItem = certInfo.getOrNull("ski");
		if (skiItem == null) {
			findings.add(prefix + "required field 'ski' is missing");
		} else if (cert != null) {
			byte[] certSki = cert.getSubjectKeyIdentifier();
			if (certSki == null) {
				findings.add(prefix + "the embedded certificate has no Subject Key Identifier extension to match 'ski' against");
			} else {
				try {
					if (!Arrays.equals(skiItem.getAsBstr(), certSki)) {
						findings.add(prefix + "'ski' does not match the Subject Key Identifier of the embedded certificate");
					}
				} catch (Exception e) {
					findings.add(prefix + "'ski' is not a byte string");
				}
			}
		}

		DataItem docTypeItem = certInfo.getOrNull("docType");
		if (!(docTypeItem instanceof CborArray) || docTypeItem.getAsArray().isEmpty()) {
			findings.add(prefix + "required field 'docType' is missing or empty - at least one document type is required");
		}

		if (cert != null) {
			Long notBefore = tdateMillis(certInfo.getOrNull("notBefore"), prefix + "notBefore", findings, false);
			if (notBefore != null && notBefore != cert.getValidityNotBefore().toEpochMilliseconds()) {
				findings.add(prefix + "'notBefore' does not match the notBefore of the embedded certificate");
			}
			Long notAfter = tdateMillis(certInfo.getOrNull("notAfter"), prefix + "notAfter", findings, false);
			if (notAfter != null && notAfter != cert.getValidityNotAfter().toEpochMilliseconds()) {
				findings.add(prefix + "'notAfter' does not match the notAfter of the embedded certificate");
			}
		}

		for (Map.Entry<DataItem, DataItem> entry : certInfo.getAsMap().entrySet()) {
			if (!(entry.getKey() instanceof Tstr)) {
				findings.add(prefix + "map contains a non-text-string key");
				continue;
			}
			String keyName = entry.getKey().getAsTstr();
			if (!KNOWN_CERTIFICATE_INFO_KEYS.contains(keyName)) {
				findings.add(prefix + "unknown field '" + keyName + "' (the CDDL reserves unknown keys for future use, so this is most likely a mistake)");
			}
		}
	}

	/** Returns the epoch millis of a tdate item, or null (adding a finding if required/mistyped). */
	private Long tdateMillis(DataItem item, String fieldName, List<String> findings, boolean required) {
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
	private String describeItem(DataItem item) {
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
	private BigInteger biguintValue(DataItem item) {
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
