package net.openid.conformance.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jose.JWSObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.JWKUtil;
import net.openid.conformance.util.SuiteWrprc;
import net.openid.conformance.util.TestKeysAndCerts;
import net.openid.conformance.util.X509CertificateUtil;
import org.bouncycastle.asn1.x500.style.BCStyle;

import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.util.HashSet;
import java.util.Set;

/**
 * Fills the client's 'verifier_info' in the test configuration with the suite's built-in
 * wallet-relying-party registration certificate ({@link SuiteWrprc}) when the tester supplied none
 * and the requests are signed with a certificate of the OpenID Foundation, such as the built-in
 * access certificate: a wallet binds a registration certificate to the access certificate by the
 * organizationIdentifier (ETSI TS 119 475 GEN-5.1.1-02), so the suite's certificate fits no other.
 * A configured 'verifier_info' is left alone, and none is sent when the tester's own DCQL query asks
 * for a credential or claim the certificate does not register, which a wallet is entitled to refuse
 * (TS 119 475 registers what a relying party may request). Runs after
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

		if (env.getString(LoadBuiltInDcqlQuery.RESOURCE_ENV_KEY) == null) {
			String unregistered = firstUnregisteredRequest(client.get("dcql"));
			if (unregistered != null) {
				log("The 'dcql' field in the 'Client' section of the test configuration requests something the suite's"
						+ " registration certificate does not register, so a wallet could refuse the request for that reason;"
						+ " the suite sends no registration certificate",
					args("unregistered", unregistered, "registered_credentials", SuiteWrprc.credentials()));
				return env;
			}
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

	/**
	 * The first credential query or claim of {@code dcql} that the suite's registration certificate
	 * does not register, described for the log, or null when it registers all of them. A credential
	 * query registers when a registered credential has the same format and meta; its claims when
	 * each path is among that credential's, and a query naming no claims asks for all of them.
	 */
	private static String firstUnregisteredRequest(JsonElement dcql) {
		if (dcql == null || !dcql.isJsonObject() || !dcql.getAsJsonObject().has("credentials")
			|| !dcql.getAsJsonObject().get("credentials").isJsonArray()) {
			return "a DCQL query without a 'credentials' array";
		}
		JsonArray registered = SuiteWrprc.credentials();
		for (JsonElement queryEl : dcql.getAsJsonObject().getAsJsonArray("credentials")) {
			if (!queryEl.isJsonObject()) {
				return queryEl.toString();
			}
			JsonObject query = queryEl.getAsJsonObject();
			JsonObject match = null;
			for (JsonElement credentialEl : registered) {
				JsonObject credential = credentialEl.getAsJsonObject();
				if (credential.get("format").equals(query.get("format")) && credential.get("meta").equals(query.get("meta"))) {
					match = credential;
					break;
				}
			}
			if (match == null) {
				return "credential query " + query.get("id") + " (format " + query.get("format") + ", meta " + query.get("meta") + ")";
			}
			JsonElement claims = query.get("claims");
			if (claims == null || !claims.isJsonArray()) {
				return "credential query " + query.get("id") + ", which names no claims and so asks for all of them";
			}
			Set<JsonElement> registeredPaths = new HashSet<>();
			for (JsonElement claim : match.getAsJsonArray("claim")) {
				registeredPaths.add(claim.getAsJsonObject().get("path"));
			}
			for (JsonElement claim : claims.getAsJsonArray()) {
				JsonElement path = claim.isJsonObject() ? claim.getAsJsonObject().get("path") : null;
				if (path == null || !registeredPaths.contains(path)) {
					return "claim " + (path == null ? claim : path) + " of credential query " + query.get("id");
				}
			}
		}
		return null;
	}

	/** The organizationIdentifier of the certificate of the signing key in {@code jwks}, or null. */
	private static String signingCertificateOrganizationIdentifier(JsonElement jwks) {
		if (jwks == null || !jwks.isJsonObject()) {
			return null;
		}
		X509Certificate cert;
		try {
			cert = JWKUtil.getSigningKeyCertificateChain(jwks.getAsJsonObject()).get(0);
		} catch (JWKUtil.SigningKeyChainException e) {
			// no single signing key with a certificate: the request signer reports that
			return null;
		}
		return X509CertificateUtil.subjectRdnValue(cert, BCStyle.ORGANIZATION_IDENTIFIER);
	}
}
