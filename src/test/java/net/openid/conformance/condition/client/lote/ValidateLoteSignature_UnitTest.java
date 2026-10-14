package net.openid.conformance.condition.client.lote;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.util.Base64;
import com.nimbusds.jose.util.Base64URL;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ValidateLoteSignature_UnitTest {

	private final Environment env = new Environment();
	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();
	private final LoteTestFixtures.Signer signer = LoteTestFixtures.INSTANCE.generateSigner("EU", "LOTE User", null);
	private final LoteTestFixtures.ListedCa ca = LoteTestFixtures.INSTANCE.generateCa("PID Issuer CA", "Example Provider");
	private ValidateLoteSignature cond;

	@BeforeEach
	public void setUp() {
		cond = new ValidateLoteSignature();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.FAILURE);
	}

	@Test
	public void acceptsAListSignedByTheCertificateInItsHeader() {
		LoteTestFixtures.INSTANCE.putLote(env, LoteSlot.PID, LoteTestFixtures.INSTANCE.goodPidList(signer, ca.getCert()));

		assertThatCode(() -> cond.execute(env)).doesNotThrowAnyException();
	}

	@ParameterizedTest
	@ValueSource(strings = {"PIDProviders", "WalletProviders", "WRPACProviders", "WRPRCProviders", "PubEAAProviders"})
	public void acceptsTheEudiwDevelopmentLists(String name) {
		LoteTestFixtures.INSTANCE.putLote(env, LoteSlot.PID, LoteTestFixtures.INSTANCE.eudiwDevList(name));

		assertThatCode(() -> cond.execute(env)).doesNotThrowAnyException();
	}

	@Test
	public void rejectsATamperedPayload() throws Exception {
		String jws = LoteTestFixtures.INSTANCE.goodPidList(signer, ca.getCert());
		String[] parts = jws.split("\\.");
		String tamperedPayload = Base64URL.encode(new Payload(Base64URL.from(parts[1]).decodeToString()
			.replace("EU PID Provider Trusted Entity", "Mallory")).toString()).toString();
		LoteTestFixtures.INSTANCE.putLote(env, LoteSlot.PID, parts[0] + "." + tamperedPayload + "." + parts[2]);

		assertThatThrownBy(() -> cond.execute(env)).isInstanceOf(ConditionError.class).hasMessageContaining("does not verify");
	}

	@Test
	public void rejectsAListSignedByADifferentKeyThanTheCertificateInItsHeader() throws Exception {
		LoteTestFixtures.Signer other = LoteTestFixtures.INSTANCE.generateSigner("EU", "LOTE User", null);
		JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.ES256)
			.x509CertChain(List.of(Base64.encode(signer.getCert().getEncoded())))
			.build();
		JWSObject jws = new JWSObject(header, new Payload(LoteTestFixtures.INSTANCE.payload(LoteTestFixtures.PID_LIST_TYPE, null,
			java.time.Instant.now(), java.time.Instant.now().plusSeconds(3600), 1, "EU", "LOTE User", o -> { }).toString()));
		jws.sign(new com.nimbusds.jose.crypto.ECDSASigner(other.getKey()));
		LoteTestFixtures.INSTANCE.putLote(env, LoteSlot.PID, jws.serialize());

		assertThatThrownBy(() -> cond.execute(env)).isInstanceOf(ConditionError.class).hasMessageContaining("does not verify");
	}

	@Test
	public void rejectsAHeaderWithoutX5c() {
		LoteTestFixtures.INSTANCE.putLote(env, LoteSlot.PID, LoteTestFixtures.INSTANCE.sign(
			LoteTestFixtures.INSTANCE.payload(LoteTestFixtures.PID_LIST_TYPE, null, java.time.Instant.now(),
				java.time.Instant.now().plusSeconds(3600), 1, "EU", "LOTE User", o -> { }),
			signer, false, JWSAlgorithm.ES256));

		assertThatThrownBy(() -> cond.execute(env)).isInstanceOf(ConditionError.class).hasMessageContaining("x5c");
	}

	@Test
	public void rejectsAMismatchingX5tS256() throws Exception {
		JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.ES256)
			.x509CertChain(List.of(Base64.encode(signer.getCert().getEncoded())))
			.x509CertSHA256Thumbprint(Base64URL.encode(new byte[32]))
			.build();
		JWSObject jws = new JWSObject(header, new Payload("{\"LoTE\":{}}"));
		jws.sign(new com.nimbusds.jose.crypto.ECDSASigner(signer.getKey()));
		LoteTestFixtures.INSTANCE.putLote(env, LoteSlot.PID, jws.serialize());

		assertThatThrownBy(() -> cond.execute(env)).isInstanceOf(ConditionError.class).hasMessageContaining("x5t#S256");
	}

	@Test
	public void rejectsASymmetricAlgorithm() throws Exception {
		JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.HS256)
			.x509CertChain(List.of(Base64.encode(signer.getCert().getEncoded())))
			.build();
		JWSObject jws = new JWSObject(header, new Payload("{\"LoTE\":{}}"));
		jws.sign(new MACSigner(new byte[32]));
		LoteTestFixtures.INSTANCE.putLote(env, LoteSlot.PID, jws.serialize());

		assertThatThrownBy(() -> cond.execute(env)).isInstanceOf(ConditionError.class).hasMessageContaining("asymmetric");
	}
}
