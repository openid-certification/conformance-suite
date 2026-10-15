package net.openid.conformance.condition.client.lote;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.nimbusds.jose.JWSObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;

import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

/**
 * Checks the certificate the list of trusted entities is signed with against ETSI TS 119 602 clause
 * 6.8.0: it must be valid now, and its subject's countryName and organizationName must match the
 * list's SchemeTerritory and one of its SchemeOperatorName values. The list and its signer thus
 * vouch for each other; whether that signer is the one the tester expects is
 * {@link ValidateLoteSignerAgainstConfiguredCertificate}'s business.
 */
public class ValidateLoteSignerCertificate extends AbstractLoteCondition {

	@Override
	@PreEnvironment(required = LOTE)
	public Environment evaluate(Environment env) {
		JWSObject jws = parseJws(env);
		X509Certificate signer = getSignerCertificateChain(jws).get(0);
		JsonObject info = getListAndSchemeInformation(env);
		if (info == null) {
			throw error("The list of trusted entities has no ListAndSchemeInformation to compare the signer certificate with");
		}

		List<String> findings = new ArrayList<>();
		try {
			signer.checkValidity();
		} catch (CertificateExpiredException e) {
			findings.add("the signer certificate has expired (notAfter " + signer.getNotAfter() + ")");
		} catch (CertificateNotYetValidException e) {
			findings.add("the signer certificate is not yet valid (notBefore " + signer.getNotBefore() + ")");
		}

		X500Name subject = X500Name.getInstance(signer.getSubjectX500Principal().getEncoded());
		String country = rdnValue(subject, BCStyle.C);
		String organization = rdnValue(subject, BCStyle.O);

		JsonElement territoryEl = info.get("SchemeTerritory");
		String territory = territoryEl != null && territoryEl.isJsonPrimitive() ? OIDFJSON.getString(territoryEl) : null;
		if (country == null) {
			findings.add("the signer certificate's subject has no countryName");
		} else if (territory != null && !country.equalsIgnoreCase(territory)) {
			findings.add("the signer certificate's countryName '" + country + "' differs from the SchemeTerritory '" + territory + "'");
		}

		List<String> operatorNames = new ArrayList<>();
		JsonElement operators = info.get("SchemeOperatorName");
		if (operators != null && operators.isJsonArray()) {
			for (JsonElement op : operators.getAsJsonArray()) {
				if (op.isJsonObject() && op.getAsJsonObject().get("value") != null) {
					operatorNames.add(OIDFJSON.getString(op.getAsJsonObject().get("value")));
				}
			}
		}
		if (organization == null) {
			findings.add("the signer certificate's subject has no organizationName");
		} else if (!operatorNames.isEmpty() && !operatorNames.contains(organization)) {
			findings.add("the signer certificate's organizationName '" + organization
				+ "' is not one of the SchemeOperatorName values " + operatorNames);
		}

		if (!findings.isEmpty()) {
			JsonArray findingsJson = new JsonArray();
			findings.forEach(findingsJson::add);
			throw error("The certificate signing the list of trusted entities does not match the list (ETSI TS 119 602 clause 6.8.0)",
				args("findings", findingsJson,
					"signer_certificate_subject", signer.getSubjectX500Principal().getName(),
					"scheme_territory", territory,
					"scheme_operator_names", operatorNames));
		}

		logSuccess("The certificate signing the list of trusted entities is valid and names the scheme territory and operator",
			args("signer_certificate_subject", signer.getSubjectX500Principal().getName(),
				"scheme_territory", territory,
				"scheme_operator_names", operatorNames));
		return env;
	}

	private static String rdnValue(X500Name name, org.bouncycastle.asn1.ASN1ObjectIdentifier type) {
		RDN[] rdns = name.getRDNs(type);
		return rdns.length == 0 ? null : rdns[0].getFirst().getValue().toString();
	}
}
