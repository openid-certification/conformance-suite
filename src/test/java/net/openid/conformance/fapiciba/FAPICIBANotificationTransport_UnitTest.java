package net.openid.conformance.fapiciba;

import com.google.gson.JsonObject;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.runner.TestExecutionManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class FAPICIBANotificationTransport_UnitTest {

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = { "  ", "(null)" })
	public void rejectsMissingCertificateBeforeDispatchingPing(String certificate) {
		TestableModule module = new TestableModule();
		ResponseEntity<?> response = (ResponseEntity<?>) module.handleHttpMtls(
			"ciba-notification-endpoint", null, null, null, request(certificate));

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
		assertThat(module.pingDispatched).isFalse();
	}

	@Test
	public void dispatchesNotificationWithClientCertificate() {
		TestableModule module = new TestableModule();
		module.handleHttpMtls("ciba-notification-endpoint", null, null, null, request("certificate"));
		assertThat(module.pingDispatched).isTrue();
	}

	private static JsonObject request(String certificate) {
		JsonObject headers = new JsonObject();
		headers.addProperty("x-ssl-cert", certificate);
		JsonObject request = new JsonObject();
		request.add("headers", headers);
		return request;
	}

	private static class TestableModule extends AbstractFAPICIBAID1 {
		private boolean pingDispatched;

		private TestableModule() {
			profileBehavior = new OpenBankingBrazilCibaServerProfileBehavior();
			executionManager = mock(TestExecutionManager.class);
			eventLog = BsonEncoding.testInstanceEventLog();
		}

		@Override
		protected Object handlePingCallback(JsonObject requestParts) {
			pingDispatched = true;
			return new ResponseEntity<>("", HttpStatus.NO_CONTENT);
		}
	}
}
