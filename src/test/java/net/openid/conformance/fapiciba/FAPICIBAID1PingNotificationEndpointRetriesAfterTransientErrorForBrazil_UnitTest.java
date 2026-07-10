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
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

public class FAPICIBAID1PingNotificationEndpointRetriesAfterTransientErrorForBrazil_UnitTest {

	private TestExecutionManager executionManager;
	private TestablePingRetryModule module;

	@BeforeEach
	public void setUp() {
		executionManager = mock(TestExecutionManager.class);
		doAnswer(invocation -> {
			Callable<?> task = invocation.getArgument(0);
			task.call();
			return null;
		}).when(executionManager).runInBackground(any());
		module = new TestablePingRetryModule(executionManager);
	}

	@Test
	public void returnsTransientFailureOnceAndProcessesTheRetry() {
		ResponseEntity<?> firstResponse = asResponse(module.handlePingCallback(new JsonObject()));

		assertThat(firstResponse.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
		assertThat(module.verifiedCallbacks).isEqualTo(1);
		assertThat(module.processedCallbacks).isZero();

		ResponseEntity<?> secondResponse = asResponse(module.handlePingCallback(new JsonObject()));

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
	public void acknowledgesAdditionalRetriesWithoutProcessingTheFlowAgain() {
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
		assertThat(delayedModule.processedCallbacks).isZero();

		assertThatCode(backgroundTask.get()::call).doesNotThrowAnyException();
		assertThat(delayedModule.getEnv().getInteger("notification_endpoint_call_count")).isEqualTo(3);
		assertThat(delayedModule.processedCallbacks).isEqualTo(1);
		assertThat(delayedModule.successfulResponses).isEqualTo(1);
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
		private int verifiedCallbacks;
		private int processedCallbacks;
		private int successfulResponses;

		private TestablePingRetryModule(TestExecutionManager executionManager) {
			this.executionManager = executionManager;
			testType = CIBAMode.PING;
		}

		@Override
		public TestExecutionManager getTestExecutionManager() {
			return executionManager;
		}

		@Override
		protected void setStatus(TestModule.Status newStatus) {
			statuses.add(newStatus);
		}

		@Override
		protected void verifyNotificationCallback(JsonObject requestParts) {
			verifiedCallbacks++;
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
	}
}
