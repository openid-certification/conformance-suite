package net.openid.conformance.condition.as;

import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class CreateRevokedIdentifierListReference_UnitTest {

	private static final String BASE_URL = "https://localhost.emobix.co.uk:8443/test/a/alias";

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private CreateRevokedIdentifierListReference cond;

	@BeforeEach
	public void setUp() {
		cond = new CreateRevokedIdentifierListReference();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_storesTheServedUriAndARandomIdentifier() {
		env.putString("base_url", BASE_URL);

		cond.execute(env);

		assertThat(OIDFJSON.getString(env.getElementFromObject(
			CreateRevokedIdentifierListReference.ENV_KEY, "uri")))
			.isEqualTo(BASE_URL + "/" + CreateRevokedIdentifierListReference.IDENTIFIER_LIST_PATH);
		assertThat(identifier()).hasSize(16);
	}

	@Test
	public void testEvaluate_allocatesADifferentIdentifierEachTime() {
		env.putString("base_url", BASE_URL);

		cond.execute(env);
		byte[] first = identifier();
		cond.execute(env);

		// ISO/IEC 18013-5 12.3.6.4 recommends the Identifier be unique per MSO so it cannot be
		// used to correlate presentations
		assertThat(identifier()).isNotEqualTo(first);
	}

	@Test
	public void testEvaluate_failsWithoutABaseUrl() {
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	private byte[] identifier() {
		return Base64.getDecoder().decode(OIDFJSON.getString(
			env.getElementFromObject(CreateRevokedIdentifierListReference.ENV_KEY, "id")));
	}
}
