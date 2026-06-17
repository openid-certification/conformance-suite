package net.openid.conformance.fapiciba.rp;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.as.EncryptIdToken;
import net.openid.conformance.condition.as.FAPIBrazilAddCPFAndCPNJToIdTokenClaims;
import net.openid.conformance.condition.as.FAPIBrazilSetRequiredIdTokenEncryptionConfig;
import net.openid.conformance.condition.as.FAPIEnsureClientJwksContainsAnEncryptionKey;
import net.openid.conformance.condition.as.GenerateIdTokenClaims;
import net.openid.conformance.condition.as.GenerateIdTokenClaimsWith181DayExp;
import net.openid.conformance.condition.as.SignIdToken;
import net.openid.conformance.condition.as.SignIdTokenWithX5tS256;
import net.openid.conformance.condition.rs.FAPIBrazilRsPathConstants;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.ConditionCallBuilder;
import net.openid.conformance.testmodule.TestExecutionUnit;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class OpenBankingBrazilCibaRPProfileBehavior_UnitTest {

	private final OpenBankingBrazilCibaRPProfileBehavior behavior = new OpenBankingBrazilCibaRPProfileBehavior();

	@Test
	public void claimsCustomerDataMtlsPaths() {
		assertThat(behavior.claimsProfileSpecificMtlsPath(FAPIBrazilRsPathConstants.BRAZIL_CONSENTS_PATH)).isTrue();
		assertThat(behavior.claimsProfileSpecificMtlsPath(FAPIBrazilRsPathConstants.BRAZIL_CONSENTS_PATH + "/consent-id")).isTrue();
		assertThat(behavior.claimsProfileSpecificMtlsPath(FAPIBrazilRsPathConstants.BRAZIL_RESOURCE_PATH)).isTrue();
	}

	@Test
	public void doesNotClaimPaymentMtlsPaths() {
		assertThat(behavior.claimsProfileSpecificMtlsPath(FAPIBrazilRsPathConstants.BRAZIL_PAYMENTS_CONSENTS_PATH)).isFalse();
		assertThat(behavior.claimsProfileSpecificMtlsPath(FAPIBrazilRsPathConstants.BRAZIL_PAYMENTS_CONSENTS_PATH + "/consent-id")).isFalse();
		assertThat(behavior.claimsProfileSpecificMtlsPath(FAPIBrazilRsPathConstants.BRAZIL_PAYMENT_INITIATION_PATH)).isFalse();
	}

	@Test
	public void usesNormalIdTokenExpiryWithBrazilClaims() {
		List<Class<? extends Condition>> conditionClasses = getConditionClasses(behavior.applyProfileSpecificIdTokenClaims());

		assertThat(conditionClasses)
			.containsExactly(GenerateIdTokenClaims.class, FAPIBrazilAddCPFAndCPNJToIdTokenClaims.class)
			.doesNotContain(GenerateIdTokenClaimsWith181DayExp.class);
	}

	@Test
	public void usesStandardIdTokenSigningCondition() {
		assertThat(behavior.getSignIdTokenCondition())
			.isEqualTo(SignIdToken.class)
			.isNotEqualTo(SignIdTokenWithX5tS256.class);
	}

	@Test
	public void validatesIdTokenEncryptionConfiguration() {
		List<Class<? extends Condition>> conditionClasses = getConditionClasses(behavior.applyProfileSpecificClientConfigurationValidation());

		assertThat(conditionClasses).containsExactly(
			FAPIBrazilSetRequiredIdTokenEncryptionConfig.class,
			FAPIEnsureClientJwksContainsAnEncryptionKey.class);
	}

	@Test
	public void encryptsIdToken() {
		List<Class<? extends Condition>> conditionClasses = getConditionClasses(behavior.applyProfileSpecificIdTokenEncryption());

		assertThat(conditionClasses).containsExactly(EncryptIdToken.class);
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
