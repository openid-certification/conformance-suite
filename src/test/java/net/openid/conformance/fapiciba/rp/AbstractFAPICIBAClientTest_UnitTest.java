package net.openid.conformance.fapiciba.rp;

import net.openid.conformance.testmodule.TestModule.Status;
import net.openid.conformance.variant.CIBAMode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class AbstractFAPICIBAClientTest_UnitTest {

	@Test
	public void defersResourceEndpointCompletionUntilPingResponseIsValidated() {
		TestableFAPICIBAClientTest test = new TestableFAPICIBAClientTest();
		test.setCibaMode(CIBAMode.PING);
		test.getEnv().putBoolean("client_was_pinged", true);

		test.resourceEndpointCallComplete();

		assertThat(test.fireTestFinishedCalled).isFalse();
		assertThat(test.lastStatus).isEqualTo(Status.WAITING);
		assertThat(test.getEnv().getBoolean("resource_endpoint_completion_pending_after_ping_response_validation")).isTrue();
	}

	@Test
	public void finishesResourceEndpointCompletionWhenPingResponseWasAlreadyValidated() {
		TestableFAPICIBAClientTest test = new TestableFAPICIBAClientTest();
		test.setCibaMode(CIBAMode.PING);
		test.getEnv().putBoolean("client_was_pinged", true);
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

	private static class TestableFAPICIBAClientTest extends AbstractFAPICIBAClientTest {

		private boolean fireTestFinishedCalled;
		private Status lastStatus;

		private void setCibaMode(CIBAMode cibaMode) {
			this.cibaMode = cibaMode;
		}

		@Override
		public void fireTestFinished() {
			fireTestFinishedCalled = true;
		}

		@Override
		protected void setStatus(Status newStatus) {
			lastStatus = newStatus;
		}
	}
}
