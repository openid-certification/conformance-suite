package net.openid.conformance.poppy.condition;

import io.specto.hoverfly.junit.core.Hoverfly;
import io.specto.hoverfly.junit.dsl.HoverflyDsl;
import io.specto.hoverfly.junit5.HoverflyExtension;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static io.specto.hoverfly.junit.core.SimulationSource.dsl;
import static io.specto.hoverfly.junit.dsl.HoverflyDsl.service;
import static io.specto.hoverfly.junit.dsl.ResponseCreators.serverError;
import static io.specto.hoverfly.junit.dsl.ResponseCreators.success;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(HoverflyExtension.class)
@ExtendWith(MockitoExtension.class)
public class FetchPoppyClientMetadataDocument_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private FetchPoppyClientMetadataDocument cond;

	@BeforeEach
	public void setUp(Hoverfly hoverfly) {
		hoverfly.simulate(dsl(
			service("agent.example")
				.get("/agent.json")
				.willReturn(success(PoppyTestData.CLIENT_METADATA, "application/json")),
			service("redirect.example")
				.get("/agent.json")
				.willReturn(HoverflyDsl.response().status(302).header("Location", "https://agent.example/agent.json")),
			service("missing.example")
				.get("/agent.json")
				.willReturn(HoverflyDsl.response().status(404)),
			service("broken.example")
				.get("/agent.json")
				.willReturn(serverError()),
			service("html.example")
				.get("/agent.json")
				.willReturn(success("<html></html>", "text/html"))));
		hoverfly.resetJournal();

		cond = new FetchPoppyClientMetadataDocument();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	private ConditionError fail(String clientId) {
		env.putString("poppy_incoming_client_id", clientId);
		return assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void jsonResponseStoredAsClient() {
		env.putString("poppy_incoming_client_id", "https://agent.example/agent.json");
		cond.execute(env);
		assertThat(env.getString("client", "client_name")).isEqualTo("Example Agent");
	}

	@Test
	public void redirectRejected() {
		assertThat(fail("https://redirect.example/agent.json").getMessage()).contains("CIMD").contains("POPPY-5.3");
	}

	@Test
	public void httpClientIdRejectedBeforeFetch() {
		assertThat(fail("http://agent.example/agent.json").getMessage()).contains("HTTPS");
	}

	@Test
	public void notFoundRejected() {
		fail("https://missing.example/agent.json");
	}

	@Test
	public void serverErrorRejected() {
		fail("https://broken.example/agent.json");
	}

	@Test
	public void htmlRejected() {
		assertThat(fail("https://html.example/agent.json").getMessage()).contains("Content-Type");
	}
}
