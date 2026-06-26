package net.openid.conformance.fapiciba;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.FAPIBrazilValidateIdTokenEncryptedUsingRSAOAEPA256GCM;
import net.openid.conformance.condition.client.FAPIBrazilValidateIdTokenSigningAlg;
import net.openid.conformance.condition.client.ValidateIdTokenEncrypted;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.ConditionCallBuilder;
import net.openid.conformance.testmodule.TestExecutionUnit;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class OpenBankingBrazilCibaServerProfileBehavior_UnitTest {

	private final OpenBankingBrazilCibaServerProfileBehavior behavior = new OpenBankingBrazilCibaServerProfileBehavior();

	@Test
	public void validatesTokenEndpointIdTokenIsEncrypted() {
		List<Class<? extends Condition>> conditionClasses = getConditionClasses(
			behavior.validateTokenEndpointIdToken());

		assertThat(conditionClasses).containsExactly(
			ValidateIdTokenEncrypted.class,
			FAPIBrazilValidateIdTokenEncryptedUsingRSAOAEPA256GCM.class);
	}

	@Test
	public void usesStandardBrazilIdTokenValidationWithoutSpecialExpiry() throws ReflectiveOperationException {
		ConditionSequence sequence = behavior.getProfileIdTokenValidationSteps()
			.getDeclaredConstructor()
			.newInstance();

		List<Class<? extends Condition>> conditionClasses = getConditionClasses(sequence);

		assertThat(conditionClasses).containsExactly(FAPIBrazilValidateIdTokenSigningAlg.class);
	}

	@Test
	public void usesPingModeAsPrimaryMode() {
		assertThat(behavior.shouldCallTokenEndpointBeforePingNotification()).isFalse();
	}

	@Test
	public void plainFapiCanCallTokenEndpointBeforePingNotification() {
		assertThat(new FAPICIBAServerProfileBehavior().shouldCallTokenEndpointBeforePingNotification()).isTrue();
	}

	private List<Class<? extends Condition>> getConditionClasses(ConditionSequence sequence) {
		sequence.evaluate();
		return sequence.getTestExecutionUnits().stream()
			.map(this::getConditionClass)
			.toList();
	}

	private Class<? extends Condition> getConditionClass(TestExecutionUnit unit) {
		return ((ConditionCallBuilder) unit).getConditionClass();
	}

}
