package net.openid.conformance.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.util.X509CertUtils;
import net.openid.conformance.oauth.statuslists.TokenStatusList;
import net.openid.conformance.testmodule.OIDFJSON;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x500.style.IETFUtils;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.time.Instant;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The suite's registration certificate follows ETSI TS 119 475, chains to the WRPRC provider CA, is
 * bound to the access certificate and registers every claim the built-in DCQL queries request.
 */
public class SuiteWrprc_UnitTest {

	private static final Instant ISSUED = Instant.parse("2026-10-15T10:00:00Z");

	@Test
	public void isAnRcWrpJwtSignedByTheLeafInItsX5cWhichChainsToTheWrprcProviderCa() throws Exception {
		JWSObject jws = JWSObject.parse(SuiteWrprc.jwt(ISSUED));
		assertThat(jws.getHeader().getType().toString()).isEqualTo("rc-wrp+jwt");
		assertThat(jws.getHeader().getAlgorithm().getName()).isEqualTo("ES256");
		assertThat(jws.getHeader().getX509CertChain()).hasSize(1);
		X509Certificate leaf = X509CertUtils.parse(jws.getHeader().getX509CertChain().get(0).decode());
		assertThat(jws.verify(new ECDSAVerifier((ECPublicKey) leaf.getPublicKey()))).isTrue();
		leaf.verify(caCert().getPublicKey());
		assertThat(leaf.getIssuerX500Principal()).isEqualTo(caCert().getSubjectX500Principal());
	}

	@Test
	public void hasEveryMandatoryFieldOfTables7And9() throws Exception {
		JsonObject payload = payload();
		for (String field : List.of("name", "sub_ln", "sub", "country", "registry_uri", "srv_description", "entitlements",
				"privacy_policy", "info_uri", "support_uri", "supervisory_authority", "policy_id", "certificate_policy",
				"iat", "status", "purpose", "credentials")) {
			assertThat(payload.has(field)).as(field).isTrue();
		}
		assertThat(payload.getAsJsonArray("policy_id").toString()).isEqualTo("[\"0.4.0.19475.3.1\"]");
		assertThat(payload.getAsJsonArray("entitlements").toString())
			.isEqualTo("[\"https://uri.etsi.org/19475/Entitlement/Service_Provider\"]");
		JsonObject statusList = payload.getAsJsonObject("status").getAsJsonObject("status_list");
		assertThat(OIDFJSON.getInt(statusList.get("idx"))).isZero();
		assertThat(OIDFJSON.getString(statusList.get("uri"))).isEqualTo(SuiteWrprc.statusListUrl());
	}

	@Test
	public void subIsTheAccessCertificatesOrganizationIdentifier() throws Exception {
		// TS 119 475 GEN-5.1.1-02: the wallet binds the two certificates by this identifier
		X509Certificate wrpac = X509CertUtils.parse(TestKeysAndCerts.getWrpac().getJwk().getX509CertChain().get(0).decode());
		RDN[] rdns = X500Name.getInstance(wrpac.getSubjectX500Principal().getEncoded()).getRDNs(BCStyle.ORGANIZATION_IDENTIFIER);
		String sub = OIDFJSON.getString(payload().get("sub"));
		assertThat(sub).isEqualTo(TestKeysAndCerts.ORGANIZATION_IDENTIFIER);
		assertThat(IETFUtils.valueToString(rdns[0].getFirst().getValue())).isEqualTo(sub);
	}

	@Test
	public void expiresWithinTwelveMonthsOfIssue() throws Exception {
		// TS 119 475 GEN-5.2.4-08
		JsonObject payload = payload();
		long iat = OIDFJSON.getLong(payload.get("iat"));
		long exp = OIDFJSON.getLong(payload.get("exp"));
		assertThat(iat).isEqualTo(ISSUED.getEpochSecond());
		assertThat(exp).isGreaterThan(iat).isLessThanOrEqualTo(Instant.ofEpochSecond(iat).plusSeconds(365L * 24 * 3600).getEpochSecond());
	}

