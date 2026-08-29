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
public class CallRicalEndpoint_UnitTest {

	private CallRicalEndpoint cond;

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private Environment env;

	private static final byte[] RICAL_BYTES = RicalTestFixtures.goodSignedRical(
		List.of(RicalTestFixtures.generateReaderPki().getCaCert()));

	@BeforeEach
	public void setUp(Hoverfly hoverfly) {
		hoverfly.simulate(dsl(
			service("rical.example.com")
				.get("/rical.cbor")
				.willReturn(success()
					.header("Content-Type", "application/cbor")
					.body(Base64.getEncoder().encodeToString(RICAL_BYTES))
					.binaryEncoding()),
			service("missing.example.com")
				.get("/rical.cbor")
				.willReturn(badRequest())));
		hoverfly.resetJournal();

		cond = new CallRicalEndpoint();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
		env = new Environment();
	}

	@Test
	public void testEvaluate_fetchesRical() {
		env.putString("rical_url", "https://rical.example.com/rical.cbor");

		assertDoesNotThrow(() -> cond.execute(env));

		assertEquals(Base64.getEncoder().encodeToString(RICAL_BYTES), env.getString("rical", "value"));
		assertEquals("application/cbor", env.getString("rical_endpoint_response", "headers.content-type"));
	}

	@Test
	public void testEvaluate_failsOnHttpErrorButRecordsResponse() {
		env.putString("rical_url", "https://missing.example.com/rical.cbor");

		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("retrieve"), e.getMessage());
		// the response details are recorded even on error status
		assertEquals(400, env.getInteger("rical_endpoint_response", "status"));
	}
}
