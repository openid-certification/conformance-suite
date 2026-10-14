package net.openid.conformance.condition.client.lote;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The lists the European Commission publishes in its development trust infrastructure for the
 * EUDI Wallet Launchpad 2026, fetched 2026-10-14 from
 * https://development.trust.tech.ec.europa.eu/lists/eudiw/: RS512, typ "jose", a JAdES x5t#o
 * instead of x5t#S256, the signer's issuing CA not in x5c, and an mDL list type of the
 * Commission's own. Assertions are time-independent apart from the structure check, which holds
 * until the lists' NextUpdate in 2027.
 */
public class EcDevelopmentList_UnitTest {

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private Environment envWith(LoteSlot slot, String name) {
		Environment env = new Environment();
		env.putObject("config", new JsonObject());
		LoteTestFixtures.INSTANCE.putLote(env, slot, LoteTestFixtures.INSTANCE.ecDevelopmentList(name));
		return env;
	}

	private static <T extends AbstractCondition> T cond(T cond, TestInstanceEventLog eventLog) {
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.WARNING);
		return cond;
	}

	@ParameterizedTest
	@CsvSource({"PID, pid-providers, 27", "WALLET, wallet-providers, 22", "WRPAC, wrpac-providers, 31", "MDL, mdl-providers, 30"})
	public void parsesVerifiesChecksAndExtractsTheList(LoteSlot slot, String name, int anchors) {
		Environment env = envWith(slot, name);
		assertThatCode(() -> cond(new ParseLote(), eventLog).execute(env)).doesNotThrowAnyException();
		assertThatCode(() -> cond(new ValidateLoteSignature(), eventLog).execute(env)).doesNotThrowAnyException();
		assertThatCode(() -> cond(new ValidateLoteStructure(), eventLog).execute(env)).doesNotThrowAnyException();
		assertThatCode(() -> cond(new ExtractLoteTrustAnchors(), eventLog).execute(env)).doesNotThrowAnyException();

		assertThat(env.getElementFromObject(AbstractLoteCondition.LOTE, "anchors.list").getAsJsonArray()).hasSize(anchors);
	}

	@ParameterizedTest
	@CsvSource({"PID, pid-providers", "WALLET, wallet-providers", "WRPAC, wrpac-providers", "MDL, mdl-providers"})
	public void flagsTheSignerWhoseCountryIsNotTheSchemeTerritory(LoteSlot slot, String name) {
		// sealed by "C=LU, O=European Commission, CN=TEST European Commission" while SchemeTerritory is "EU"
		Environment env = envWith(slot, name);
		cond(new ParseLote(), eventLog).execute(env);

		assertThatThrownBy(() -> cond(new ValidateLoteSignerCertificate(), eventLog).execute(env))
			.isInstanceOf(ConditionError.class).hasMessageContaining("6.8.0");
	}
}
