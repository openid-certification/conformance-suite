package net.openid.conformance.fapiciba.rp;

import com.google.gson.JsonObject;
import net.openid.conformance.frontchannel.BrowserControl;
import net.openid.conformance.info.ImageService;
import net.openid.conformance.info.TestInfoService;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.runner.TestExecutionManager;
import net.openid.conformance.runner.TestRunnerSupport;
import net.openid.conformance.security.AuthenticationFacade;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.testmodule.TestFailureException;
import net.openid.conformance.testmodule.TestModule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.ArrayDeque;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ExecutorCompletionService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// The suite OP rejects the certless notification and stops, so its pairing cannot exercise a later poll.
public class FAPICIBAClientPingWithoutMTLSCertificateTest_UnitTest {

	private TestableModule module;
	private Queue<Runnable> tasks;

	@BeforeEach
	public void setUp() {
		module = new TestableModule();
		var support = mock(TestRunnerSupport.class);
		when(support.getRunningTestById("certless-ping-test")).thenReturn(module);
		tasks = new ArrayDeque<>();
		var completion = new ExecutorCompletionService<Object>(tasks::add);
		var manager = new TestExecutionManager("certless-ping-test", completion,
			mock(AuthenticationFacade.class), support);
		module.setProperties("certless-ping-test", Map.of(), mock(TestInstanceEventLog.class),
			mock(BrowserControl.class), mock(TestInfoService.class), manager, mock(ImageService.class));
		module.startWaiting();
	}

	@AfterEach
	public void releaseLock() {
		module.forceReleaseLock();
	}

	@Test
	public void latePollReturnsTerminalErrorWithoutChangingPassedResult() {
		module.completePing();
		runFinalization();
		assertFinished();
		for (int attempt = 0; attempt < 2; attempt++) {
			assertTerminalResponse(module.handleHttpMtls("token", null, null, null, new JsonObject()));
			assertThat(module.getStatus()).isEqualTo(TestModule.Status.FINISHED);
			assertThat(module.getResult()).isEqualTo(TestModule.Result.PASSED);
			module.checkLockReleased();
		}
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	public void pollAfterPingCompletionReturnsTerminalErrorBeforeFinalization(boolean viaMtlsEntry) {
		module.completePing();
		assertThat(module.getStatus()).isEqualTo(TestModule.Status.WAITING);

		Object response = viaMtlsEntry
			? module.handleHttpMtls("token", null, null, null, new JsonObject())
			: module.tokenEndpoint("incoming_request");

		assertTerminalResponse(response);
		assertThat(module.getStatus()).isEqualTo(TestModule.Status.WAITING);
		assertThat(module.getResult()).isEqualTo(TestModule.Result.UNKNOWN);
		module.checkLockReleased();
		runFinalization();
		assertFinished();
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	public void pingCompletingBetweenTlsChecksAndTokenDispatchReturnsTerminalError(boolean finalizeBeforeDispatch) {
		module.beforeTokenDispatch = () -> {
			module.checkLockReleased();
			module.completePing();
			if (finalizeBeforeDispatch) {
				runFinalization();
			}
		};

		assertTerminalResponse(module.handleHttpMtls("token", null, null, null, tlsRequest()));
		module.checkLockReleased();
		if (!finalizeBeforeDispatch) {
			assertThat(module.getStatus()).isEqualTo(TestModule.Status.WAITING);
			runFinalization();
		}
		assertFinished();
	}

	@Test
	public void pollBeforePingCompletionStillValidatesRequest() {
		module.getEnv().putObjectFromJsonString("client", "{\"client_id\":\"expected-client\"}");
		JsonObject request = tlsRequest();
		JsonObject form = new JsonObject();
		form.addProperty("client_id", "wrong-client");
		form.addProperty("grant_type", "urn:openid:params:grant-type:ciba");
		request.add("body_form_params", form);

		assertThatThrownBy(() -> module.handleHttpMtls("token", null, null, null, request))
			.isInstanceOf(TestFailureException.class)
			.hasMessageContaining("client_id on the request wrong-client does not match");
	}

	@Test
	public void completesWithoutRequiringFallbackPolling() {
		module.completePing();
		runFinalization();
		assertFinished();
	}

	private void runFinalization() {
		tasks.remove().run();
		// Finalization cancels its own task; clear the interrupt on this test thread.
		Thread.interrupted();
	}

	private void assertFinished() {
		assertThat(module.getStatus()).isEqualTo(TestModule.Status.FINISHED);
		assertThat(module.getResult()).isEqualTo(TestModule.Result.PASSED);
		module.checkLockReleased();
	}

	private void assertTerminalResponse(Object result) {
		var response = (ResponseEntity<?>) result;
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
		assertThat(response.getHeaders().getCacheControl()).isEqualTo("no-store");
		assertThat(response.getHeaders().getPragma()).isEqualTo("no-cache");
		assertThat(OIDFJSON.getString(((JsonObject) response.getBody()).get("error"))).isEqualTo("invalid_grant");
	}

	private JsonObject tlsRequest() {
		JsonObject request = new JsonObject();
		JsonObject headers = new JsonObject();
		headers.addProperty("x-ssl-protocol", "TLSv1.3");
		request.add("headers", headers);
		return request;
	}

	private static class TestableModule extends FAPICIBAClientPingWithoutMTLSCertificateTest {
		private Runnable beforeTokenDispatch;

		@Override
		public String getName() {
			return "certless-ping-test";
		}

		@Override
		protected Object tokenEndpoint(String requestId) {
			if (beforeTokenDispatch != null) {
				beforeTokenDispatch.run();
			}
			return super.tokenEndpoint(requestId);
		}

		void startWaiting() {
			setStatus(Status.CONFIGURED);
			setStatus(Status.WAITING);
		}

		void completePing() {
			setStatus(Status.RUNNING);
			pingRequestComplete();
		}
	}
}
