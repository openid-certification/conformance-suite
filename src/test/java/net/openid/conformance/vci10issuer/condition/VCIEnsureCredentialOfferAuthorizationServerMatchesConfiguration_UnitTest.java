package net.openid.conformance.vci10issuer.condition;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.Test;

import static net.openid.conformance.vci10issuer.condition.AbstractVCICredentialOfferAuthorizationServerCondition.AUTHORIZATION_CODE_GRANT;
import static net.openid.conformance.vci10issuer.condition.AbstractVCICredentialOfferAuthorizationServerCondition.PRE_AUTHORIZED_CODE_GRANT;
import static net.openid.conformance.vci10issuer.condition.VCICredentialOfferAuthorizationServerTestEnvironment.AS1;
import static net.openid.conformance.vci10issuer.condition.VCICredentialOfferAuthorizationServerTestEnvironment.AS2;
import static net.openid.conformance.vci10issuer.condition.VCICredentialOfferAuthorizationServerTestEnvironment.authCodeGrant;
import static net.openid.conformance.vci10issuer.condition.VCICredentialOfferAuthorizationServerTestEnvironment.env;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class VCIEnsureCredentialOfferAuthorizationServerMatchesConfiguration_UnitTest {

	private static final String TWO_SERVERS = "[\"" + AS1 + "\", \"" + AS2 + "\"]";

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private VCIEnsureCredentialOfferAuthorizationServerMatchesConfiguration cond(String grantType) {
		VCIEnsureCredentialOfferAuthorizationServerMatchesConfiguration cond =
			new VCIEnsureCredentialOfferAuthorizationServerMatchesConfiguration(grantType);
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
		return cond;
	}

	@Test
	public void noConfiguredAuthorizationServerPasses() {
		Environment env = env(TWO_SERVERS, authCodeGrant(AS2));

		assertThatCode(() -> cond(AUTHORIZATION_CODE_GRANT).execute(env)).doesNotThrowAnyException();
	}

	@Test
	public void anEmptiedFieldCountsAsNotConfigured() {
		Environment env = env(TWO_SERVERS, authCodeGrant(AS2));
		env.putString("config", "vci.authorization_server", "");

		assertThatCode(() -> cond(AUTHORIZATION_CODE_GRANT).execute(env)).doesNotThrowAnyException();
	}

	@Test
	public void theConfiguredAuthorizationServerPasses() {
		Environment env = env(TWO_SERVERS, authCodeGrant(AS2));
		env.putString("config", "vci.authorization_server", AS2);

		assertThatCode(() -> cond(AUTHORIZATION_CODE_GRANT).execute(env)).doesNotThrowAnyException();
	}

	@Test
	public void aDifferentAuthorizationServerFailsNamingTheField() {
		Environment env = env(TWO_SERVERS, authCodeGrant(AS2));
		env.putString("config", "vci.authorization_server", AS1);

		assertThatThrownBy(() -> cond(AUTHORIZATION_CODE_GRANT).execute(env))
			.isInstanceOf(ConditionError.class)
			.hasMessageContaining("'Authorization Server URL Override' in the test configuration");
	}

	@Test
	public void theHintOfAnotherGrantIsIgnored() {
		Environment env = env(TWO_SERVERS, authCodeGrant(AS2));
		env.putString("config", "vci.authorization_server", AS1);

		assertThatCode(() -> cond(PRE_AUTHORIZED_CODE_GRANT).execute(env)).doesNotThrowAnyException();
	}

	@Test
	public void withASingleAuthorizationServerTheHintIsNotUsed() {
		Environment env = env("[\"" + AS1 + "\"]", authCodeGrant(AS2));
		env.putString("config", "vci.authorization_server", AS1);

		assertThatCode(() -> cond(AUTHORIZATION_CODE_GRANT).execute(env)).doesNotThrowAnyException();
	}
}
