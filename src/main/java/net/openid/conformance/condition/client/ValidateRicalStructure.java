package net.openid.conformance.condition.client;

import com.google.gson.JsonArray;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.multipaz.cbor.Bstr;
import org.multipaz.cbor.Cbor;
import org.multipaz.cbor.CborArray;
import org.multipaz.cbor.CborMap;
import org.multipaz.cbor.DataItem;
import org.multipaz.cbor.Tstr;
import org.multipaz.cose.CoseSign1;
import org.multipaz.crypto.X509Cert;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Validates the RICAL payload against the format in ISO/IEC 18013-5 second edition draft
 * Annex F.3.2.1/F.3.2.2: required fields, plausible dates, per-CertificateInfo consistency with
 * the embedded certificate, trust-anchor reachability, and no undefined fields (unknown keys are
 * reserved for future use, so an unknown field is most likely a misspelling — this condition is
 * expected to be called as a WARNING). Annex F is an informative annex of a CD-ballot draft, so
 * findings here are quality signals about the RICAL provider, not conformance verdicts on the
 * entity under test.
 */
public class ValidateRicalStructure extends AbstractRicalCondition {

	// F.3.2.1: the type identifier Annex F defines for general-purpose RICALs
	public static final String READER_AUTHENTICATION_RICAL_TYPE = "org.iso.18013.5.1.reader_authentication";

	private static final Set<String> KNOWN_RICAL_KEYS = Set.of(
		"version", "provider", "date", "nextUpdate", "notAfter", "certificateInfos", "id",
		"latestRicalUrl", "extensions", "type");

	// the draft's CDDL spells the constraints key "trustContraints" while the field list says
	// "trustConstraints"; accept both spellings without a finding until ISO resolves the typo
	private static final Set<String> KNOWN_CERTIFICATE_INFO_KEYS = Set.of(
		"certificate", "serialNumber", "isTrustAnchor", "ski", "aki", "type", "trustConstraints",
		"trustContraints", "name", "issuingCountry", "stateOrProvinceName", "issuer", "subject",
		"notBefore", "notAfter", "extensions");

	// C.1.6.4 requires asserted times to be accurate within three minutes; allow that much skew
	private static final long CLOCK_SKEW_MILLIS = 3 * 60 * 1000;

	// sanity bounds: no RICAL predates the ISO 18013-5 era, and dates absurdly far in the
	// future indicate a nonsensical value rather than a real schedule
	private static final long EARLIEST_PLAUSIBLE_MILLIS = java.time.Instant.parse("2019-01-01T00:00:00Z").toEpochMilli();
	private static final long FIFTY_YEARS_MILLIS = 50L * 365 * 24 * 3600 * 1000;

	// cap the reported findings so a large broken RICAL doesn't produce an enormous log entry
	private static final int MAX_REPORTED_FINDINGS = 50;

	@Override
	@PreEnvironment(required = "rical")
	public Environment evaluate(Environment env) {

		byte[] ricalBytes = getRicalBytes(env);

		CoseSign1 coseSign1 = getRicalCoseSign1(ricalBytes);
		DataItem rical;
		try {
			rical = Cbor.INSTANCE.decode(coseSign1.getPayload());
		} catch (Exception e) {
			throw error("Failed to parse the RICAL COSE_Sign1 payload as CBOR", e);
		}

		if (!(rical instanceof CborMap)) {
			throw error("The RICAL payload is not a CBOR map");
		}

		List<String> findings = new ArrayList<>();

		checkTopLevel(rical, findings);

		DataItem certificateInfos = rical.getOrNull("certificateInfos");
		if (certificateInfos instanceof CborArray) {
			List<DataItem> entries = certificateInfos.getAsArray();
			for (int i = 0; i < entries.size(); i++) {
				checkCertificateInfo(entries.get(i), i, findings);
			}
			checkTrustAnchorReachability(entries, findings);
		}

		if (!findings.isEmpty()) {
			List<String> reported = findings.size() > MAX_REPORTED_FINDINGS
				? findings.subList(0, MAX_REPORTED_FINDINGS) : findings;
			JsonArray findingsJson = new JsonArray();
			reported.forEach(findingsJson::add);
			String suffix = findings.size() > MAX_REPORTED_FINDINGS
				? "; ... " + (findings.size() - MAX_REPORTED_FINDINGS) + " further findings not shown" : "";
			throw error("RICAL structure problems found: " + String.join("; ", reported) + suffix,
				args("findings", findingsJson,
					"total_findings", findings.size()));
		}

		logSuccess("RICAL structure matches the ISO/IEC 18013-5 Annex F.3.2 format");

		return env;
	}

