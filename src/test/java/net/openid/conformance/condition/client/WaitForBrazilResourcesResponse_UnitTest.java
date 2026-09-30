package net.openid.conformance.condition.client;

import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class WaitForBrazilResourcesResponse_UnitTest {

	@ParameterizedTest
	@CsvSource({"0,1", "1,2", "2,4", "3,8", "4,16", "5,30", "20,30"})
	public void usesExponentialBackoffWithACap(int attempt, long expected) {
		var condition = new WaitForBrazilResourcesResponse(System.nanoTime() + TimeUnit.MINUTES.toNanos(5), attempt);
		assertThat(condition.getExpectedWaitSeconds(new Environment())).isEqualTo(expected);
	}

	@Test
	public void doesNotWaitPastRemainingBudget() {
		var condition = new WaitForBrazilResourcesResponse(System.nanoTime() + TimeUnit.SECONDS.toNanos(10), 5);
		assertThat(condition.getExpectedWaitSeconds(new Environment())).isBetween(1L, 10L);
	}

	@Test
	public void failsWhenThePollingBudgetHasExpired() {
		var condition = new WaitForBrazilResourcesResponse(System.nanoTime() - 1, 0);
		condition.setProperties("UNIT-TEST", BsonEncoding.testInstanceEventLog(), ConditionResult.FAILURE);
		assertThrows(ConditionError.class, () -> condition.execute(new Environment()));
	}
}
