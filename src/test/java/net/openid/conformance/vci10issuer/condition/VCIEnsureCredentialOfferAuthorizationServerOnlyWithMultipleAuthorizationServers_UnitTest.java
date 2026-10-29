package net.openid.conformance.vci10issuer.condition;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static net.openid.conformance.vci10issuer.condition.VCICredentialOfferAuthorizationServerTestEnvironment.AS1;
import static net.openid.conformance.vci10issuer.condition.VCICredentialOfferAuthorizationServerTestEnvironment.AS2;
import static net.openid.conformance.vci10issuer.condition.VCICredentialOfferAuthorizationServerTestEnvironment.ISSUER;
import static net.openid.conformance.vci10issuer.condition.VCICredentialOfferAuthorizationServerTestEnvironment.authCodeGrant;
import static net.openid.conformance.vci10issuer.condition.VCICredentialOfferAuthorizationServerTestEnvironment.env;
import static net.openid.conformance.vci10issuer.condition.VCICredentialOfferAuthorizationServerTestEnvironment.preAuthGrant;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class VCIEnsureCredentialOfferAuthorizationServerOnlyWithMultipleAuthorizationServers_UnitTest {

	private VCIEnsureCredentialOfferAuthorizationServerOnlyWithMultipleAuthorizationServers cond;

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	@BeforeEach
	public void setUp() {
		cond = new VCIEnsureCredentialOfferAuthorizationServerOnlyWithMultipleAuthorizationServers();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
	}

	@Test
	public void noAuthorizationServerInTheOfferPasses() {
		Environment env = env(null, "{\"authorization_code\": {}}");

		assertThatCode(() -> cond.execute(env)).doesNotThrowAnyException();
	}

	@Test
	public void withMultipleAuthorizationServersItPasses() {
		Environment env = env("[\"" + AS1 + "\", \"" + AS2 + "\"]", authCodeGrant(AS1));

		assertThatCode(() -> cond.execute(env)).doesNotThrowAnyException();
	}

	@Test
	public void withoutAuthorizationServersItFails() {
		Environment env = env(null, authCodeGrant(ISSUER));

		assertThatThrownBy(() -> cond.execute(env)).isInstanceOf(ConditionError.class);
	}

	@Test
	public void withASingleAuthorizationServerItFails() {
		Environment env = env("[\"" + AS1 + "\"]", preAuthGrant(AS1));

		assertThatThrownBy(() -> cond.execute(env)).isInstanceOf(ConditionError.class);
	}
}
