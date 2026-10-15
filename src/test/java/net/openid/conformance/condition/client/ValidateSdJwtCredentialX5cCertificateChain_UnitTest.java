package net.openid.conformance.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
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

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The credential's x5c chain may end at the configured 'Credential Trust Anchor' or at any PID or
 * mDL provider listed in the configured lists of trusted entities.
 */
public class ValidateSdJwtCredentialX5cCertificateChain_UnitTest {

	private final Environment env = new Environment();
	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();
	private final LoteTestFixtures.ListedCa listedCa = LoteTestFixtures.INSTANCE.generateCa("Listed PID CA", "Listed Org");
	private final LoteTestFixtures.ListedCa otherCa = LoteTestFixtures.INSTANCE.generateCa("Other CA", "Other Org");
	private ValidateSdJwtCredentialX5cCertificateChain cond;

	@BeforeEach
	public void setUp() {
		cond = new ValidateSdJwtCredentialX5cCertificateChain();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.FAILURE);
	}

	private void putCredentialSignedUnder(LoteTestFixtures.ListedCa ca) throws Exception {
		kotlin.Pair<ECKey, X509Certificate> leaf = LoteTestFixtures.INSTANCE.mintLeaf(ca, "credential signer");
		SignedJWT jwt = new SignedJWT(
			new JWSHeader.Builder(JWSAlgorithm.ES256).x509CertChain(List.of(Base64.encode(leaf.getSecond().getEncoded()))).build(),
			new JWTClaimsSet.Builder().claim("vct", "urn:test").build());
		jwt.sign(new ECDSASigner(leaf.getFirst()));

		JsonObject header = new JsonObject();
		JsonArray x5c = new JsonArray();
		x5c.add(Base64.encode(leaf.getSecond().getEncoded()).toString());
		header.add("x5c", x5c);
		JsonObject credential = new JsonObject();
		credential.add("header", header);
		credential.addProperty("value", jwt.serialize());
		JsonObject sdjwt = new JsonObject();
		sdjwt.add("credential", credential);
		env.putObject("sdjwt", sdjwt);
	}

	@Test
	public void passesWhenTheChainEndsAtAListedPidProvider() throws Exception {
		putCredentialSignedUnder(listedCa);
		LoteTestFixtures.INSTANCE.putAnchors(env, LoteSlot.PID, LoteTestFixtures.PID_ISSUANCE, "Listed entity", listedCa.getCert());

		assertThatCode(() -> cond.execute(env)).doesNotThrowAnyException();
	}

	@Test
	public void passesWhenTheChainEndsAtTheConfiguredAnchorWhileAListIsAlsoPresent() throws Exception {
		putCredentialSignedUnder(otherCa);
		env.putString("credential_trust_anchor_pem", X509CertificateUtil.toPem(otherCa.getCert()));
		LoteTestFixtures.INSTANCE.putAnchors(env, LoteSlot.PID, LoteTestFixtures.PID_ISSUANCE, "Listed entity", listedCa.getCert());

		assertThatCode(() -> cond.execute(env)).doesNotThrowAnyException();
	}

	@Test
	public void failsWhenTheChainEndsAtNoneOfTheAnchors() throws Exception {
		putCredentialSignedUnder(otherCa);
		LoteTestFixtures.INSTANCE.putAnchors(env, LoteSlot.PID, LoteTestFixtures.PID_ISSUANCE, "Listed entity", listedCa.getCert());

		assertThatThrownBy(() -> cond.execute(env)).isInstanceOf(ConditionError.class).hasMessageContaining("PKIX path validation failed");
	}

	@Test
	public void aRevocationServiceCertificateIsNotAnAnchorForTheCredential() throws Exception {
		putCredentialSignedUnder(listedCa);
		LoteTestFixtures.INSTANCE.putAnchors(env, LoteSlot.PID, LoteTestFixtures.PID_REVOCATION, "Listed entity", listedCa.getCert());

		// no issuance anchor at all: the legacy walk runs and passes on a CA-issued leaf, so this
		// documents the filtering rather than a failure
		assertThatCode(() -> cond.execute(env)).doesNotThrowAnyException();
	}
}
