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

import java.net.URI;
import java.util.List;

import static io.specto.hoverfly.junit.core.SimulationSource.dsl;
import static io.specto.hoverfly.junit.dsl.HoverflyDsl.service;
import static io.specto.hoverfly.junit.dsl.ResponseCreators.serverError;
import static io.specto.hoverfly.junit.dsl.ResponseCreators.success;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(HoverflyExtension.class)
@ExtendWith(MockitoExtension.class)
public class FetchPoppyJson_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private FetchPoppyJson cond;

	@BeforeEach
	public void setUp(Hoverfly hoverfly) {
		hoverfly.simulate(dsl(
			service("example.com")
				.get("/.well-known/poppy.json")
				.willReturn(success(PoppyTestData.POPPY_JSON, "application/json")),
			service("www.example.com")
				.get("/.well-known/poppy.json")
				.willReturn(HoverflyDsl.response().status(302).header("Location", "https://example.com/.well-known/poppy.json")),
			service("broken.example.com")
				.get("/.well-known/poppy.json")
				.willReturn(serverError()),
			service("html.example.com")
				.get("/.well-known/poppy.json")
				.willReturn(success("<html></html>", "text/html")),
			service("insecure.example.com")
				.get("/.well-known/poppy.json")
				.willReturn(HoverflyDsl.response().status(302).header("Location", "http://example.com/.well-known/poppy.json")),
			service("wrongpath.example.com")
				.get("/.well-known/poppy.json")
				.willReturn(HoverflyDsl.response().status(302).header("Location", "https://example.com/other.json")),
			service("loop.example.com")
				.get("/.well-known/poppy.json")
				.willReturn(HoverflyDsl.response().status(302).header("Location", "https://loop.example.com/.well-known/poppy.json"))));
		hoverfly.resetJournal();

		cond = new FetchPoppyJson();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void startUrl() {
		assertThat(FetchPoppyJson.startUrl("example.com")).hasToString("https://example.com/.well-known/poppy.json");
	}

	@Test
	public void checkHopsAcceptsWwwToApex() {
		assertDoesNotThrow(() -> FetchPoppyJson.checkHops(List.of(
			URI.create("https://www.example.com/.well-known/poppy.json"),
			URI.create("https://example.com/.well-known/poppy.json"))));
	}

	@Test
	public void checkHopsRejectsHttpHop() {
		assertThrows(IllegalArgumentException.class, () -> FetchPoppyJson.checkHops(List.of(
			URI.create("http://www.example.com/.well-known/poppy.json"),
			URI.create("https://example.com/.well-known/poppy.json"))));
	}

	@Test
	public void checkHopsRejectsWrongPath() {
		assertThrows(IllegalArgumentException.class, () -> FetchPoppyJson.checkHops(List.of(
			URI.create("https://example.com/other.json"))));
	}

	@Test
	public void checkHopsRejectsQuery() {
		assertThrows(IllegalArgumentException.class, () -> FetchPoppyJson.checkHops(List.of(
			URI.create("https://example.com/.well-known/poppy.json?x=1"))));
	}

	@Test
	public void checkHopsRejectsExplicitPort() {
		assertThrows(IllegalArgumentException.class, () -> FetchPoppyJson.checkHops(List.of(
			URI.create("https://example.com:8443/.well-known/poppy.json"))));
	}

	@Test
	public void directFetch() {
		env.putString("poppy_domain", "example.com");
		cond.execute(env);
		assertThat(env.getString("poppy_json_final_url")).isEqualTo("https://example.com/.well-known/poppy.json");
		assertThat(env.getString("poppy_json", "protocol_version")).isEqualTo("0.4");
	}

	@Test
	public void wwwToApexRedirect() {
		env.putString("poppy_domain", "www.example.com");
		cond.execute(env);
		assertThat(env.getString("poppy_json_final_url")).isEqualTo("https://example.com/.well-known/poppy.json");
	}

	@Test
	public void serverErrorFails() {
		env.putString("poppy_domain", "broken.example.com");
		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertThat(e.getMessage()).contains("HTTP 200");
	}

	@Test
	public void htmlContentTypeFails() {
		env.putString("poppy_domain", "html.example.com");
		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertThat(e.getMessage()).contains("Content-Type");
	}

	@Test
	public void redirectToHttpFails() {
		env.putString("poppy_domain", "insecure.example.com");
		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertThat(e.getMessage()).contains("non-HTTPS");
	}

	@Test
	public void redirectToWrongPathFails() {
		env.putString("poppy_domain", "wrongpath.example.com");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void redirectLoopFails() {
		env.putString("poppy_domain", "loop.example.com");
		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertThat(e.getMessage()).contains("redirected more than");
	}
}