	@Test
	public void registersEveryClaimOfEveryBuiltInDcqlQuery() throws Exception {
		JsonArray credentials = payload().getAsJsonArray("credentials");
		Set<String> registered = new HashSet<>();
		for (JsonElement element : credentials) {
			JsonObject credential = element.getAsJsonObject();
			assertThat(credential.has("claims")).as("TS 119 475 names the member 'claim'").isFalse();
			for (JsonElement claim : credential.getAsJsonArray("claim")) {
				registered.add(OIDFJSON.getString(credential.get("format")) + " " + credential.get("meta")
					+ " " + claim.getAsJsonObject().get("path"));
			}
		}
		int requested = 0;
		for (JsonObject query : SuiteWrprc.builtInDcqlQueries()) {
			for (JsonElement element : query.getAsJsonArray("credentials")) {
				JsonObject credential = element.getAsJsonObject();
				for (JsonElement claim : credential.getAsJsonArray("claims")) {
					requested++;
					assertThat(registered).contains(OIDFJSON.getString(credential.get("format")) + " " + credential.get("meta")
						+ " " + claim.getAsJsonObject().get("path"));
				}
			}
		}
		assertThat(requested).isPositive();
		assertThat(SuiteWrprc.builtInDcqlQueries()).hasSizeGreaterThanOrEqualTo(8);
	}

	@Test
	public void theStatusListIsSignedByTheSameLeafAndSaysValid() throws Exception {
		JWSObject jws = JWSObject.parse(SuiteWrprc.statusListJwt(ISSUED));
		assertThat(jws.getHeader().getType().toString()).isEqualTo("statuslist+jwt");
		X509Certificate leaf = X509CertUtils.parse(jws.getHeader().getX509CertChain().get(0).decode());
		assertThat(jws.verify(new ECDSAVerifier((ECPublicKey) leaf.getPublicKey()))).isTrue();
		leaf.verify(caCert().getPublicKey());
		JsonObject payload = JsonParser.parseString(jws.getPayload().toString()).getAsJsonObject();
		assertThat(OIDFJSON.getString(payload.get("sub"))).isEqualTo(SuiteWrprc.statusListUrl());
		JsonObject statusList = payload.getAsJsonObject("status_list");
		TokenStatusList list = TokenStatusList.decode(OIDFJSON.getString(statusList.get("lst")), OIDFJSON.getInt(statusList.get("bits")));
		assertThat(list.getStatus(0)).isEqualTo(TokenStatusList.Status.VALID);
	}

	@Test
	public void verifierInfoCarriesTheRegistrarDatasetAndTheBase64urlEncodedCertificate() throws Exception {
		// TS 119 472-2 OIDFVP-HAIP-COMMON-REQ-RO-02 to RO-16
		JsonArray verifierInfo = SuiteWrprc.verifierInfo();
		assertThat(verifierInfo).hasSize(2);
		JsonObject dataset = verifierInfo.get(0).getAsJsonObject();
		assertThat(OIDFJSON.getString(dataset.get("format"))).isEqualTo("registrar_dataset");
		assertThat(dataset.has("credential_ids")).isFalse();
		for (String member : List.of("identifier", "srvDescription", "registryURI", "intendedUseIdentifier", "purpose", "policyURI")) {
			assertThat(dataset.getAsJsonObject("data").has(member)).as(member).isTrue();
		}
		JsonObject certificate = verifierInfo.get(1).getAsJsonObject();
		assertThat(OIDFJSON.getString(certificate.get("format"))).isEqualTo("registration_cert");
		assertThat(certificate.has("credential_ids")).isFalse();
		String jwt = new String(Base64.getUrlDecoder().decode(OIDFJSON.getString(certificate.get("data"))), StandardCharsets.US_ASCII);
		assertThat(JWSObject.parse(jwt).getHeader().getType().toString()).isEqualTo("rc-wrp+jwt");
	}

	private static JsonObject payload() throws Exception {
		return JsonParser.parseString(JWSObject.parse(SuiteWrprc.jwt(ISSUED)).getPayload().toString()).getAsJsonObject();
	}

	private static X509Certificate caCert() throws Exception {
		return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(
			new ByteArrayInputStream(TestKeysAndCerts.SuiteCa.WRPRC_PROVIDER.getCertPem().getBytes(StandardCharsets.US_ASCII)));
	}
}
