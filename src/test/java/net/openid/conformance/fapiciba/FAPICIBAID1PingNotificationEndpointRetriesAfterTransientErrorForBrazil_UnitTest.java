package net.openid.conformance.fapiciba;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.EnsureNotificationEndpointWasRetried;
import net.openid.conformance.runner.TestExecutionManager;
import net.openid.conformance.testmodule.TestModule;
import net.openid.conformance.variant.CIBAMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

public class FAPICIBAID1PingNotificationEndpointRetriesAfterTransientErrorForBrazil_UnitTest {

	private TestExecutionManager executionManager;
	private TestablePingRetryModule module;
	private AtomicReference<Callable<?>> retryTimeoutTask;
	private AtomicLong retryTimeoutSeconds;

	@BeforeEach
	public void setUp() {
		executionManager = mock(TestExecutionManager.class);
		retryTimeoutTask = new AtomicReference<>();
		retryTimeoutSeconds = new AtomicLong();
		doAnswer(invocation -> {
			Callable<?> task = invocation.getArgument(0);
			task.call();
			return null;
		}).when(executionManager).runInBackground(any());
		doAnswer(invocation -> {
			retryTimeoutTask.set(invocation.getArgument(0));
			retryTimeoutSeconds.set(invocation.getArgument(1));
			return null;
		}).when(executionManager).scheduleInBackground(any(), anyLong(), eq(TimeUnit.SECONDS));
		module = new TestablePingRetryModule(executionManager);
	}

