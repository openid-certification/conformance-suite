package net.openid.conformance.condition.client;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class ValidateConfiguredClientCertificatesAgainstRical_UnitTest {

	private ValidateConfiguredClientCertificatesAgainstRical cond;

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private Environment env;

	private RicalTestFixtures.ReaderPki pki;

	@BeforeEach
	public void setUp() {
		cond = new ValidateConfiguredClientCertificatesAgainstRical();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.WARNING);
		env = new Environment();
		pki = RicalTestFixtures.generateReaderPki();
	}

	@Test
	public void testEvaluate_passesWhenClientChainCovered() {
		RicalTestFixtures.putClientJwks(env, pki);
		RicalTestFixtures.putRical(env, RicalTestFixtures.goodSignedRical(List.of(pki.getCaCert())));

		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void testEvaluate_failsWhenClientChainNotCovered() {
		RicalTestFixtures.putClientJwks(env, pki);
		RicalTestFixtures.ReaderPki otherPki = RicalTestFixtures.generateReaderPki("Some Other Reader CA");
		RicalTestFixtures.putRical(env, RicalTestFixtures.goodSignedRical(List.of(otherPki.getCaCert())));

		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("does not chain"), e.getMessage());
	}

	@Test
	public void testEvaluate_checksSecondClientToo() {
		RicalTestFixtures.putClientJwks(env, pki);
		RicalTestFixtures.ReaderPki secondPki = RicalTestFixtures.generateReaderPki("Second Reader CA");
		RicalTestFixtures.putClientJwks(env, secondPki, "client2_jwks");
		// only the first client's CA is listed
		RicalTestFixtures.putRical(env, RicalTestFixtures.goodSignedRical(List.of(pki.getCaCert())));

		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("Second client JWKS"), e.getMessage());
	}

	@Test
	public void testEvaluate_passesWhenBothClientsCovered() {
		RicalTestFixtures.putClientJwks(env, pki);
		RicalTestFixtures.ReaderPki secondPki = RicalTestFixtures.generateReaderPki("Second Reader CA");
		RicalTestFixtures.putClientJwks(env, secondPki, "client2_jwks");
		RicalTestFixtures.putRical(env,
			RicalTestFixtures.goodSignedRical(List.of(pki.getCaCert(), secondPki.getCaCert())));

		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void testEvaluate_failsWhenJwksHasNoX5c() {
		com.google.gson.JsonObject jwks = com.google.gson.JsonParser.parseString(
			"{\"keys\":[{\"kty\":\"EC\",\"crv\":\"P-256\",\"x\":\"AA\",\"y\":\"AA\"}]}").getAsJsonObject();
		env.putObject("client_jwks", jwks);
		RicalTestFixtures.putRical(env, RicalTestFixtures.goodSignedRical(List.of(pki.getCaCert())));

		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("x5c"), e.getMessage());
	}
}
