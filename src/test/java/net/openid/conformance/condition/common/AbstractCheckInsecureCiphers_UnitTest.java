package net.openid.conformance.condition.common;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.common.util.TestTlsServer;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.bouncycastle.tls.AlertDescription;
import org.bouncycastle.tls.CipherSuite;
import org.bouncycastle.tls.ProtocolVersion;
import org.bouncycastle.tls.TlsFatalAlertReceived;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@Timeout(10)
public class AbstractCheckInsecureCiphers_UnitTest {

	private static final int ALLOWED_CIPHER = CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256;
	private static final int TLS13_CIPHER = CipherSuite.TLS_AES_128_GCM_SHA256;

	private enum CipherCheck {
		FAPI(DisallowInsecureCipher::new, ConditionResult.FAILURE,
			CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA, "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA"),
		BCP195_WARNING(CheckForBCP195InsecureFAPICiphers::new, ConditionResult.WARNING,
			CipherSuite.TLS_DHE_RSA_WITH_AES_128_GCM_SHA256, "DHE_RSA_WITH_AES_128_GCM_SHA256"),
		BCP195_REQUIRED(RequireOnlyBCP195RecommendedCiphersForTLS12::new, ConditionResult.FAILURE,
			CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA, "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA");

		private final Supplier<AbstractCheckInsecureCiphers> factory;
		private final ConditionResult severity;
		private final int insecureCipher;
		private final String insecureCipherName;

		CipherCheck(Supplier<AbstractCheckInsecureCiphers> factory, ConditionResult severity,
				int insecureCipher, String insecureCipherName) {
			this.factory = factory;
			this.severity = severity;
			this.insecureCipher = insecureCipher;
			this.insecureCipherName = insecureCipherName;
		}
	}

	private final Environment env = new Environment();
	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private AbstractCheckInsecureCiphers condition(CipherCheck check, int port) {
		JsonObject tls = new JsonObject();
		tls.addProperty("testHost", "127.0.0.1");
		tls.addProperty("testPort", port);
		env.putObject("tls", tls);
		AbstractCheckInsecureCiphers condition = check.factory.get();
		condition.setProperties("UNIT-TEST", eventLog, check.severity);
		return condition;
	}

	private static Stream<Arguments> mtlsCases() {
		return Arrays.stream(CipherCheck.values()).flatMap(check ->
			Stream.of(AlertDescription.bad_certificate, AlertDescription.handshake_failure).flatMap(alert ->
				Stream.of(false, true).map(tls13 -> Arguments.of(check, alert, tls13))));
	}

	private ProtocolVersion[] versions(boolean tls13) {
		return tls13 ? new ProtocolVersion[]{ProtocolVersion.TLSv13, ProtocolVersion.TLSv12}
			: new ProtocolVersion[]{ProtocolVersion.TLSv12};
	}

	@ParameterizedTest
	@MethodSource("mtlsCases")
	public void mtlsWithAllowedCiphers_runsCipherCheck(CipherCheck check, short alert, boolean tls13) throws Exception {
		try (TestTlsServer server = new TestTlsServer(versions(tls13),
				new int[]{ALLOWED_CIPHER, TLS13_CIPHER}, alert)) {
			AbstractCheckInsecureCiphers condition = condition(check, server.getPort());
			assertDoesNotThrow(() -> condition.execute(env));
			assertEquals(2, server.getConnectionCount(), "Must run the cipher check after the TLS 1.2 probe");
		}
	}

	@ParameterizedTest
	@MethodSource("mtlsCases")
	public void mtlsWithInsecureCipher_reportsCipher(CipherCheck check, short alert, boolean tls13) throws Exception {
		try (TestTlsServer server = new TestTlsServer(versions(tls13),
				new int[]{ALLOWED_CIPHER, check.insecureCipher, TLS13_CIPHER}, alert)) {
			AbstractCheckInsecureCiphers condition = condition(check, server.getPort());
			ConditionError error = assertThrows(ConditionError.class, () -> condition.execute(env));
			assertTrue(error.getMessage().contains("Server accepted a cipher that is not on the list of permitted ciphers"));
			assertEquals(2, server.getConnectionCount());

			@SuppressWarnings("unchecked")
			ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
			verify(eventLog, atLeastOnce()).log(anyString(), captor.capture());
			assertTrue(captor.getAllValues().stream().anyMatch(entry ->
				check.insecureCipherName.equals(entry.get("cipher_suite")) && check.severity.equals(entry.get("result"))));
		}
	}

	@ParameterizedTest
	@EnumSource(CipherCheck.class)
	public void tls13Only_doesNotRunTls12CipherCheck(CipherCheck check) throws Exception {
		try (TestTlsServer server = new TestTlsServer(new ProtocolVersion[]{ProtocolVersion.TLSv13},
				new int[]{TLS13_CIPHER})) {
			AbstractCheckInsecureCiphers condition = condition(check, server.getPort());
			assertDoesNotThrow(() -> condition.execute(env));
			assertEquals(1, server.getConnectionCount());
		}
	}

	@ParameterizedTest
	@EnumSource(CipherCheck.class)
	public void unexpectedAlertBeforeServerHello_fails(CipherCheck check) throws Exception {
		try (ServerSocket server = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"));
				var executor = Executors.newSingleThreadExecutor()) {
			server.setSoTimeout(2000);
			var response = executor.submit(() -> {
				try (var client = server.accept()) {
					client.setSoTimeout(2000);
					var input = client.getInputStream();
					byte[] header = input.readNBytes(5);
					int length = ((header[3] & 0xff) << 8) | (header[4] & 0xff);
					input.readNBytes(length);
					// TLS alert record: fatal internal_error before any ServerHello.
					client.getOutputStream().write(new byte[]{21, 3, 3, 0, 2, 2, AlertDescription.internal_error});
				}
				return null;
			});
			AbstractCheckInsecureCiphers condition = condition(check, server.getLocalPort());
			ConditionError error = assertThrows(ConditionError.class, () -> condition.execute(env));
			TlsFatalAlertReceived cause = assertInstanceOf(TlsFatalAlertReceived.class, error.getCause());
			assertEquals(AlertDescription.internal_error, cause.getAlertDescription());
			response.get(2, TimeUnit.SECONDS);
		}
	}
}
