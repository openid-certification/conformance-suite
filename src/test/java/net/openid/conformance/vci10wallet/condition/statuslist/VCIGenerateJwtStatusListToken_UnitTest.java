package net.openid.conformance.vci10wallet.condition.statuslist;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jwt.SignedJWT;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.client.lote.LoteTestFixtures;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.oauth.statuslists.JwtStatusListTokenClaimsBuilder;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.TestKeysAndCerts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The emulated issuer's JWT status list is signed with the credential signing key, whose chain
 * goes in {@code x5c}: the built-in PID provider certificate, or the configured 'Signing JWK'.
 */
public class VCIGenerateJwtStatusListToken_UnitTest {

	private static final String ISSUER = "https://issuer.example.com/";

	private final Environment env = new Environment();
	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();
	private VCIGenerateJwtStatusListToken cond;

	@BeforeEach
	public void setUp() {
		cond = new VCIGenerateJwtStatusListToken();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
		env.putObject("config", new JsonObject());
		env.putObject("vci", new JsonObject());
		env.putString("server", "issuer", ISSUER);
		env.putString("current_status_list_id", "1");
	}

	@Test
	public void signsWithTheBuiltInPidProviderCertificateWhenNoSigningJwkIsConfigured() throws Exception {
		cond.execute(env);

		SignedJWT jwt = SignedJWT.parse(env.getString("current_status_list_jwt"));
		ECKey builtIn = TestKeysAndCerts.getSdJwtPidSigner().getJwk();
		assertThat(jwt.getHeader().getType().getType()).isEqualTo(JwtStatusListTokenClaimsBuilder.TYP);
		assertThat(jwt.getHeader().getX509CertChain()).isEqualTo(builtIn.getX509CertChain());
		assertThat(jwt.verify(new ECDSAVerifier(builtIn.toECPublicKey()))).isTrue();
		assertThat(jwt.getJWTClaimsSet().getSubject()).isEqualTo(ISSUER + "statuslists/1");
	}

	@Test
	public void signsWithTheConfiguredSigningJwkAndItsChain() throws Exception {
		LoteTestFixtures.ListedCa ca = LoteTestFixtures.INSTANCE.generateCa("Issuer CA", "Example Issuer");
		ECKey configured = LoteTestFixtures.INSTANCE.mintLeaf(ca, "issuer").getFirst();
		env.putObject("config", "credential.signing_jwk", JsonParser.parseString(configured.toJSONString()).getAsJsonObject());

		cond.execute(env);

		SignedJWT jwt = SignedJWT.parse(env.getString("current_status_list_jwt"));
		assertThat(jwt.getHeader().getX509CertChain()).isEqualTo(configured.getX509CertChain());
		assertThat(jwt.verify(new ECDSAVerifier(configured.toECPublicKey()))).isTrue();
	}
}
