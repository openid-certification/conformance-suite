package net.openid.conformance.condition.client;

import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.MdocIacaCrl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class EnsureMdocDsCertificateCrlNextUpdateWithin90Days_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private EnsureMdocDsCertificateCrlNextUpdateWithin90Days cond;

	@BeforeEach
	public void setUp() {
		cond = new EnsureMdocDsCertificateCrlNextUpdateWithin90Days();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.FAILURE);
	}

	@Test
	public void passesForTheCrlTheSuiteServes() {
		MdocCrlTestFixtures.putCrl(env, MdocIacaCrl.generate());
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void passesForExactly90Days() throws Exception {
		Instant thisUpdate = Instant.now().minus(Duration.ofMinutes(1));
		MdocCrlTestFixtures.putCrl(env, MdocCrlTestFixtures.iacaCrl()
			.thisUpdate(thisUpdate).nextUpdate(thisUpdate.plus(Duration.ofDays(90))).build());
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void failsWhenNextUpdateIsMoreThan90DaysAfterThisUpdate() throws Exception {
		MdocCrlTestFixtures.putCrl(env, MdocCrlTestFixtures.iacaCrl().nextUpdate(Instant.now().plus(Duration.ofDays(91))).build());
		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("90 days"), e.getMessage());
	}

	@Test
	public void leavesAMissingNextUpdateToTheProfileCheck() throws Exception {
		MdocCrlTestFixtures.putCrl(env, MdocCrlTestFixtures.iacaCrl().nextUpdate(null).build());
		assertDoesNotThrow(() -> cond.execute(env));
	}
}
