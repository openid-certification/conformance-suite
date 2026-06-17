package net.openid.conformance.fapiciba.rp;

import net.openid.conformance.condition.rs.FAPIBrazilRsPathConstants;
import org.junit.jupiter.api.Test;

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
}
