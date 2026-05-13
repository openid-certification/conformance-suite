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
 * Unit tests for {@link CheckForBCP195InsecureFAPICiphers}.
 *
 * The condition's insecure list under BCP195 is just the two DHE_RSA AES-GCM ciphers; all
 * other TLS 1.2 ciphers (including TLS_ECDHE_RSA_*_GCM_*) are not in the offered set, so the
 * test server must specifically be configured with one of the DHE_RSA AES-GCM ciphers to
 * trigger a FAILURE.
 *
 * Tests marked {@code @Disabled} demonstrate the bug tracked by GitLab issue #1787 — same
 * underlying root cause as in DisallowInsecureCipher_UnitTest.
 */
@ExtendWith(MockitoExtension.class)
@Timeout(10)
public class CheckForBCP195InsecureFAPICiphers_UnitTest {

	// In the condition's insecure list — DHE_RSA AES-GCM was flagged by BCP195/RFC9325.
	private static final int INSECURE_BCP195_CIPHER = CipherSuite.TLS_DHE_RSA_WITH_AES_128_GCM_SHA256;

	// Not in the BCP195-insecure list — ECDHE_RSA AES-GCM is allowed.
	private static final int ALLOWED_BCP195_CIPHER = CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256;

	// Mandatory-to-implement TLS 1.3 cipher.
	private static final int TLS13_CIPHER = CipherSuite.TLS_AES_128_GCM_SHA256;

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private CheckForBCP195InsecureFAPICiphers cond;

	@BeforeEach
	public void setUp() {
		cond = new CheckForBCP195InsecureFAPICiphers();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.WARNING);
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
				new int[]{INSECURE_BCP195_CIPHER})) {
			setTlsEnv(server.getPort());
			assertCipherFailure("DHE_RSA_WITH_AES_128_GCM_SHA256");
		}
	}

	@Test
	public void tls12Only_serverAcceptsOnlyAllowedCiphers_succeeds() throws Exception {
		try (TestTlsServer server = new TestTlsServer(
				new ProtocolVersion[]{ProtocolVersion.TLSv12},
				new int[]{ALLOWED_BCP195_CIPHER})) {
			setTlsEnv(server.getPort());
			assertDoesNotThrow(() -> cond.execute(env));
		}
	}

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
				new int[]{ALLOWED_BCP195_CIPHER, TLS13_CIPHER})) {
			setTlsEnv(server.getPort());
			assertDoesNotThrow(() -> cond.execute(env));
		}
	}

	@Test
	public void tls12And13_serverAcceptsInsecureCipher_fails() throws Exception {
		try (TestTlsServer server = new TestTlsServer(
				new ProtocolVersion[]{ProtocolVersion.TLSv13, ProtocolVersion.TLSv12},
				new int[]{INSECURE_BCP195_CIPHER, TLS13_CIPHER})) {
			setTlsEnv(server.getPort());
			assertCipherFailure("DHE_RSA_WITH_AES_128_GCM_SHA256");
		}
	}
}
