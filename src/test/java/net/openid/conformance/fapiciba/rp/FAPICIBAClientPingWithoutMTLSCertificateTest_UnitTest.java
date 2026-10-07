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
import net.openid.conformance.testmodule.TestModule;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.concurrent.ExecutorCompletionService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// The suite OP rejects the certless notification and stops, so its pairing cannot exercise a later poll.
public class FAPICIBAClientPingWithoutMTLSCertificateTest_UnitTest {

	@Test
	public void latePollReturnsTerminalErrorWithoutChangingPassedResult() throws Exception {
		TestableModule module = completedModule();
		for (int attempt = 0; attempt < 2; attempt++) {
			var response = (ResponseEntity<?>) module.handleHttpMtls("token", null, null, null, new JsonObject());
			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
			assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
			assertThat(response.getHeaders().getCacheControl()).isEqualTo("no-store");
			assertThat(response.getHeaders().getPragma()).isEqualTo("no-cache");
			assertThat(OIDFJSON.getString(((JsonObject) response.getBody()).get("error"))).isEqualTo("invalid_grant");
			assertThat(module.getStatus()).isEqualTo(TestModule.Status.FINISHED);
			assertThat(module.getResult()).isEqualTo(TestModule.Result.PASSED);
			module.checkLockReleased();
		}
	}

	@Test
	public void pollDispatchedBeforeFinalizationReturnsTerminalError() throws Exception {
		TestableModule module = completedModule();
		var response = (ResponseEntity<?>) module.tokenEndpoint("incoming_request");
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(OIDFJSON.getString(((JsonObject) response.getBody()).get("error"))).isEqualTo("invalid_grant");
		assertThat(module.getStatus()).isEqualTo(TestModule.Status.FINISHED);
		assertThat(module.getResult()).isEqualTo(TestModule.Result.PASSED);
		module.checkLockReleased();
	}

	@Test
	public void completesWithoutRequiringFallbackPolling() throws Exception {
		TestableModule module = completedModule();
		assertThat(module.getStatus()).isEqualTo(TestModule.Status.FINISHED);
		assertThat(module.getResult()).isEqualTo(TestModule.Result.PASSED);
	}

	private TestableModule completedModule() throws Exception {
		var module = new TestableModule();
		var support = mock(TestRunnerSupport.class);
		when(support.getRunningTestById("certless-ping-test")).thenReturn(module);
		var completion = new ExecutorCompletionService<Object>(Runnable::run);
		var manager = new TestExecutionManager("certless-ping-test", completion,
			mock(AuthenticationFacade.class), support);
		module.setProperties("certless-ping-test", Map.of(), mock(TestInstanceEventLog.class),
			mock(BrowserControl.class), mock(TestInfoService.class), manager, mock(ImageService.class));
		module.completePing();
		completion.take().get();
		return module;
	}

	private static class TestableModule extends FAPICIBAClientPingWithoutMTLSCertificateTest {
		@Override
		public String getName() {
			return "certless-ping-test";
		}

		void completePing() {
			setStatus(Status.CONFIGURED);
			setStatus(Status.RUNNING);
			pingRequestComplete();
		}
	}
}
