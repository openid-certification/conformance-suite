package net.openid.conformance.condition.as;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.util.Base64;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.client.lote.LoteSlot;
import net.openid.conformance.condition.client.lote.LoteTestFixtures;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.X509CertificateUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.cert.X509Certificate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The request object's x5c chain may end at the configured 'Request Object Trust Anchor' or at
 * any access certificate provider listed in the configured WRPAC providers list.
 */
public class ValidateRequestObjectSignatureAgainstLoteAnchors_UnitTest {

	private final Environment env = new Environment();
	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();
	private final LoteTestFixtures.ListedCa accessCa = LoteTestFixtures.INSTANCE.generateCa("Access CA", "Access Org");
	private final LoteTestFixtures.ListedCa otherCa = LoteTestFixtures.INSTANCE.generateCa("Other CA", "Other Org");
	private ValidateRequestObjectSignatureAgainstX5cHeader cond;

	@BeforeEach
	public void setUp() {
		cond = new ValidateRequestObjectSignatureAgainstX5cHeader();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.FAILURE);
	}

	private void putRequestObjectSignedUnder(LoteTestFixtures.ListedCa ca) throws Exception {
		kotlin.Pair<ECKey, X509Certificate> leaf = LoteTestFixtures.INSTANCE.mintLeaf(ca, "verifier");
		SignedJWT jwt = new SignedJWT(
			new JWSHeader.Builder(JWSAlgorithm.ES256).x509CertChain(List.of(Base64.encode(leaf.getSecond().getEncoded()))).build(),
			new JWTClaimsSet.Builder().claim("client_id", "x509_hash:abc").build());
		jwt.sign(new ECDSASigner(leaf.getFirst()));
		env.putString("authorization_request_object", "value", jwt.serialize());
	}

	@Test
	public void passesWhenTheChainEndsAtAListedAccessCertificateProvider() throws Exception {
		putRequestObjectSignedUnder(accessCa);
		LoteTestFixtures.INSTANCE.putAnchors(env, LoteSlot.WRPAC, LoteTestFixtures.WRPAC_ISSUANCE, "Access CA entity", accessCa.getCert());

		assertThatCode(() -> cond.execute(env)).doesNotThrowAnyException();
		assertThat(env.getString("request_object_signing_alg")).isEqualTo("ES256");
	}

	@Test
	public void passesWhenTheChainEndsAtTheConfiguredAnchorWhileAListIsAlsoPresent() throws Exception {
		putRequestObjectSignedUnder(otherCa);
		env.putString("client_request_object_trust_anchor_pem", X509CertificateUtil.toPem(otherCa.getCert()));
		LoteTestFixtures.INSTANCE.putAnchors(env, LoteSlot.WRPAC, LoteTestFixtures.WRPAC_ISSUANCE, "Access CA entity", accessCa.getCert());

		assertThatCode(() -> cond.execute(env)).doesNotThrowAnyException();
	}

	@Test
	public void failsWhenTheListIsConfiguredButListsNoAccessCertificateProvider() throws Exception {
		putRequestObjectSignedUnder(accessCa);
		LoteTestFixtures.INSTANCE.putAnchors(env, LoteSlot.WRPAC, LoteTestFixtures.WRPAC_ISSUANCE, "Access CA entity");

		assertThatThrownBy(() -> cond.execute(env)).isInstanceOf(ConditionError.class).hasMessageContaining("trusts nobody");
	}

	@Test
	public void failsWhenTheChainEndsAtNoneOfTheAnchors() throws Exception {
		putRequestObjectSignedUnder(otherCa);
		LoteTestFixtures.INSTANCE.putAnchors(env, LoteSlot.WRPAC, LoteTestFixtures.WRPAC_ISSUANCE, "Access CA entity", accessCa.getCert());

		assertThatThrownBy(() -> cond.execute(env)).isInstanceOf(ConditionError.class).hasMessageContaining("PKIX path validation failed");
	}
}