	@Test
	public void invokesTargetedRetryAssertionWhenAuthenticationRequestExpires() throws Exception {
		module.getEnv().putObjectFromJsonString("backchannel_authentication_endpoint_response",
			"{\"expires_in\":300}");
		module.performValidateAuthorizationResponse();
		module.conditionClasses.clear();
		module.conditionRequirements.clear();

		ResponseEntity<?> firstResponse = asResponse(module.handlePingCallback(new JsonObject()));

		assertThat(firstResponse.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
		assertThat(retryTimeoutSeconds).hasValue(300);
		assertThat(retryTimeoutTask).doesNotHaveNullValue();

		retryTimeoutTask.get().call();

		assertThat(module.getEnv().getInteger("notification_endpoint_call_count")).isEqualTo(1);
		assertThat(module.conditionClasses).containsExactly(EnsureNotificationEndpointWasRetried.class);
		assertThat(module.conditionRequirements).containsExactly(List.of("BrazilCIBA-6.2.8"));
		assertThat(module.statuses).containsExactly(
			TestModule.Status.RUNNING,
			TestModule.Status.WAITING,
			TestModule.Status.RUNNING);
	}

	@Test
	public void returnsTransientFailureOnceAndProcessesTheRetry() throws Exception {
		module.getEnv().putObjectFromJsonString("backchannel_authentication_endpoint_response",
			"{\"expires_in\":300}");
		module.performValidateAuthorizationResponse();
		module.conditionClasses.clear();
		module.conditionRequirements.clear();

		ResponseEntity<?> firstResponse = asResponse(module.handlePingCallback(new JsonObject()));

		assertThat(firstResponse.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
		assertThat(module.verifiedCallbacks).isEqualTo(1);
		assertThat(module.processedCallbacks).isZero();

		ResponseEntity<?> secondResponse = asResponse(module.handlePingCallback(new JsonObject()));
		retryTimeoutTask.get().call();

		assertThat(secondResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
		assertThat(module.getEnv().getInteger("notification_endpoint_call_count")).isEqualTo(2);
		assertThat(module.verifiedCallbacks).isEqualTo(2);
		assertThat(module.processedCallbacks).isEqualTo(1);
		assertThat(module.successfulResponses).isEqualTo(1);
		assertThat(module.conditionClasses).containsExactly(EnsureNotificationEndpointWasRetried.class);
		assertThat(module.conditionRequirements).containsExactly(List.of("BrazilCIBA-6.2.8"));
		assertThat(module.statuses).containsExactly(TestModule.Status.RUNNING, TestModule.Status.WAITING, TestModule.Status.RUNNING);
	}

	@Test
	public void doesNotScheduleRetryAssertionWhenExpiresInIsMissing() {
		ResponseEntity<?> firstResponse = asResponse(module.handlePingCallback(new JsonObject()));

		assertThat(firstResponse.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
		assertThat(retryTimeoutTask).hasValue(null);
	}

	@Test
	public void validatesAdditionalRetriesWithoutProcessingTheFlowAgain() {
		AtomicReference<Callable<?>> backgroundTask = new AtomicReference<>();
		TestExecutionManager delayedExecutionManager = mock(TestExecutionManager.class);
		doAnswer(invocation -> {
			backgroundTask.set(invocation.getArgument(0));
			return null;
		}).when(delayedExecutionManager).runInBackground(any());
		TestablePingRetryModule delayedModule = new TestablePingRetryModule(delayedExecutionManager);

		delayedModule.handlePingCallback(new JsonObject());
		delayedModule.handlePingCallback(new JsonObject());

		ResponseEntity<?> thirdResponse = asResponse(delayedModule.handlePingCallback(new JsonObject()));

		assertThat(thirdResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
		assertThat(delayedModule.verifiedCallbacks).isEqualTo(2);
		assertThat(delayedModule.processedCallbacks).isZero();

		assertThatCode(backgroundTask.get()::call).doesNotThrowAnyException();
		assertThat(delayedModule.getEnv().getInteger("notification_endpoint_call_count")).isEqualTo(3);
		assertThat(delayedModule.processedCallbacks).isEqualTo(1);
		assertThat(delayedModule.successfulResponses).isEqualTo(1);
	}

	@Test
	public void serializesFastRetryWithFirstCallbackStateTransitions() throws Exception {
		module.blockFirstCallbackVerification = true;
		ExecutorService executor = Executors.newFixedThreadPool(2);
		try {
			Future<Object> firstCallback = executor.submit(() -> module.handlePingCallback(new JsonObject()));
			assertThat(module.firstCallbackVerificationStarted.await(1, TimeUnit.SECONDS)).isTrue();

			CountDownLatch retryStarted = new CountDownLatch(1);
			Future<Object> retry = executor.submit(() -> {
				retryStarted.countDown();
				return module.handlePingCallback(new JsonObject());
			});
			assertThat(retryStarted.await(1, TimeUnit.SECONDS)).isTrue();
			assertThatThrownBy(() -> retry.get(100, TimeUnit.MILLISECONDS))
				.isInstanceOf(TimeoutException.class);

			module.allowFirstCallbackVerification.countDown();
			assertThat(asResponse(firstCallback.get()).getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
			assertThat(asResponse(retry.get()).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
			assertThat(module.statuses).startsWith(TestModule.Status.RUNNING, TestModule.Status.WAITING);
		} finally {
			module.allowFirstCallbackVerification.countDown();
			executor.shutdownNow();
		}
	}

	private static ResponseEntity<?> asResponse(Object response) {
		assertThat(response).isInstanceOf(ResponseEntity.class);
		return (ResponseEntity<?>) response;
	}

	private static class TestablePingRetryModule extends FAPICIBAID1PingNotificationEndpointRetriesAfterTransientErrorForBrazil {

		private final TestExecutionManager executionManager;
		private final List<Class<? extends Condition>> conditionClasses = new ArrayList<>();
		private final List<List<String>> conditionRequirements = new ArrayList<>();
		private final List<TestModule.Status> statuses = new ArrayList<>();
		private TestModule.Status currentStatus = TestModule.Status.RUNNING;
		private int verifiedCallbacks;
		private int processedCallbacks;
		private int successfulResponses;
		private boolean blockFirstCallbackVerification;
		private final CountDownLatch firstCallbackVerificationStarted = new CountDownLatch(1);
		private final CountDownLatch allowFirstCallbackVerification = new CountDownLatch(1);

		private TestablePingRetryModule(TestExecutionManager executionManager) {
			this.executionManager = executionManager;
			testType = CIBAMode.PING;
		}

		@Override
		public TestExecutionManager getTestExecutionManager() {
			return executionManager;
		}

		@Override
		public TestModule.Status getStatus() {
			return currentStatus;
		}

		@Override
		protected void setStatus(TestModule.Status newStatus) {
			currentStatus = newStatus;
			statuses.add(newStatus);
		}

		@Override
		protected void verifyNotificationCallback(JsonObject requestParts) {
			verifiedCallbacks++;
			if (blockFirstCallbackVerification && verifiedCallbacks == 1) {
				firstCallbackVerificationStarted.countDown();
				try {
					assertThat(allowFirstCallbackVerification.await(1, TimeUnit.SECONDS)).isTrue();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					throw new AssertionError(e);
				}
			}
		}

		@Override
		protected void processPingNotificationCallback(JsonObject requestParts) {
			verifiedCallbacks++;
			processedCallbacks++;
		}

		@Override
		protected void handleSuccessfulTokenEndpointResponse() {
			successfulResponses++;
		}

		@Override
		protected void callAndStopOnFailure(Class<? extends Condition> conditionClass, String... requirements) {
			conditionClasses.add(conditionClass);
			conditionRequirements.add(List.of(requirements));
		}

		@Override
		protected void callAndContinueOnFailure(Class<? extends Condition> conditionClass,
			Condition.ConditionResult onFail, String... requirements) {
			conditionClasses.add(conditionClass);
			conditionRequirements.add(List.of(requirements));
		}
	}
}
