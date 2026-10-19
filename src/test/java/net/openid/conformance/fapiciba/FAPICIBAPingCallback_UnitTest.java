package net.openid.conformance.fapiciba;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.frontchannel.BrowserControl;
import net.openid.conformance.info.ImageService;
import net.openid.conformance.info.TestInfoService;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.runner.TestExecutionManager;
import net.openid.conformance.runner.TestRunnerSupport;
import net.openid.conformance.security.AuthenticationFacade;
import net.openid.conformance.testmodule.TestFailureException;
import net.openid.conformance.testmodule.TestModule;
import net.openid.conformance.variant.CIBAMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayDeque;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FAPICIBAPingCallback_UnitTest {
	private final Queue<Runnable> tasks = new ArrayDeque<>();
	private Runnable afterTaskQueued = () -> { };
	private final ExecutorCompletionService<Object> completion = new ExecutorCompletionService<>(task -> {
		tasks.add(task);
		afterTaskQueued.run();
	});
	private final CallbackModule module = new CallbackModule();

	@BeforeEach
	public void initialize() {
		initializeModule(module);
		module.testType = CIBAMode.PING;
		module.getEnv().putString("client_notification_token", "notification-token");
		module.getEnv().putObject("authorization_endpoint_request", new JsonObject());
		module.setAuthReqId("first");
		module.waitForCallback();
	}

	private void initializeModule(AbstractFAPICIBAID1 testModule) {
		TestRunnerSupport runner = mock(TestRunnerSupport.class);
		when(runner.getRunningTestById("ping-callback")).thenReturn(testModule);
		TestExecutionManager manager = new TestExecutionManager("ping-callback",
			completion, mock(AuthenticationFacade.class), runner);
		testModule.setProperties("ping-callback", Map.of(), mock(TestInstanceEventLog.class),
			mock(BrowserControl.class), mock(TestInfoService.class), manager, mock(ImageService.class));
	}

	@Test
	public void duplicateWhileTokenRequestIsPendingDoesNotRepeatProcessing() throws Exception {
		module.duringTokenRequest = () -> {
			module.handlePingCallback(callback("first"));
			assertThatCode(() -> runNextTask().get()).doesNotThrowAnyException();
		};

		module.handlePingCallback(callback("first"));
		runNextTask().get();
		module.handlePingCallback(callback("first"));
		runNextTask().get();

		assertThat(module.tokenRequests).isOne();
		assertThat(module.completedResponses).isOne();
		module.checkLockReleased();
	}

	@Test
	public void freshAuthReqIdCanBeProcessedInTheSameModule() {
		module.receive(callback("first"));
		module.setAuthReqId("second");
		module.receive(callback("second"));
		module.receive(callback("second"));

		assertThat(module.tokenRequests).isEqualTo(2);
		assertThat(module.completedResponses).isEqualTo(2);
	}

	@ParameterizedTest
	@ValueSource(strings = {"bearer", "auth_req_id", "body", "extra-field", "content-type"})
	public void duplicateStillUndergoesCallbackValidation(String invalidPart) {
		module.receive(callback("first"));
		JsonObject duplicate = callback("first");
		switch (invalidPart) {
			case "bearer" -> duplicate.getAsJsonObject("headers").addProperty("authorization", "Bearer wrong");
			case "auth_req_id" -> duplicate.getAsJsonObject("body_json").addProperty("auth_req_id", "wrong");
			case "body" -> duplicate.remove("body_json");
			case "extra-field" -> duplicate.getAsJsonObject("body_json").addProperty("unexpected", true);
			case "content-type" -> duplicate.getAsJsonObject("headers").addProperty("content-type", "text/plain");
			default -> throw new IllegalArgumentException(invalidPart);
		}

		assertThatThrownBy(() -> module.receive(duplicate)).isInstanceOf(TestFailureException.class);
		assertThat(module.tokenRequests).isOne();
		assertThat(module.completedResponses).isOne();
	}

	@Test
	public void invalidCallbackDoesNotClaimTheAuthReqId() {
		JsonObject invalid = callback("first");
		invalid.getAsJsonObject("headers").addProperty("authorization", "Bearer wrong");

		assertThatThrownBy(() -> module.receive(invalid)).isInstanceOf(TestFailureException.class);
		module.receive(callback("first"));

		assertThat(module.tokenRequests).isOne();
		assertThat(module.completedResponses).isOne();
	}

	@Test
	public void firstTokenErrorRemainsAFailureDespiteDuplicatePing() {
		module.tokenStatus = 400;
		module.duringTokenRequest = () -> module.receive(callback("first"));

		assertThatThrownBy(() -> module.receive(callback("first")))
			.isInstanceOf(TestFailureException.class)
			.hasMessageContaining("Invalid http status");
		module.receive(callback("first"));

		assertThat(module.tokenRequests).isOne();
		assertThat(module.completedResponses).isZero();
	}

	@Test
	public void callbackDoesNotResumeInterruptedTest() throws Exception {
		module.stop("Stopped before callback worker resumes");
		module.scheduleNotificationCallback(callback("first"), 0);

		runNextTask().get();

		assertThat(module.getStatus()).isEqualTo(TestModule.Status.INTERRUPTED);
		assertThat(module.tokenRequests).isZero();
		module.checkLockReleased();
	}

	@Test
	public void delayed401CallbackDoesNotResumeInterruptedTest() throws Exception {
		NoRetryModule noRetryModule = new NoRetryModule();
		initializeModule(noRetryModule);
		// Stop before the new future is tracked, so cancellation cannot hide a missing status guard.
		afterTaskQueued = () -> noRetryModule.stop("Stopped while callback was being queued");
		noRetryModule.receive(callback("first"));

		runNextTask().get();

		assertThat(noRetryModule.getStatus()).isEqualTo(TestModule.Status.INTERRUPTED);
		noRetryModule.checkLockReleased();
	}

	@Test
	public void callbackSubmissionAfterFinalisationIsIgnored() throws Exception {
		module.finish();
		runNextTask();
		assertThat(module.getStatus()).isEqualTo(TestModule.Status.FINISHED);

		module.scheduleNotificationCallback(callback("first"), 0);

		assertThat(tasks).isEmpty();
		assertThat(module.tokenRequests).isZero();
		module.checkLockReleased();
	}

	private static JsonObject callback(String authReqId) {
		JsonObject request = JsonParser.parseString("""
			{"headers":{"authorization":"Bearer notification-token",
			"content-type":"application/json","x-ssl-protocol":"TLSv1.3"},"body_json":{}}
			""").getAsJsonObject();
		request.getAsJsonObject("body_json").addProperty("auth_req_id", authReqId);
		return request;
	}

	private Future<Object> runNextTask() throws InterruptedException {
		Thread worker = Thread.ofPlatform().daemon().start(tasks.remove());
		worker.join(10000);
		assertThat(worker.isAlive()).isFalse();
		return completion.take();
	}

	private static class NoRetryModule extends FAPICIBAID1PingNotificationEndpointReturns401AndRequireServerDoesNotRetry {
		@Override
		public String getName() {
			return "ping-no-retry";
		}

		private void receive(JsonObject request) {
			setStatus(Status.WAITING);
			handlePingCallback(request);
		}

		@Override
		public void cleanup() {
		}
	}

	private static class CallbackModule extends AbstractFAPICIBAID1 {
		private int tokenRequests;
		private int completedResponses;
		private int tokenStatus = 200;
		private Runnable duringTokenRequest;

		@Override
		public String getName() {
			return "ping-callback";
		}

		private void setAuthReqId(String authReqId) {
			JsonObject response = new JsonObject();
			response.addProperty("auth_req_id", authReqId);
			env.putObject("backchannel_authentication_endpoint_response", response);
		}

		private void receive(JsonObject request) {
			setStatus(Status.RUNNING);
			try {
				processNotificationCallback(request);
			} finally {
				if (env.getLock().isHeldByCurrentThread()) {
					setStatus(Status.WAITING);
				}
			}
		}

		@Override
		protected void callTokenEndpointForCibaGrant() {
			tokenRequests++;
			Runnable callback = duringTokenRequest;
			duringTokenRequest = null;
			if (callback != null) {
				// Model the interceptor releasing the test lock while the token response is pending.
				setStatus(Status.WAITING);
				callback.run();
				setStatus(Status.RUNNING);
			}
			env.putInteger("token_endpoint_response_http_status", tokenStatus);
		}

		@Override
		protected void handleSuccessfulTokenEndpointResponse() {
			if (tokenStatus != 200) {
				super.handleSuccessfulTokenEndpointResponse();
			}
			completedResponses++;
			setStatus(Status.WAITING);
		}

		private void finish() {
			setStatus(Status.RUNNING);
			fireTestFinished();
		}

		private void waitForCallback() {
			setStatus(Status.WAITING);
		}

		@Override
		public void cleanup() {
		}
	}
}
