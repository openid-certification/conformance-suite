package net.openid.conformance.fapiciba.rp;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.as.CheckForClientCertificate;
import net.openid.conformance.condition.as.EnsureClientCertificateMatches;
import net.openid.conformance.condition.as.ExtractClientCertificateFromRequestHeaders;
import net.openid.conformance.condition.as.FAPIBrazilSetRequiredIdTokenEncryptionConfig;
import net.openid.conformance.condition.as.dynregistration.FAPIBrazilRegisterClient;
import net.openid.conformance.condition.as.dynregistration.FAPIBrazilValidateIdTokenEncryptionConfig;
import net.openid.conformance.condition.client.CheckIncomingContentTypeIsApplicationJson;
import net.openid.conformance.condition.rs.EnsureIncomingRequestMethodIsPost;
import net.openid.conformance.condition.rs.ExtractBearerAccessTokenFromHeader;
import net.openid.conformance.condition.rs.RequireBearerRegistrationAccessToken;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.runner.TestExecutionManager;
import net.openid.conformance.testmodule.Command;
import net.openid.conformance.testmodule.TestModule.Status;
import net.openid.conformance.variant.CIBAMode;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

public class FAPICIBAClientBrazilDCRHappyPathTest_UnitTest {

	@Test
	public void mtlsRegistrationValidatesMethodAndContentTypeBeforeReadingTheRequest() {
		TestableFAPICIBAClientBrazilDCRHappyPathTest test =
			new TestableFAPICIBAClientBrazilDCRHappyPathTest();
		JsonObject requestParts = new JsonObject();
		requestParts.addProperty("method", "POST");

		assertThatCode(() -> test.handleHttpMtls("register", null, null, null, requestParts))
			.doesNotThrowAnyException();

		assertThat(test.conditionCalls)
			.extracting(ConditionCall::conditionClass)
			.containsSubsequence(EnsureIncomingRequestMethodIsPost.class, CheckIncomingContentTypeIsApplicationJson.class);
	}

	@Test
	public void dynamicRegistrationValidatesIdTokenEncryptionMetadataWithoutNormalizingIt() {
		TestableFAPICIBAClientBrazilDCRHappyPathTest test =
			new TestableFAPICIBAClientBrazilDCRHappyPathTest();
		JsonObject requestParts = new JsonObject();
		requestParts.addProperty("method", "POST");

		test.handleHttpMtls("register", null, null, null, requestParts);

		assertThat(test.conditionCalls)
			.extracting(ConditionCall::conditionClass)
			.contains(FAPIBrazilValidateIdTokenEncryptionConfig.class)
			.doesNotContain(FAPIBrazilSetRequiredIdTokenEncryptionConfig.class);
		assertThat(test.requirementsFor(FAPIBrazilValidateIdTokenEncryptionConfig.class))
			.containsExactly("BrazilOB22-5.1.1-1", "BrazilOB22-6.3", "BrazilCIBA-6.3.8");
	}

	@Test
	public void mtlsCleanupDeleteValidatesClientCertificateAndRegistrationAccessToken() {
		TestableFAPICIBAClientBrazilDCRHappyPathTest test =
			new TestableFAPICIBAClientBrazilDCRHappyPathTest();
		test.getEnv().putString("registration_client_uri", "path", "clienturi/test");
		JsonObject requestParts = new JsonObject();
		requestParts.addProperty("method", "DELETE");

		test.handleHttpMtls("clienturi/test", null, null, null, requestParts);

		assertThat(test.conditionCalls)
			.extracting(ConditionCall::conditionClass)
			.containsExactly(
				ExtractClientCertificateFromRequestHeaders.class,
				CheckForClientCertificate.class,
				EnsureClientCertificateMatches.class,
				ExtractBearerAccessTokenFromHeader.class,
				RequireBearerRegistrationAccessToken.class);
		assertThat(test.requirementsFor(ExtractBearerAccessTokenFromHeader.class))
			.containsExactly("RFC7592-2.3");
		assertThat(test.requirementsFor(RequireBearerRegistrationAccessToken.class))
			.containsExactly("RFC7592-2.3");
	}

