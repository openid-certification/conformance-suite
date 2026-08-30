package net.openid.conformance.fapiciba.rp;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.as.CheckForClientCertificate;
import net.openid.conformance.condition.as.EnsureClientCertificateMatches;
import net.openid.conformance.condition.as.ExtractClientCertificateFromRequestHeaders;
import net.openid.conformance.condition.as.dynregistration.FAPIBrazilRegisterClient;
import net.openid.conformance.condition.client.CheckIncomingContentTypeIsApplicationJson;
import net.openid.conformance.condition.rs.EnsureIncomingRequestMethodIsPost;
import net.openid.conformance.condition.rs.ExtractBearerAccessTokenFromHeader;
import net.openid.conformance.condition.rs.RequireBearerRegistrationAccessToken;
import net.openid.conformance.testmodule.Command;
import net.openid.conformance.testmodule.TestModule.Status;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

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

	private record ConditionCall(
		Class<? extends Condition> conditionClass,
		List<String> requirements
	) {
	}

	private static class TestableFAPICIBAClientBrazilDCRHappyPathTest
		extends FAPICIBAClientBrazilDCRHappyPathTest {

		private final List<ConditionCall> conditionCalls = new ArrayList<>();

		private List<String> requirementsFor(Class<? extends Condition> conditionClass) {
			return conditionCalls.stream()
				.filter(call -> call.conditionClass() == conditionClass)
				.findFirst()
				.orElseThrow()
				.requirements();
		}

		@Override
		protected void setStatus(Status newStatus) {
			// Status changes are not relevant to endpoint dispatch behavior in this test.
		}

		@Override
		public Status getStatus() {
			return Status.RUNNING;
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
