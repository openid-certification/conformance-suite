package net.openid.conformance.poppy.condition;

import io.specto.hoverfly.junit.core.Hoverfly;
import io.specto.hoverfly.junit5.HoverflyExtension;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.OAuthUriUtil;
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
public class FetchPoppyAuthorizationServerMetadata_UnitTest {

	private static final String PATH = "/.well-known/oauth-authorization-server";

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private FetchPoppyAuthorizationServerMetadata cond;

	@BeforeEach
	public void setUp(Hoverfly hoverfly) {
		hoverfly.simulate(dsl(
			service("auth.example.com").get(PATH)
				.willReturn(success(PoppyTestData.AS_METADATA, "application/json")),
			service("broken.example.com").get(PATH).willReturn(serverError()),
			service("html.example.com").get(PATH).willReturn(success("<html></html>", "text/html"))));
		hoverfly.resetJournal();
		cond = new FetchPoppyAuthorizationServerMetadata();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void urlInsertsPath() {
		String issuer = "https://as.example.com/test/a/x";
		assertThat(FetchPoppyAuthorizationServerMetadata.url(issuer))
			.hasToString(OAuthUriUtil.generateWellKnownUrlForPath(issuer, "oauth-authorization-server"))
			.hasToString("https://as.example.com/.well-known/oauth-authorization-server/test/a/x");
	}

	@Test
	public void fetchesMetadataIntoServer() {
		env.putString("poppy_issuer", "https://auth.example.com");
		cond.execute(env);
		assertThat(env.getString("server", "issuer")).isEqualTo("https://auth.example.com");
	}

	@Test
	public void serverErrorFails() {
		env.putString("poppy_issuer", "https://broken.example.com");
		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertThat(e.getMessage()).contains("HTTP 200");
	}

	@Test
	public void htmlFails() {
		env.putString("poppy_issuer", "https://html.example.com");
		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertThat(e.getMessage()).contains("Content-Type");
	}
}