	@Test
	public void resourceCompletionWaitsForRegistrationCleanupBeforeFinishing() throws Exception {
		TestableFAPICIBAClientBrazilDCRHappyPathTest test =
			new TestableFAPICIBAClientBrazilDCRHappyPathTest();

		test.completeResourceEndpointCall();

		assertThat(test.finishCount).isZero();
		assertThat(test.getStatus()).isEqualTo(Status.WAITING);
		assertThat(test.scheduledFinishTask).doesNotHaveNullValue();
		assertThat(test.scheduledFinishDelaySeconds).hasValue(20);

		test.runScheduledFinishTask();

		assertThat(test.finishCount).isEqualTo(1);
		assertThat(test.statuses).containsSubsequence(Status.WAITING, Status.RUNNING);
	}

	@Test
	public void authenticatedCleanupDuringGracePeriodFinishesExactlyOnce() throws Exception {
		TestableFAPICIBAClientBrazilDCRHappyPathTest test =
			new TestableFAPICIBAClientBrazilDCRHappyPathTest();
		test.getEnv().putString("registration_client_uri", "path", "clienturi/test");
		JsonObject requestParts = new JsonObject();
		requestParts.addProperty("method", "DELETE");
		test.completeResourceEndpointCall();

		test.handleHttpMtls("clienturi/test", null, null, null, requestParts);

		assertThat(test.finishCount).isEqualTo(1);
		assertThat(test.conditionCalls)
			.extracting(ConditionCall::conditionClass)
			.contains(ExtractBearerAccessTokenFromHeader.class, RequireBearerRegistrationAccessToken.class);

		test.runScheduledFinishTask();

		assertThat(test.finishCount).isEqualTo(1);
	}

	@Test
	public void authenticatedCleanupBeforeResourceCompletionFinishesWhenResourceCompletes() {
		TestableFAPICIBAClientBrazilDCRHappyPathTest test =
			new TestableFAPICIBAClientBrazilDCRHappyPathTest();
		test.getEnv().putString("registration_client_uri", "path", "clienturi/test");
		JsonObject requestParts = new JsonObject();
		requestParts.addProperty("method", "DELETE");

		test.handleHttpMtls("clienturi/test", null, null, null, requestParts);
		assertThat(test.finishCount).isZero();

		test.completeResourceEndpointCall();

		assertThat(test.finishCount).isEqualTo(1);
		assertThat(test.scheduledFinishTask.get()).isNull();
	}

	@Test
	public void plainHttpCleanupDuringGracePeriodFinishesWithoutWaitingForFallback() throws Exception {
		TestableFAPICIBAClientBrazilDCRHappyPathTest test =
			new TestableFAPICIBAClientBrazilDCRHappyPathTest();
		test.getEnv().putString("registration_client_uri", "path", "clienturi/test");
		JsonObject requestParts = new JsonObject();
		requestParts.addProperty("method", "DELETE");
		test.completeResourceEndpointCall();

		test.handleHttp("clienturi/test", null, null, null, requestParts);

		assertThat(test.finishCount).isEqualTo(1);

		test.runScheduledFinishTask();

		assertThat(test.finishCount).isEqualTo(1);
	}

	@Test
	public void resourceCompletionBeforePingValidationStartsGracePeriodAfterPingResponse() {
		TestableFAPICIBAClientBrazilDCRHappyPathTest test =
			new TestableFAPICIBAClientBrazilDCRHappyPathTest();
		test.getEnv().putBoolean(PingClientNotificationEndpoint.CLIENT_PING_ATTEMPTED, true);

		test.completeResourceEndpointCall();

		assertThat(test.finishCount).isZero();
		assertThat(test.scheduledFinishTask.get()).isNull();
		assertThat(test.getStatus()).isEqualTo(Status.WAITING);

		test.completePingResponseValidation();

		assertThat(test.finishCount).isZero();
		assertThat(test.scheduledFinishTask).doesNotHaveNullValue();
		assertThat(test.getStatus()).isEqualTo(Status.WAITING);
	}

