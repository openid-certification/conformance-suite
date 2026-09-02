package net.openid.conformance.fapiciba.rp;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.runner.TestExecutionManager;
import net.openid.conformance.testmodule.Command;
import net.openid.conformance.testmodule.TestFailureException;
import net.openid.conformance.testmodule.TestModule.Status;
import net.openid.conformance.variant.CIBAMode;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

public class AbstractFAPICIBAClientTest_UnitTest {

	@Test
	public void pingModeRetainsPollingFallbackForGenericProfiles() {
		TestableFAPICIBAClientTest test = new TestableFAPICIBAClientTest();
		test.setCibaMode(CIBAMode.PING);

		assertThat(test.shouldIssueFinalTokenResponse(2)).isFalse();
		assertThat(test.shouldIssueFinalTokenResponse(3)).isTrue();
	}

	@Test
	public void pingModeIssuesFinalResponseAfterSuccessfulNotification() {
		TestableFAPICIBAClientTest test = new TestableFAPICIBAClientTest();
		test.setCibaMode(CIBAMode.PING);
		test.getEnv().putBoolean("client_was_pinged", true);

		assertThat(test.shouldIssueFinalTokenResponse(1)).isTrue();
	}

	@Test
	public void pingModeIssuesFinalResponseDuringBrazilNotificationAttempt() {
		TestableFAPICIBAClientTest test = new TestableFAPICIBAClientTest();
		test.setCibaMode(CIBAMode.PING);
		test.getEnv().putBoolean(PingClientNotificationEndpoint.CLIENT_PING_ATTEMPTED, true);

		assertThat(test.shouldIssueFinalTokenResponse(1)).isTrue();
	}

	@Test
	public void defersResourceEndpointCompletionUntilPingResponseIsValidated() {
		TestableFAPICIBAClientTest test = new TestableFAPICIBAClientTest();
		test.setCibaMode(CIBAMode.PING);
		test.getEnv().putBoolean(PingClientNotificationEndpoint.CLIENT_PING_ATTEMPTED, true);

		test.resourceEndpointCallComplete();

		assertThat(test.fireTestFinishedCalled).isFalse();
		assertThat(test.lastStatus).isEqualTo(Status.WAITING);
		assertThat(test.getEnv().getBoolean("resource_endpoint_completion_pending_after_ping_response_validation")).isTrue();
	}

	@Test
	public void finishesResourceEndpointCompletionWhenPingResponseWasAlreadyValidated() {
		TestableFAPICIBAClientTest test = new TestableFAPICIBAClientTest();
		test.setCibaMode(CIBAMode.PING);
		test.getEnv().putBoolean(PingClientNotificationEndpoint.CLIENT_PING_ATTEMPTED, true);
		test.getEnv().putBoolean("client_ping_response_validated", true);

		test.resourceEndpointCallComplete();

		assertThat(test.fireTestFinishedCalled).isTrue();
		assertThat(test.lastStatus).isNull();
		assertThat(test.getEnv().getBoolean("resource_endpoint_completion_pending_after_ping_response_validation")).isNull();
	}

	@Test
	public void completesPendingResourceEndpointAfterPingResponseValidation() {
		TestableFAPICIBAClientTest test = new TestableFAPICIBAClientTest();
		test.getEnv().putBoolean("resource_endpoint_completion_pending_after_ping_response_validation", true);

		test.markPingResponseValidatedAndFinishPendingResourceEndpoint();

		assertThat(test.getEnv().getBoolean("client_ping_response_validated")).isTrue();
		assertThat(test.fireTestFinishedCalled).isTrue();
		assertThat(test.lastStatus).isNull();
	}

	@Test
	public void waitsAfterPingResponseValidationWhenResourceEndpointIsNotComplete() {
		TestableFAPICIBAClientTest test = new TestableFAPICIBAClientTest();

		test.markPingResponseValidatedAndFinishPendingResourceEndpoint();

		assertThat(test.getEnv().getBoolean("client_ping_response_validated")).isTrue();
		assertThat(test.fireTestFinishedCalled).isFalse();
		assertThat(test.lastStatus).isEqualTo(Status.WAITING);
	}

	@Test
	public void finishesImmediatelyWhenPingHasNotBeenSent() {
		TestableFAPICIBAClientTest test = new TestableFAPICIBAClientTest();
		test.setCibaMode(CIBAMode.PING);

		test.resourceEndpointCallComplete();

		assertThat(test.fireTestFinishedCalled).isTrue();
		assertThat(test.lastStatus).isNull();
	}

