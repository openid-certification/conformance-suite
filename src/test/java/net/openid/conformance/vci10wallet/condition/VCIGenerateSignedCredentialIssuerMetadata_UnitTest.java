package net.openid.conformance.vci10wallet.condition;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jwt.SignedJWT;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.TestKeysAndCerts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class VCIGenerateSignedCredentialIssuerMetadata_UnitTest {

	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private VCIGenerateSignedCredentialIssuerMetadata cond;

	@BeforeEach
	public void setUp() {
		cond = new VCIGenerateSignedCredentialIssuerMetadata();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
		env.putObject("credential_issuer_metadata",
			JsonParser.parseString("{\"credential_issuer\":\"https://issuer.example\"}").getAsJsonObject());
	}

	@Test
	public void signsWithTheBuiltInAccessCertificateWhenNoSigningJwkIsConfigured() throws Exception {
		// ETSI TS 119 472-3 ISS-MDATA-4.2.1-02: the issuer metadata is signed with the issuer's access certificate
		env.putObject("config", new JsonObject());

		cond.execute(env);

		SignedJWT jwt = SignedJWT.parse(env.getString("signed_credential_issuer_metadata"));
		ECKey wrpac = TestKeysAndCerts.getWrpac().getJwk();
		assertThat(jwt.getHeader().getX509CertChain()).containsExactly(wrpac.getX509CertChain().get(0));
		assertThat(jwt.verify(new com.nimbusds.jose.crypto.ECDSAVerifier(wrpac.toECPublicKey()))).isTrue();
		assertThat(jwt.getJWTClaimsSet().getStringClaim("credential_issuer")).isEqualTo("https://issuer.example");
	}

	@Test
	public void signsWithTheConfiguredSigningJwkWhenThereIsOne() throws Exception {
		ECKey configured = TestKeysAndCerts.getSdJwtPidSigner().getJwk();
		JsonObject config = new JsonObject();
		JsonObject credential = new JsonObject();
		credential.add("signing_jwk", JsonParser.parseString(configured.toJSONString()));
		config.add("credential", credential);
		env.putObject("config", config);

		cond.execute(env);

		SignedJWT jwt = SignedJWT.parse(env.getString("signed_credential_issuer_metadata"));
		assertThat(jwt.getHeader().getX509CertChain()).containsExactly(configured.getX509CertChain().get(0));
		assertThat(jwt.verify(new com.nimbusds.jose.crypto.ECDSAVerifier(configured.toECPublicKey()))).isTrue();
	}
}
