package net.openid.conformance.condition.as;

import net.openid.conformance.condition.Condition.ConditionResult;
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
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

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
			RevocationListReference.ENV_KEY, "uri")))
			.matches(Pattern.quote(BASE_URL + "/identifierlists/") + "[A-Za-z0-9]{20}");
		assertThat(identifier()).hasSize(16);
		assertThat(OIDFJSON.getString(env.getElementFromObject(
			RevocationListReference.ENV_KEY, "mechanism"))).isEqualTo("identifier_list");
	}

	@Test
	public void testEvaluate_allocatesADifferentUriEachTime() {
		env.putString("base_url", BASE_URL);

		cond.execute(env);
		String first = OIDFJSON.getString(env.getElementFromObject(
			RevocationListReference.ENV_KEY, "uri"));
		cond.execute(env);

		assertThat(OIDFJSON.getString(env.getElementFromObject(
			RevocationListReference.ENV_KEY, "uri")))
			.as("a verifier must not be able to reuse a list cached from an earlier run")
			.isNotEqualTo(first);
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

	private byte[] identifier() {
		return Base64.getDecoder().decode(OIDFJSON.getString(
			env.getElementFromObject(RevocationListReference.ENV_KEY, "id")));
	}
}
