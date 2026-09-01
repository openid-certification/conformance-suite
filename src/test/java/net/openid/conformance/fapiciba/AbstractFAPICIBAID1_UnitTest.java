package net.openid.conformance.fapiciba;

import net.openid.conformance.logging.BsonEncoding;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class AbstractFAPICIBAID1_UnitTest {

	@Test
	public void authorizationFlowCanFinishAfterHandlingAnErrorResponse() {
		TestableModule module = new TestableModule();

		module.performAuthorizationFlow();

		assertThat(module.events).containsExactly(
			"pre", "create", "request", "handle-error", "finished");
	}

	private static class TestableModule extends AbstractFAPICIBAID1 {

		private final List<String> events = new ArrayList<>();

		TestableModule() {
			eventLog = BsonEncoding.testInstanceEventLog();
		}

		@Override
		protected void performPreAuthorizationSteps() {
			events.add("pre");
		}

		@Override
		protected void createAuthorizationRequest() {
			events.add("create");
		}

		@Override
		protected void performAuthorizationRequest() {
			events.add("request");
		}

		@Override
		protected boolean handleAuthorizationEndpointErrorResponse() {
			events.add("handle-error");
			return true;
		}

		@Override
		protected void performValidateAuthorizationResponse() {
			events.add("validate-success");
		}

		@Override
		protected void performPostAuthorizationResponse() {
			events.add("post");
		}

		@Override
		public void fireTestFinished() {
			events.add("finished");
		}
	}
}
