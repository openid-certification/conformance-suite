package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.util.Base64;
import com.nimbusds.jose.util.X509CertUtils;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.SuiteWrprc;
import net.openid.conformance.util.TestKeysAndCerts;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x500.style.IETFUtils;

import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.util.List;

/**
 * Fills the client's 'verifier_info' in the test configuration with the suite's built-in
 * wallet-relying-party registration certificate ({@link SuiteWrprc}) when the tester supplied none
 * and the requests are signed with a certificate of the OpenID Foundation, such as the built-in
 * access certificate: a wallet binds a registration certificate to the access certificate by the
 * organizationIdentifier (ETSI TS 119 475 GEN-5.1.1-02), so the suite's certificate fits no other.
 * A configured 'verifier_info' is left alone. Runs after
 * {@link AddBuiltInAccessCertificateToClientConfiguration} and before the client configuration is
 * read into the environment.
 */
public class AddBuiltInRegistrationCertificateToClientConfiguration extends AbstractCondition {

	@Override
	@PreEnvironment(required = "config")
	public Environment evaluate(Environment env) {
		JsonElement clientEl = env.getElementFromObject("config", "client");
		if (clientEl == null || !clientEl.isJsonObject()) {
			log("No 'Client' section in the test configuration; the suite sends no registration certificate");
			return env;
		}
		JsonObject client = clientEl.getAsJsonObject();

		JsonElement verifierInfo = client.get("verifier_info");
		if (verifierInfo != null && !verifierInfo.isJsonNull()) {
			log("The 'verifier_info' field in the 'Client' section of the test configuration is set; the suite sends it",
				args("verifier_info", verifierInfo));
			return env;
		}

		String organizationIdentifier = signingCertificateOrganizationIdentifier(client.get("jwks"));
		if (!TestKeysAndCerts.ORGANIZATION_IDENTIFIER.equals(organizationIdentifier)) {
			log("The requests are not signed with a certificate of the OpenID Foundation, which the suite's registration"
					+ " certificate is bound to; the suite sends no registration certificate",
				args("signing_certificate_organization_identifier", organizationIdentifier));
			return env;
		}

		client.add("verifier_info", SuiteWrprc.verifierInfo());
		String wrprc = SuiteWrprc.jwt();
		JsonObject wrprcPayload;
		try {
			wrprcPayload = JsonParser.parseString(JWSObject.parse(wrprc).getPayload().toString()).getAsJsonObject();
		} catch (ParseException e) {
			throw error("The suite's registration certificate is not a JWS", e, args("registration_certificate", wrprc));
		}
		logSuccess("No 'verifier_info' field in the 'Client' section of the test configuration; the suite sends its"
				+ " built-in wallet-relying-party registration certificate, issued under the WRPRC provider CA served at "
				+ TestKeysAndCerts.SuiteCa.WRPRC_PROVIDER.getTrustAnchorUrl(),
			args("registration_certificate", wrprc,
				"registration_certificate_payload", wrprcPayload,
				"wrprc_provider_ca_url", TestKeysAndCerts.SuiteCa.WRPRC_PROVIDER.getTrustAnchorUrl()));
		return env;
	}

	/** The organizationIdentifier of the first key's certificate in {@code jwks}, or null. */
	private static String signingCertificateOrganizationIdentifier(JsonElement jwks) {
		if (jwks == null || !jwks.isJsonObject()) {
			return null;
		}
		try {
			List<JWK> keys = JWKSet.parse(jwks.toString()).getKeys();
			if (keys.isEmpty() || keys.get(0).getX509CertChain() == null || keys.get(0).getX509CertChain().isEmpty()) {
				return null;
			}
			Base64 der = keys.get(0).getX509CertChain().get(0);
			X509Certificate cert = X509CertUtils.parse(der.decode());
			if (cert == null) {
				return null;
			}
			RDN[] rdns = X500Name.getInstance(cert.getSubjectX500Principal().getEncoded()).getRDNs(BCStyle.ORGANIZATION_IDENTIFIER);
			return rdns.length == 0 ? null : IETFUtils.valueToString(rdns[0].getFirst().getValue());
		} catch (ParseException e) {
			return null;
		}
	}
}
