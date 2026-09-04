package net.openid.conformance.fapiciba;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.EnsureOpenBankingBrazilCibaExpiresInDoesNotExceedMaximum;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.ConditionCallBuilder;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class FAPICIBAID1EnsureRequestedExpiryAboveMaximumIsCappedForBrazil_UnitTest {

	@Test
	public void directlyWiresMaximumExpiryAssertion() {
		TestableModule module = new TestableModule();

		module.performValidateAuthorizationResponse();

		assertThat(module.conditionClasses)
			.endsWith(EnsureOpenBankingBrazilCibaExpiresInDoesNotExceedMaximum.class);
		assertThat(module.conditionRequirements)
			.endsWith(List.of("BrazilCIBA-6.2.6"));
	}

	private static class TestableModule
		extends FAPICIBAID1EnsureRequestedExpiryAboveMaximumIsCappedForBrazil {

		private final List<Class<? extends Condition>> conditionClasses = new ArrayList<>();
		private final List<List<String>> conditionRequirements = new ArrayList<>();

		private TestableModule() {
			setupOpenBankingBrazil();
		}

		@Override
		protected void call(ConditionSequence sequence) {
			sequence.evaluate();
			sequence.getTestExecutionUnits().stream()
				.filter(ConditionCallBuilder.class::isInstance)
				.map(ConditionCallBuilder.class::cast)
				.forEach(builder -> recordCondition(
					builder.getConditionClass(), builder.getRequirements()));
		}

		@Override
		protected void callAndStopOnFailure(Class<? extends Condition> conditionClass,
			String... requirements) {
			recordCondition(conditionClass, requirements);
		}

		@Override
		protected void callAndContinueOnFailure(Class<? extends Condition> conditionClass,
			Condition.ConditionResult onFail, String... requirements) {
			recordCondition(conditionClass, requirements);
		}

		private void recordCondition(Class<? extends Condition> conditionClass,
			String... requirements) {
			conditionClasses.add(conditionClass);
			conditionRequirements.add(List.of(requirements));
		}
	}
}
