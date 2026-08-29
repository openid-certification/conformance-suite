package net.openid.conformance.condition.client;

import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class ValidateIdentifierListTokenCwtFormat_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private ValidateIdentifierListTokenCwtFormat cond;

	@BeforeEach
	public void setUp() {
		cond = new ValidateIdentifierListTokenCwtFormat();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
		env.putString(AbstractIdentifierListCwtCondition.ENV_IDENTIFIER_LIST_URI,
			IdentifierListCwtTestFixtures.DEFAULT_URI);
	}

	@Test
	public void testEvaluate_acceptsAWellFormedIdentifierList() throws Exception {
		putToken(IdentifierListCwtTestFixtures.validIdentifierListToken());

		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void testEvaluate_acceptsAnEmptyIdentifiersMap() throws Exception {
		putToken(IdentifierListCwtTestFixtures.emptyIdentifierListToken());

		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void testEvaluate_rejectsTheStatusListMediaTypeInTheTypeHeader() throws Exception {
		putToken(IdentifierListCwtTestFixtures.identifierListTokenWithStatusListType());

		ConditionError error = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertThat(error.getMessage()).contains("application/identifierlist+cwt");
	}

	@Test
	public void testEvaluate_rejectsAStatusListClaimAlongsideTheIdentifierList() throws Exception {
		putToken(IdentifierListCwtTestFixtures.identifierListTokenWithStatusListClaim());

		ConditionError error = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertThat(error.getMessage()).contains("StatusList claim (key 65533)");
	}

	@Test
	public void testEvaluate_rejectsAMissingIdentifierListClaim() throws Exception {
		putToken(IdentifierListCwtTestFixtures.identifierListTokenWithoutIdentifierListClaim());

		ConditionError error = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertThat(error.getMessage()).contains("IdentifierList claim (key 65530)");
	}

	@Test
	public void testEvaluate_rejectsASubClaimThatDoesNotMatchTheMsoReference() throws Exception {
		putToken(IdentifierListCwtTestFixtures.validIdentifierListToken());
		env.putString(AbstractIdentifierListCwtCondition.ENV_IDENTIFIER_LIST_URI,
			"https://elsewhere.example.com/identifierlists/9");

		ConditionError error = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertThat(error.getMessage()).contains("sub claim (key 2)");
	}

	private void putToken(byte[] token) {
		env.putString(AbstractIdentifierListCwtCondition.ENV_IDENTIFIER_LIST_TOKEN,
			IdentifierListCwtTestFixtures.encode(token));
	}
}