	@Test
	public void sendsPingNotificationByDefault() {
		TestableFAPICIBAClientTest test = new TestableFAPICIBAClientTest();

		assertThat(test.pingNotificationShouldBeSent()).isTrue();
	}

	@Test
	public void rejectsGenericAccountsEndpointWhenProfileDisablesIt() {
		TestableFAPICIBAClientTest test = new TestableFAPICIBAClientTest();
		test.setProfileBehavior(new OpenBankingBrazilCibaRPProfileBehavior());

		assertThatThrownBy(() -> test.handleHttpMtls(AbstractFAPICIBAClientTest.ACCOUNTS_PATH, null, null, null, new JsonObject()))
			.isInstanceOf(TestFailureException.class)
			.hasMessageContaining("Got unexpected HTTP (using mtls) call to " + AbstractFAPICIBAClientTest.ACCOUNTS_PATH);
		assertThat(test.accountsEndpointCalled).isFalse();
	}

	@Test
	public void closesPingLogBlockWhenTerminalTestCannotResume() throws Exception {
		TestableFAPICIBAClientTest test = new TestableFAPICIBAClientTest();

		test.runPingTaskWithCompletionBlocked();

		assertThat(test.endedLogBlock).isTrue();
		assertThat(test.pingRequestCompleteCalled).isFalse();
	}

	@Test
	public void closesPingLogBlockWhenNotificationValidationFails() {
		TestableFAPICIBAClientTest test = new TestableFAPICIBAClientTest();
		test.failPingRequest = true;

		assertThatThrownBy(test::runPingTaskWithCompletionBlocked)
			.isInstanceOf(IllegalStateException.class);
		assertThat(test.endedLogBlock).isTrue();
		assertThat(test.pingRequestCompleteCalled).isFalse();
	}

	private static class TestableFAPICIBAClientTest extends AbstractFAPICIBAClientTest {

		private boolean fireTestFinishedCalled;
		private Status lastStatus;
		private boolean accountsEndpointCalled;
		private final TestExecutionManager executionManager = mock(TestExecutionManager.class);
		private final AtomicReference<Callable<?>> backgroundTask = new AtomicReference<>();
		private boolean endedLogBlock;
		private boolean pingRequestCompleteCalled;
		private boolean failPingRequest;

		private TestableFAPICIBAClientTest() {
			doAnswer(invocation -> {
				backgroundTask.set(invocation.getArgument(0));
				return null;
			}).when(executionManager).runInBackground(any());
		}

		private void setCibaMode(CIBAMode cibaMode) {
			this.cibaMode = cibaMode;
		}

		private void setProfileBehavior(FAPICIBARPProfileBehavior profileBehavior) {
			this.profileBehavior = profileBehavior;
			profileBehavior.setModule(this);
		}

		private boolean pingNotificationShouldBeSent() {
			return shouldSendPingNotification();
		}

		private boolean shouldIssueFinalTokenResponse(int tokenPollCount) {
			return shouldIssueFinalCibaTokenResponse(tokenPollCount);
		}

		private void runPingTaskWithCompletionBlocked() throws Exception {
			Method spawnThreadForPing = AbstractFAPICIBAClientTest.class
				.getDeclaredMethod("spawnThreadForPing");
			spawnThreadForPing.setAccessible(true);
			spawnThreadForPing.invoke(this);
			backgroundTask.get().call();
		}

		@Override
		public void fireTestFinished() {
			fireTestFinishedCalled = true;
		}

		@Override
		protected void setStatus(Status newStatus) {
			lastStatus = newStatus;
		}

		@Override
		protected void call(Command builder) {
			builder.getEnvCommands().forEach(command -> command.accept(getEnv()));
			endedLogBlock = endedLogBlock || builder.isEndBlock();
		}

		@Override
		public TestExecutionManager getTestExecutionManager() {
			return executionManager;
		}

		@Override
		protected void sendPingRequestAndVerifyResponse() {
			if (failPingRequest) {
				throw new IllegalStateException("Notification validation failed");
			}
		}

		@Override
		protected void waitBeforePing() {
			// Do not delay the background completion behavior under test.
		}

		@Override
		protected boolean setStatusRunningIfWaiting() {
			return false;
		}

		@Override
		protected void pingRequestComplete() {
			pingRequestCompleteCalled = true;
		}

		@Override
		protected void callAndContinueOnFailure(Class<? extends Condition> conditionClass, Condition.ConditionResult onFail, String... requirements) {
			// Not relevant to endpoint dispatch behavior in these unit tests.
		}

		@Override
		protected Object accountsEndpoint(String requestId) {
			accountsEndpointCalled = true;
			return "accounts";
		}
	}
}
