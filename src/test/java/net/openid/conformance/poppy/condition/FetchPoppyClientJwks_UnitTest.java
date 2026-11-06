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
public class FetchPoppyClientJwks_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private FetchPoppyClientJwks cond;

	@BeforeEach
	public void setUp(Hoverfly hoverfly) {
		hoverfly.simulate(dsl(
			service("agent.example")
				.get("/jwks.json")
				.willReturn(success(PoppyTestData.PUBLIC_JWKS, "application/json")),
			service("jwkset.example")
				.get("/jwks.json")
				.willReturn(success(PoppyTestData.PUBLIC_JWKS, "application/jwk-set+json")),
			service("redirect.example")
				.get("/jwks.json")
				.willReturn(HoverflyDsl.response().status(302).header("Location", "https://agent.example/jwks.json")),
			service("missing.example")
				.get("/jwks.json")
				.willReturn(HoverflyDsl.response().status(404)),
			service("broken.example")
				.get("/jwks.json")
				.willReturn(serverError()),
			service("html.example")
				.get("/jwks.json")
				.willReturn(success("<html></html>", "text/html")),
			service("notaset.example")
				.get("/jwks.json")
				.willReturn(success("{\"foo\": 1}", "application/json")),
			service("badkeys.example")
				.get("/jwks.json")
				.willReturn(success("{\"keys\": [1]}", "application/json"))));
		hoverfly.resetJournal();

		cond = new FetchPoppyClientJwks();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	private void run(String host) {
		env.putObject("client", PoppyTestData.json("{\"jwks_uri\": \"https://" + host + "/jwks.json\"}"));
		cond.execute(env);
	}

	private void fails(String host) {
		assertThrows(ConditionError.class, () -> run(host));
	}

	@Test
	public void jsonStoredAsJwks() {
		run("agent.example");
		assertThat(env.getElementFromObject("client", "jwks.keys").getAsJsonArray()).hasSize(1);
	}

	@Test
	public void jwkSetContentTypeAccepted() {
		run("jwkset.example");
		assertThat(env.getElementFromObject("client", "jwks.keys").getAsJsonArray()).hasSize(1);
	}

	@Test
	public void redirectRejected() {
		fails("redirect.example");
	}

	@Test
	public void notFoundRejected() {
		fails("missing.example");
	}

	@Test
	public void serverErrorRejected() {
		fails("broken.example");
	}

	@Test
	public void htmlRejected() {
		fails("html.example");
	}

	@Test
	public void notAJwkSetRejected() {
		fails("notaset.example");
	}

	@Test
	public void keysNotObjectsRejected() {
		fails("badkeys.example");
	}

	@Test
	public void missingJwksUriFails() {
		env.putObject("client", new com.google.gson.JsonObject());
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}
}
