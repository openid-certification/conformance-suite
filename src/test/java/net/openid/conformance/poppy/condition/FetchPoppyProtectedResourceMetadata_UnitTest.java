package net.openid.conformance.poppy.condition;

import io.specto.hoverfly.junit.core.Hoverfly;
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
import static io.specto.hoverfly.junit.dsl.ResponseCreators.notFound;
import static io.specto.hoverfly.junit.dsl.ResponseCreators.serverError;
import static io.specto.hoverfly.junit.dsl.ResponseCreators.success;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(HoverflyExtension.class)
@ExtendWith(MockitoExtension.class)
public class FetchPoppyProtectedResourceMetadata_UnitTest {

	private static final String PATH = "/.well-known/oauth-protected-resource";

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private FetchPoppyProtectedResourceMetadata cond;

	@BeforeEach
	public void setUp(Hoverfly hoverfly) {
		hoverfly.simulate(dsl(
			service("example.com").get(PATH)
				.willReturn(success("{\"resource\":\"https://example.com\",\"authorization_servers\":[\"https://auth.example.com\"]}", "application/json")),
			service("missing.example.com").get(PATH).willReturn(notFound()),
			service("broken.example.com").get(PATH).willReturn(serverError()),
			service("html.example.com").get(PATH).willReturn(success("<html></html>", "text/html"))));
		hoverfly.resetJournal();
		cond = new FetchPoppyProtectedResourceMetadata();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void derivedUrl() {
		assertThat(FetchPoppyProtectedResourceMetadata.url("example.com"))
			.hasToString("https://example.com/.well-known/oauth-protected-resource");
	}

	@Test
	public void fetchesMetadata() {
		env.putString("poppy_domain", "example.com");
		cond.execute(env);
		assertThat(env.getString("poppy_protected_resource_metadata", "resource")).isEqualTo("https://example.com");
	}

	@Test
	public void notFoundExplainsNoIssuer() {
		env.putString("poppy_domain", "missing.example.com");
		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertThat(e.getMessage()).contains("no issuer").contains("needs the Company to offer sign-in");
	}

	@Test
	public void serverErrorFails() {
		env.putString("poppy_domain", "broken.example.com");
		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertThat(e.getMessage()).contains("HTTP 200");
	}

	@Test
	public void htmlFails() {
		env.putString("poppy_domain", "html.example.com");
		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertThat(e.getMessage()).contains("Content-Type");
	}
}
