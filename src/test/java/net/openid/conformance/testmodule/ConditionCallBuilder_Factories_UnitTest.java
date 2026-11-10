package net.openid.conformance.testmodule;

import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.common.CheckServerConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ConditionCallBuilder_Factories_UnitTest {

	@Test
	public void stopOnFailureStopsAndFails() {
		ConditionCallBuilder call = ConditionCallBuilder.stopOnFailure(CheckServerConfiguration.class);
		assertTrue(call.isStopOnFailure());
		assertEquals(ConditionResult.FAILURE, call.getOnFail());
		assertSame(CheckServerConfiguration.class, call.getConditionClass());
	}

	@Test
	public void continueOnFailureContinuesAndFails() {
		ConditionCallBuilder call = ConditionCallBuilder.continueOnFailure(CheckServerConfiguration.class);
		assertFalse(call.isStopOnFailure());
		assertEquals(ConditionResult.FAILURE, call.getOnFail());
	}

	@Test
	public void warnOnFailureContinuesAndWarns() {
		ConditionCallBuilder call = ConditionCallBuilder.warnOnFailure(CheckServerConfiguration.class)
			.requirements("REQ-1", "REQ-2");
		assertFalse(call.isStopOnFailure());
		assertEquals(ConditionResult.WARNING, call.getOnFail());
		assertArrayEquals(new String[] { "REQ-1", "REQ-2" }, call.getRequirements());
	}

	@Test
	public void instanceFactoriesKeepTheInstance() {
		CheckServerConfiguration condition = new CheckServerConfiguration();
		ConditionCallBuilder call = ConditionCallBuilder.warnOnFailure(condition);
		assertSame(condition, call.getCondition());
		assertSame(CheckServerConfiguration.class, call.getConditionClass());
		assertEquals(ConditionResult.WARNING, call.getOnFail());
	}

	@Test
	public void onFailOverridesTheFactoryDefault() {
		ConditionCallBuilder call = ConditionCallBuilder.continueOnFailure(CheckServerConfiguration.class)
			.onFail(ConditionResult.INFO);
		assertFalse(call.isStopOnFailure());
		assertEquals(ConditionResult.INFO, call.getOnFail());
	}
}