	private void checkTopLevel(DataItem rical, List<String> findings) {
		long now = System.currentTimeMillis();

		DataItem version = rical.getOrNull("version");
		if (!(version instanceof Tstr)) {
			findings.add("required field 'version' is missing or not a text string");
		} else if (!"1.0".equals(version.getAsTstr())) {
			findings.add("'version' is '" + version.getAsTstr() + "' but '1.0' is the only value Annex F defines");
		}
		if (!(rical.getOrNull("provider") instanceof Tstr)) {
			findings.add("required field 'provider' is missing or not a text string");
		}
		if (!(rical.getOrNull("type") instanceof Tstr)) {
			findings.add("required field 'type' is missing or not a text string");
		}

		Long date = CborStructureChecks.tdateMillis(rical.getOrNull("date"), "date", findings, true);
		if (date != null && date > now + CLOCK_SKEW_MILLIS) {
			findings.add("'date' (RICAL issuance) is in the future");
		}
		if (date != null && date < EARLIEST_PLAUSIBLE_MILLIS) {
			findings.add("'date' (RICAL issuance) is implausibly old");
		}
		Long nextUpdate = CborStructureChecks.tdateMillis(rical.getOrNull("nextUpdate"), "nextUpdate", findings, false);
		if (nextUpdate != null && nextUpdate < now - CLOCK_SKEW_MILLIS) {
			findings.add("'nextUpdate' is in the past - the RICAL is overdue for an update");
		}
		if (nextUpdate != null && nextUpdate > now + FIFTY_YEARS_MILLIS) {
			findings.add("'nextUpdate' is implausibly far in the future");
		}
		Long notAfter = CborStructureChecks.tdateMillis(rical.getOrNull("notAfter"), "notAfter", findings, false);
		if (notAfter != null && notAfter < now - CLOCK_SKEW_MILLIS) {
			findings.add("'notAfter' is in the past - the RICAL is no longer valid");
		}
		if (notAfter != null && notAfter > now + FIFTY_YEARS_MILLIS) {
			findings.add("'notAfter' is implausibly far in the future");
		}

		DataItem latestRicalUrl = rical.getOrNull("latestRicalUrl");
		if (latestRicalUrl != null) {
			if (!(latestRicalUrl instanceof Tstr)) {
				findings.add("'latestRicalUrl' is not a text string");
			} else if (!latestRicalUrl.getAsTstr().toLowerCase(Locale.ROOT).startsWith("https://")) {
				findings.add("'latestRicalUrl' is not an HTTPS URL as Annex F requires");
			}
		}

		DataItem certificateInfos = rical.getOrNull("certificateInfos");
		if (!(certificateInfos instanceof CborArray)) {
			findings.add("required field 'certificateInfos' is missing or not an array");
		} else if (certificateInfos.getAsArray().isEmpty()) {
			// F.3.2.5: the certificateInfos array shall not be empty
			findings.add("'certificateInfos' is empty - the array shall not be empty");
		}

		for (DataItem key : rical.getAsMap().keySet()) {
			if (!(key instanceof Tstr)) {
				findings.add("RICAL map contains a non-text-string key");
				continue;
			}
			String keyName = key.getAsTstr();
			if (!KNOWN_RICAL_KEYS.contains(keyName)) {
				findings.add("unknown field '" + keyName + "' in the RICAL (unknown keys are reserved for future use, so this is most likely a mistake)");
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
			BigInteger listed = CborStructureChecks.biguintValue(serialItem);
			if (listed == null) {
				String hint = "";
				if (serialItem instanceof org.multipaz.cbor.Tagged
					&& ((org.multipaz.cbor.Tagged) serialItem).getTagNumber() == org.multipaz.cbor.Tagged.NEGATIVE_BIGNUM) {
					hint = " - a serial number whose first byte is 0x80 or above has probably been encoded with the negative-bignum tag by mistake";
				}
				findings.add(prefix + "'serialNumber' must be a biguint (a tag 2 unsigned-bignum byte string, or a plain unsigned integer) but is "
					+ CborStructureChecks.describeItem(serialItem) + hint);
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

		DataItem trustAnchorItem = certInfo.getOrNull("isTrustAnchor");
		if (trustAnchorItem == null) {
			// F.3.2.2 lists isTrustAnchor as required; real-world RICALs (e.g. the Geneva 2026
			// interop list) omit it, and readers then have to guess the entry's role
			findings.add(prefix + "required field 'isTrustAnchor' is missing");
		} else {
			try {
				trustAnchorItem.getAsBoolean();
			} catch (Exception e) {
				findings.add(prefix + "'isTrustAnchor' is not a boolean");
			}
		}

		if (cert != null && !cert.getSubject().getName().equals(cert.getIssuer().getName())) {
			// F.3.2.2: aki shall be present for Sub CAs (a non-self-issued certificate)
			DataItem akiItem = certInfo.getOrNull("aki");
			if (akiItem == null) {
				findings.add(prefix + "'aki' is missing but the certificate is not self-issued - Annex F requires 'aki' for Sub CA entries");
			} else {
				byte[] certAki = cert.getAuthorityKeyIdentifier();
				if (certAki != null) {
					try {
						if (!Arrays.equals(akiItem.getAsBstr(), certAki)) {
							findings.add(prefix + "'aki' does not match the Authority Key Identifier of the embedded certificate");
						}
					} catch (Exception e) {
						findings.add(prefix + "'aki' is not a byte string");
					}
				}
			}
		}

		if (cert != null) {
			Long notBefore = CborStructureChecks.tdateMillis(certInfo.getOrNull("notBefore"), prefix + "notBefore", findings, false);
			if (notBefore != null && notBefore != cert.getValidityNotBefore().toEpochMilliseconds()) {
				findings.add(prefix + "'notBefore' does not match the notBefore of the embedded certificate");
			}
			Long notAfter = CborStructureChecks.tdateMillis(certInfo.getOrNull("notAfter"), prefix + "notAfter", findings, false);
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
				findings.add(prefix + "unknown field '" + keyName + "' (unknown keys are reserved for future use, so this is most likely a mistake)");
			}
		}
	}

	/**
	 * F.3.2.5 structure integrity: each included CA shall have a certificate validation path to
	 * an entry with isTrustAnchor set to true. Approximated without full PKIX path building:
	 * at least one entry must be a trust anchor, and every non-trust-anchor entry's issuer must
	 * appear as the subject of some other entry in the list.
	 */
	private void checkTrustAnchorReachability(List<DataItem> entries, List<String> findings) {
		List<X509Cert> certs = new ArrayList<>();
		List<Boolean> anchorFlags = new ArrayList<>();
		for (DataItem entry : entries) {
			if (!(entry instanceof CborMap)) {
				return; // malformed entries already reported per-entry
			}
			DataItem certItem = entry.getOrNull("certificate");
			if (!(certItem instanceof Bstr)) {
				return;
			}
			try {
				certs.add(X509Cert.Companion.fromDataItem(certItem));
			} catch (Exception e) {
				return;
			}
			DataItem trustAnchorItem = entry.getOrNull("isTrustAnchor");
			// entries omitting the required isTrustAnchor already get a finding; treat them as
			// anchors here (matching multipaz's parse default) to avoid duplicate findings
			boolean isAnchor;
			try {
				isAnchor = trustAnchorItem == null || trustAnchorItem.getAsBoolean();
			} catch (Exception e) {
				isAnchor = true;
			}
			anchorFlags.add(isAnchor);
		}

		if (!anchorFlags.contains(Boolean.TRUE)) {
			findings.add("no entry has isTrustAnchor set to true - each included CA shall have a certificate validation path to a trust anchor entry");
			return;
		}

		for (int i = 0; i < certs.size(); i++) {
			if (anchorFlags.get(i)) {
				continue;
			}
			String issuer = certs.get(i).getIssuer().getName();
			boolean issuerListed = false;
			for (X509Cert other : certs) {
				if (other.getSubject().getName().equals(issuer)) {
					issuerListed = true;
					break;
				}
			}
			if (!issuerListed) {
				findings.add("certificateInfos[" + i + "] (" + certs.get(i).getSubject().getName()
					+ "): has isTrustAnchor false but its issuer is not listed in the RICAL, so it has no path to a trust anchor entry");
			}
		}
	}
}
