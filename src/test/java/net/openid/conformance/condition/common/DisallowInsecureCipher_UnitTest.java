package net.openid.conformance.condition.common;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.common.util.TestTlsServer;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.bouncycastle.tls.CipherSuite;
import org.bouncycastle.tls.ProtocolVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link DisallowInsecureCipher}.
 *
 * Uses an in-process BouncyCastle TLS server fixture so each test runs against a server
 * with a known protocol-version and cipher-suite configuration. Tests cover the three
 * protocol-version topologies the condition encounters in the wild: TLS 1.2-only,
 * TLS 1.3-only, and TLS 1.2+1.3.
 *
 * Tests marked {@code @Disabled} demonstrate the bug tracked by GitLab issue #1787. The
 * annotation message records the exact failure mode observed today; re-enable after the
 * probe-and-skip fix lands.
 */
@ExtendWith(MockitoExtension.class)
@Timeout(10)
public class DisallowInsecureCipher_UnitTest {

	// Allowed by the FAPI cipher allow-list (see FAPITLSClient#FAPI_TLS_1_2_CIPHERS).
	private static final int ALLOWED_FAPI_CIPHER = CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256;

	// Not in the FAPI allow-list, so DisallowInsecureCipher offers it as an "insecure" cipher.
	private static final int INSECURE_CIPHER = CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA;

	// Mandatory-to-implement TLS 1.3 cipher.
	private static final int TLS13_CIPHER = CipherSuite.TLS_AES_128_GCM_SHA256;

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private DisallowInsecureCipher cond;

	@BeforeEach
	public void setUp() {
		cond = new DisallowInsecureCipher();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.FAILURE);
	}

	private void setTlsEnv(int port) {
		JsonObject tls = JsonParser.parseString(
			"{\"testHost\":\"127.0.0.1\",\"testPort\":" + port + "}").getAsJsonObject();
		env.putObject("tls", tls);
	}

	/**
	 * Assert that the condition failed via the "server accepted an insecure cipher" path
	 * (not via an unexpected TLS error or a fixture/credential regression) and that the
	 * reported cipher matches the expected name. The cipher name comes from the args map
	 * passed to {@code error(...)} in {@code notifySelectedCipherSuite}, so we capture it
	 * off the spied event log rather than the exception message.
	 */
	private void assertCipherFailure(String expectedCipherName) {
		ConditionError ex = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(
			ex.getMessage().contains("Server accepted a cipher that is not on the list of permitted ciphers"),
			"Expected failure on the 'server accepted insecure cipher' path, got: " + ex.getMessage());

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
		verify(eventLog, atLeastOnce()).log(anyString(), captor.capture());
		boolean found = captor.getAllValues().stream()
			.anyMatch(m -> expectedCipherName.equals(m.get("cipher_suite")));
		assertTrue(found,
			"Expected failure to report selected cipher '" + expectedCipherName + "'; captured log payloads: "
				+ captor.getAllValues());
	}

	@Test
	public void tls12Only_serverAcceptsInsecureCipher_fails() throws Exception {
		try (TestTlsServer server = new TestTlsServer(
				new ProtocolVersion[]{ProtocolVersion.TLSv12},
				new int[]{INSECURE_CIPHER})) {
			setTlsEnv(server.getPort());
			assertCipherFailure("TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA");
		}
	}

	@Test
	public void tls12Only_serverAcceptsOnlyAllowedCiphers_succeeds() throws Exception {
		try (TestTlsServer server = new TestTlsServer(
				new ProtocolVersion[]{ProtocolVersion.TLSv12},
				new int[]{ALLOWED_FAPI_CIPHER})) {
			setTlsEnv(server.getPort());
			assertDoesNotThrow(() -> cond.execute(env));
		}
	}

	@Disabled("Tracks GitLab #1787 — currently throws ConditionError "
		+ "'Failed to make TLS connection, but in a different way than expected' "
		+ "because the catch ladder in AbstractCheckInsecureCiphers does not recognise the "
		+ "protocol_version alert sent by a TLS 1.3-only server when the client offers only "
		+ "TLS 1.2. Enable when the probe-and-skip fix lands.")
	@Test
	public void tls13Only_shouldSkipCleanly() throws Exception {
		try (TestTlsServer server = new TestTlsServer(
				new ProtocolVersion[]{ProtocolVersion.TLSv13},
				new int[]{TLS13_CIPHER})) {
			setTlsEnv(server.getPort());
			assertDoesNotThrow(() -> cond.execute(env));
		}
	}

	@Test
	public void tls12And13_serverAcceptsOnlyAllowedCiphers_succeeds() throws Exception {
		try (TestTlsServer server = new TestTlsServer(
				new ProtocolVersion[]{ProtocolVersion.TLSv13, ProtocolVersion.TLSv12},
				new int[]{ALLOWED_FAPI_CIPHER, TLS13_CIPHER})) {
			setTlsEnv(server.getPort());
			assertDoesNotThrow(() -> cond.execute(env));
		}
	}

	@Test
	public void tls12And13_serverAcceptsInsecureCipher_fails() throws Exception {
		try (TestTlsServer server = new TestTlsServer(
				new ProtocolVersion[]{ProtocolVersion.TLSv13, ProtocolVersion.TLSv12},
				new int[]{INSECURE_CIPHER, TLS13_CIPHER})) {
			setTlsEnv(server.getPort());
			assertCipherFailure("TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA");
		}
	}
}
