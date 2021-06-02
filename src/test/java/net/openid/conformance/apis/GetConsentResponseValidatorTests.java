package net.openid.conformance.apis;

import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.openbanking_brasil.GetConsentResponseValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;


@UseResurce("jsonResponses/account/readConsentResponse.json")
/**
 * This class tests the condition class ConsentResponseValidator
 *
 * The condition under test expects there to be a response body, which
 * will be populated by a preceeding condition
 *
 * The base class of this unit test is AbstractJsonResponseConditionUnitTest
 *
 * Notice the @UseResource annotation. This informs the unit testing framework
 * to load a mock HTTP response entity from that JSON file from the classpath
 * (typically in src/test/resources)
 *
 * If this annotation is present on any individual test method, that overrides
 * the class level annotation for that test
 */
public class GetConsentResponseValidatorTests extends AbstractJsonResponseConditionUnitTest {

	@Test
	public void validateStructure() {

		// Here we simply create an instance of our Condition class
		GetConsentResponseValidator condition = new GetConsentResponseValidator();

		// This evaluates the condition, passing in the loaded JSON document as
		// if it were an HTTP response
		run(condition);

	}

	@Test
	@UseResurce("jsonResponses/account/readConsentResponse_missing_consents.json")
	public void validateStructureWithMissingField() {

		// Here we simply create an instance of our Condition class
		GetConsentResponseValidator condition = new GetConsentResponseValidator();

		// This evaluates the condition, passing in the loaded JSON document as
		// if it were an HTTP response
		// In this instance, we expect a failure, and thus, examine it
		ConditionError error = runAndFail(condition);

		// We make sure it is the error we're expecting
		assertThat(error.getMessage(), containsString("ConsentResponseValidator: Unable to find path $.data.permissions"));

	}

}