	@Test
	public void gracePeriodTimerDoesNotDropCompletionWhileARequestIsRunning() throws Exception {
		TestableFAPICIBAClientBrazilDCRHappyPathTest test =
			new TestableFAPICIBAClientBrazilDCRHappyPathTest();
		test.completeResourceEndpointCall();
		test.beginRequest();

		test.runScheduledFinishTask();

		assertThat(test.finishCount).isEqualTo(1);
	}

	private record ConditionCall(
		Class<? extends Condition> conditionClass,
		List<String> requirements
	) {
	}

	private static class TestableFAPICIBAClientBrazilDCRHappyPathTest
		extends FAPICIBAClientBrazilDCRHappyPathTest {

		private final List<ConditionCall> conditionCalls = new ArrayList<>();
		private final TestExecutionManager executionManager = mock(TestExecutionManager.class);
		private final List<Status> statuses = new ArrayList<>();
		private final AtomicReference<Callable<?>> scheduledFinishTask = new AtomicReference<>();
		private final AtomicLong scheduledFinishDelaySeconds = new AtomicLong();
		private Status currentStatus = Status.RUNNING;
		private int finishCount;

		private TestableFAPICIBAClientBrazilDCRHappyPathTest() {
			cibaMode = CIBAMode.PING;
			eventLog = BsonEncoding.testInstanceEventLog();
			doAnswer(invocation -> {
				scheduledFinishTask.set(invocation.getArgument(0));
				scheduledFinishDelaySeconds.set(invocation.getArgument(1));
				return null;
			}).when(executionManager).scheduleInBackground(any(), anyLong(), eq(TimeUnit.SECONDS));
		}

		private void completeResourceEndpointCall() {
			resourceEndpointCallComplete();
		}

		private void completePingResponseValidation() {
			markPingResponseValidatedAndFinishPendingResourceEndpoint();
		}

		private void beginRequest() {
			setStatus(Status.RUNNING);
		}

		private void runScheduledFinishTask() throws Exception {
			scheduledFinishTask.get().call();
		}

		private List<String> requirementsFor(Class<? extends Condition> conditionClass) {
			return conditionCalls.stream()
				.filter(call -> call.conditionClass() == conditionClass)
				.findFirst()
				.orElseThrow()
				.requirements();
		}

		@Override
		protected void setStatus(Status newStatus) {
			currentStatus = newStatus;
			statuses.add(newStatus);
		}

		@Override
		public Status getStatus() {
			return currentStatus;
		}

		@Override
		public String getName() {
			return "UNIT-TEST";
		}

		@Override
		public TestExecutionManager getTestExecutionManager() {
			return executionManager;
		}

		@Override
		public void fireTestFinished() {
			finishCount++;
			currentStatus = Status.FINISHED;
		}

		@Override
		protected void validateClientJwks() {
			// Not relevant to registration endpoint dispatch behavior.
		}

		@Override
		protected void validateClientConfiguration() {
			// Not relevant to registration endpoint dispatch behavior.
		}

		@Override
		protected void call(Command builder) {
			builder.getEnvCommands().forEach(command -> command.accept(getEnv()));
		}

		@Override
		protected void callAndStopOnFailure(
			Class<? extends Condition> conditionClass,
			String... requirements
		) {
			conditionCalls.add(new ConditionCall(conditionClass, List.of(requirements)));
			if (FAPIBrazilRegisterClient.class.equals(conditionClass)) {
				getEnv().putObject("client", new JsonObject());
			}
		}

		@Override
		protected void callAndStopOnFailure(
			Class<? extends Condition> conditionClass,
			Condition.ConditionResult onFail,
			String... requirements
		) {
			conditionCalls.add(new ConditionCall(conditionClass, List.of(requirements)));
		}

		@Override
		protected void callAndContinueOnFailure(
			Class<? extends Condition> conditionClass,
			Condition.ConditionResult onFail,
			String... requirements
		) {
			conditionCalls.add(new ConditionCall(conditionClass, List.of(requirements)));
		}

		@Override
		protected void skipIfElementMissing(String objId, String path, Condition.ConditionResult onSkip,
			Class<? extends Condition> conditionClass, Condition.ConditionResult onFail, String... requirements) {
			conditionCalls.add(new ConditionCall(conditionClass, List.of(requirements)));
		}
	}
}
