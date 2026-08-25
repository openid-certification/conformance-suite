package net.openid.conformance.condition.client;

import io.specto.hoverfly.junit.core.Hoverfly;
import io.specto.hoverfly.junit5.HoverflyExtension;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Base64;
import java.util.List;

import static io.specto.hoverfly.junit.core.SimulationSource.dsl;
import static io.specto.hoverfly.junit.dsl.HoverflyDsl.service;
import static io.specto.hoverfly.junit.dsl.ResponseCreators.badRequest;
import static io.specto.hoverfly.junit.dsl.ResponseCreators.success;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
@ExtendWith(HoverflyExtension.class)
public class CallVicalEndpoint_UnitTest {

	private CallVicalEndpoint cond;

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private Environment env;

	private static final byte[] VICAL_BYTES = VicalTestFixtures.goodSignedVical(
		List.of(VicalTestFixtures.generateSigner().getCert()));

	@BeforeEach
	public void setUp(Hoverfly hoverfly) {
		hoverfly.simulate(dsl(
			service("vical.example.com")
				.get("/vical.cbor")
				.willReturn(success()
					.header("Content-Type", "application/cbor")
					.body(Base64.getEncoder().encodeToString(VICAL_BYTES))
					.binaryEncoding()),
			service("missing.example.com")
				.get("/vical.cbor")
				.willReturn(badRequest())));
		hoverfly.resetJournal();

		cond = new CallVicalEndpoint();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
		env = new Environment();
	}

	@Test
	public void testEvaluate_fetchesVical() {
		env.putString("vical_url", "https://vical.example.com/vical.cbor");

		assertDoesNotThrow(() -> cond.execute(env));

		assertEquals(Base64.getEncoder().encodeToString(VICAL_BYTES), env.getString("vical", "value"));
		assertEquals("application/cbor", env.getString("vical_endpoint_response", "headers.content-type"));
	}

	@Test
	public void testEvaluate_failsOnHttpErrorButRecordsResponse() {
		env.putString("vical_url", "https://missing.example.com/vical.cbor");

		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("retrieve"), e.getMessage());
		// the response details are recorded even on error status
		assertEquals(400, env.getInteger("vical_endpoint_response", "status"));
	}
}
