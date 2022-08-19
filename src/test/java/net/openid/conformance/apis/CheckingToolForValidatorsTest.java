package net.openid.conformance.apis;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.openinsurance.validator.channels.v1.ElectronicChannelsValidator;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.UseResurce;
import net.openid.conformance.util.field.ObjectField;
import net.openid.conformance.util.field.StringField;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

public class CheckingToolForValidatorsTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	@UseResurce("jsonResponses/errors/electronicChannelsResponseWithAdditionalField.json")
	public void validateWithErrorAdditionalFieldWhatAreAbsentInSwagger() {
		ElectronicChannelsValidator condition = new ElectronicChannelsValidator();
		ConditionError error = runAndFail(condition);
		String expected = "ElectronicChannelsValidator: This fields are not validated on the Electronic Channels API response: urlComplementaryList";
		assertThat(error.getMessage(), containsString(expected));
	}

	@Test
	@UseResurce("jsonResponses/errors/validatorWithError1.json")
	public void validateWithError1() {
		TestValidator condition = new TestValidator();
		ConditionError error = runAndFail(condition);
		String expected = "TestValidator: This fields are not validated on the TestValidator API response: someField3";
		assertThat(error.getMessage(), containsString(expected));
	}

	@Test
	@UseResurce("jsonResponses/errors/validatorWithError1.json")
	public void validateWithError2() {
		TestValidator2 condition = new TestValidator2();
		ConditionError error = runAndFail(condition);
		String expected = "TestValidator2: This fields are not validated on the TestValidator2 API response: someField2, someField3";
		assertThat(error.getMessage(), containsString(expected));
	}

	@Test
	@UseResurce("jsonResponses/errors/validatorWithError1.json")
	public void validateWithError3() {
		TestValidator3 condition = new TestValidator3();
		ConditionError error = runAndFail(condition);
		String expected = "TestValidator3: This fields are not validated on the TestValidator3 API response: someField1, innerField1, innerField2";
		assertThat(error.getMessage(), containsString(expected));
	}

	@Test
	@UseResurce("jsonResponses/errors/validatorWithError1.json")
	public void validateOptionalFieldWithBlot() {
		TestValidator4 condition = new TestValidator4();
		ConditionError error = runAndFail(condition);
		String expected = "TestValidator4: This fields are not validated on the TestValidator4 API response: someField3";
		assertThat(error.getMessage(), containsString(expected));
	}


	private static class TestValidator extends AbstractJsonAssertingCondition {

		@Override
		public Environment evaluate(Environment environment) {
			JsonElement body = bodyFrom(environment);
			assertField(body,
				new ObjectField
					.Builder("data")
					.setValidator(data -> {
						assertField(data, new ObjectField
							.Builder("someField1")
							.setValidator(element -> {
								assertField(element,
									new StringField
										.Builder("innerField1")
										.build());
								assertField(element,
									new StringField
										.Builder("innerField2")
										.build());
							})
							.build());

						assertField(data,
							new StringField
								.Builder("someField2")
								.build());
//						With out someField3
//						assertField(data,
//							new ObjectField
//								.Builder("someField3")
//								.build());

						assertField(data,
							new StringField
								.Builder("someField4")
								.build());
					})
					.build());
			return environment;
		}
	}

	private static class TestValidator2 extends AbstractJsonAssertingCondition {

		@Override
		public Environment evaluate(Environment environment) {
			JsonElement body = bodyFrom(environment);
			assertField(body,
				new ObjectField
					.Builder("data")
					.setValidator(data -> {
						assertField(data, new ObjectField
							.Builder("someField1")
							.setValidator(element -> {
								assertField(element,
									new StringField
										.Builder("innerField1")
										.build());
								assertField(element,
									new StringField
										.Builder("innerField2")
										.build());
							})
							.build());

//						assertField(data,
//							new StringField
//								.Builder("someField2")
//								.build());
//						With out someField3
//						assertField(data,
//							new ObjectField
//								.Builder("someField3")
//								.build());

						assertField(data,
							new StringField
								.Builder("someField4")
								.build());
					})
					.build());
			return environment;
		}
	}

	private static class TestValidator3 extends AbstractJsonAssertingCondition {

		@Override
		public Environment evaluate(Environment environment) {
			JsonElement body = bodyFrom(environment);
			assertField(body,
				new ObjectField
					.Builder("data")
					.setValidator(data -> {
						//With out someField1 and innerField1 and innerField2
						//	assertField(data,
//							new ObjectField
//								.Builder("someField1")
//								.setValidator(element -> {
//									assertField(element,
//										new StringField
//											.Builder("innerField1")
//											.build());
//									assertField(element,
//										new StringField
//											.Builder("innerField2")
//											.build());
//								})
//								.build());

						assertField(data,
							new StringField
								.Builder("someField2")
								.build());

						assertField(data,
							new ObjectField
								.Builder("someField3")
								.build());

						assertField(data,
							new StringField
								.Builder("someField4")
								.build());
					})
					.build());
			return environment;
		}
	}

	private static class TestValidator4 extends AbstractJsonAssertingCondition {

		@Override
		public Environment evaluate(Environment environment) {
			JsonElement body = bodyFrom(environment);
			assertField(body,
				new ObjectField
					.Builder("data")
					.setValidator(data -> {
						assertField(data, new ObjectField
							.Builder("someField1")
							.setValidator(element -> {
								assertField(element,
									new StringField
										.Builder("innerField1")
										.build());
								assertField(element,
									new StringField
										.Builder("innerField2")
										.build());
							})
							.build());

						assertField(data,
							new StringField
								.Builder("someField2")
								.build());

						assertField(data,
							new ObjectField
								.Builder("SomeField3")//with a blot
								.setOptional()
								.build());

						assertField(data,
							new StringField
								.Builder("someField4")
								.build());
					})
					.build());
			return environment;
		}
	}
}
