package net.openid.conformance.fapiciba;

import com.google.gson.JsonObject;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.runner.TestExecutionManager;
import net.openid.conformance.info.TestInfoService;
import net.openid.conformance.testmodule.TestFailureException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.concurrent.Callable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.ArgumentMatchers.any;

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
	public void wrongHostResponseAndFailureIdentifyTheMtlsNotificationUri() {
		AbstractFAPICIBAID1 module = new FAPICIBAID1EnsureOtherScopeOrderSucceeds();
		TestExecutionManager manager = mock(TestExecutionManager.class);
		java.util.List<Callable<?>> workers = new java.util.ArrayList<>();
		doAnswer(invocation -> {
			workers.add(invocation.getArgument(0));
			return null;
		}).when(manager).runInBackground(any());
		module.setProperties("UNIT-TEST", Map.of(), BsonEncoding.testInstanceEventLog(), null,
			mock(TestInfoService.class), manager, null);
		module.setupOpenBankingBrazil();
		// Represent a configured module waiting for its callback, without contacting an external AS.
		ReflectionTestUtils.setField(module, "status", net.openid.conformance.testmodule.TestModule.Status.WAITING);
		module.getEnv().putString("notification_uri", "https://mtls.example/test-mtls/id/ciba-notification-endpoint");

		ResponseEntity<?> response = (ResponseEntity<?>) module.handleHttp(
			"ciba-notification-endpoint", null, null, null, request(null));

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(response.getBody().toString()).contains("https://mtls.example/test-mtls/id/ciba-notification-endpoint");
		assertThat(workers).hasSize(1);
		assertThatThrownBy(workers.getFirst()::call).isInstanceOf(TestFailureException.class)
			.hasMessageContaining("mTLS notification endpoint");
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
