package net.openid.conformance.condition.client;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.MdocUtil;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x500.style.IETFUtils;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.multipaz.cbor.DataItem;

import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Cross-checks the mdoc document signer certificate's subject against the credential's own
 * data elements, per ISO/IEC 18013-5 Table B.3: the certificate's countryName must contain
 * "exactly the same value as in the issuing country data element", and stateOrProvinceName,
 * when present, must "exactly match the value of the data element issuing_jurisdiction, if
 * that element is present". Skipped (with a log entry) when the credential does not disclose
 * an issuing_country element — e.g. non-ID doctypes, or presentations where it was not
 * requested.
 */
public class ValidateMdocDsCertificateMatchesIssuingCountry extends AbstractValidateMdocDsCertificate {

	@Override
	@PreEnvironment(strings = { "mdoc_credential_cbor" })
	public Environment evaluate(Environment env) {
		DataItem issuerSigned = decodeIssuerSigned(env);
		X509Certificate dsCert = extractDsCertificate(issuerSigned);

		String issuingCountry = null;
		String issuingJurisdiction = null;
		try {
			Map<String, List<DataItem>> itemsByNamespace = MdocUtil.getIssuerSignedItems(issuerSigned);
			for (List<DataItem> items : itemsByNamespace.values()) {
				for (DataItem item : items) {
					DataItem inner = item.getAsTaggedEncodedCbor();
					String elementIdentifier = inner.getOrNull("elementIdentifier") != null
						? inner.get("elementIdentifier").getAsTstr() : null;
					if ("issuing_country".equals(elementIdentifier)) {
						issuingCountry = inner.get("elementValue").getAsTstr();
					} else if ("issuing_jurisdiction".equals(elementIdentifier)) {
						issuingJurisdiction = inner.get("elementValue").getAsTstr();
					}
				}
			}
		} catch (Exception e) {
			throw error("Failed to parse the mdoc credential's data elements", e);
		}

		if (issuingCountry == null) {
			logSuccess("The credential does not disclose an issuing_country data element; "
				+ "certificate subject cross-check skipped");
			return env;
		}

		X500Name subject = X500Name.getInstance(dsCert.getSubjectX500Principal().getEncoded());
		String certCountry = firstRdnValue(subject, BCStyle.C);
		String certState = firstRdnValue(subject, BCStyle.ST);

		List<String> violations = new ArrayList<>();
		if (certCountry == null) {
			violations.add("the credential's issuing_country is '" + issuingCountry
				+ "' but the document signer certificate subject has no countryName");
		} else if (!certCountry.equals(issuingCountry)) {
			violations.add("the document signer certificate countryName '" + certCountry
				+ "' does not exactly match the credential's issuing_country '" + issuingCountry + "'");
		}
		if (issuingJurisdiction != null && certState != null && !certState.equals(issuingJurisdiction)) {
			violations.add("the document signer certificate stateOrProvinceName '" + certState
				+ "' does not exactly match the credential's issuing_jurisdiction '" + issuingJurisdiction + "'");
		}

		if (!violations.isEmpty()) {
			throw error("The document signer certificate subject does not match the credential's data elements as required by ISO 18013-5 Table B.3: "
					+ String.join("; ", violations),
				args("certificate_subject", dsCert.getSubjectX500Principal().getName(),
					"issuing_country", issuingCountry,
					"issuing_jurisdiction", issuingJurisdiction,
					"violations", violations));
		}

		logSuccess("Document signer certificate subject matches the credential's issuing country data",
			args("issuing_country", issuingCountry,
				"issuing_jurisdiction", issuingJurisdiction));
		return env;
	}

	private static String firstRdnValue(X500Name name, ASN1ObjectIdentifier attribute) {
		RDN[] rdns = name.getRDNs(attribute);
		if (rdns.length == 0) {
			return null;
		}
		return IETFUtils.valueToString(rdns[0].getFirst().getValue());
	}
}
