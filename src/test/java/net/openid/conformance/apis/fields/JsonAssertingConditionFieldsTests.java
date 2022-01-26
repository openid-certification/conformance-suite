package net.openid.conformance.apis.fields;

import com.google.gson.JsonElement;
import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.condition.client.jsonAsserting.ErrorMessagesUtils;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.UseResurce;
import net.openid.conformance.util.field.DoubleField;
import net.openid.conformance.util.field.IntArrayField;
import net.openid.conformance.util.field.IntField;
import net.openid.conformance.util.field.NumberArrayField;
import net.openid.conformance.util.field.NumberField;
import net.openid.conformance.util.field.StringArrayField;
import net.openid.conformance.util.field.StringField;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;


public class JsonAssertingConditionFieldsTests extends AbstractJsonResponseConditionUnitTest {

	@Test
	@UseResurce("jsonResponses/fieldsTests/FieldsTestValid.json")
	public void validateStructure() {
		run(new FieldsTestValidator());
	}

	@Test
	@UseResurce("jsonResponses/fieldsTests/FieldsTestValid.json")
	public void validateDefault() { // without min/max Value
		run(new FieldsDefaultValidator());
	}

	//NumberField
	@Test
	@UseResurce("jsonResponses/fieldsTests/FieldsTestNumberLessMin.json")
	public void validateNumberLessMin() {
		FieldsTestValidator condition = new FieldsTestValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils
			.createFieldValueIsLessThanMinimum("number1", condition.getApiName())));
	}

	//NumberField
	@Test
	@UseResurce("jsonResponses/fieldsTests/FieldsTestNumberMoreMax.json")
	public void validateNumberMoreMax() {
		FieldsTestValidator condition = new FieldsTestValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils
			.createFieldValueIsMoreThanMaximum("number1", condition.getApiName())));
	}

	//NumberField
	@Test
	@UseResurce("jsonResponses/fieldsTests/FieldsTestNumberMinLength.json")
	public void validateNumberMinLength() {
		FieldsTestValidatorMinMaxLengthItem condition = new FieldsTestValidatorMinMaxLengthItem();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(),containsString(ErrorMessagesUtils
			.createFieldValueIsLessThanMinLengthMessage("number1", condition.getApiName())));
	}

	//NumberField
	@Test
	@UseResurce("jsonResponses/fieldsTests/FieldsTestNumberMaxLength.json")
	public void validateNumberMaxLength() {
		FieldsTestValidatorMinMaxLengthItem condition = new FieldsTestValidatorMinMaxLengthItem();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(),containsString(ErrorMessagesUtils
			.createFieldValueIsMoreThanMaxLengthMessage("number1", condition.getApiName())));
	}


	//DoubleField
	@Test
	@UseResurce("jsonResponses/fieldsTests/FieldsTestDoubleLessMin.json")
	public void validateDoubleLessMin() {
		FieldsTestValidator condition = new FieldsTestValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils
			.createFieldValueIsLessThanMinimum("double1", condition.getApiName())));
	}

	//DoubleField
	@Test
	@UseResurce("jsonResponses/fieldsTests/FieldsTestDoubleMoreMax.json")
	public void validateDoubleMoreMax() {
		FieldsTestValidator condition = new FieldsTestValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils
			.createFieldValueIsMoreThanMaximum("double1", condition.getApiName())));
	}

	//DoubleField
	@Test
	@UseResurce("jsonResponses/fieldsTests/FieldsTestDoubleMinLength.json")
	public void validateDoubleMinLength() {
		FieldsTestValidatorMinMaxLengthItem condition = new FieldsTestValidatorMinMaxLengthItem();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(),containsString(ErrorMessagesUtils
			.createFieldValueIsLessThanMinLengthMessage("double1", condition.getApiName())));
	}

	//DoubleField
	@Test
	@UseResurce("jsonResponses/fieldsTests/FieldsTestDoubleMaxLength.json")
	public void validateDoubleMaxLength() {
		FieldsTestValidatorMinMaxLengthItem condition = new FieldsTestValidatorMinMaxLengthItem();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(),containsString(ErrorMessagesUtils
			.createFieldValueIsMoreThanMaxLengthMessage("double1", condition.getApiName())));
	}

	//IntField
	@Test
	@UseResurce("jsonResponses/fieldsTests/FieldsTestIntLessMin.json")
	public void validateIntLessMin() {
		FieldsTestValidator condition = new FieldsTestValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils
			.createFieldValueIsLessThanMinimum("int1", condition.getApiName())));
	}

	//IntField
	@Test
	@UseResurce("jsonResponses/fieldsTests/FieldsTestIntMoreMax.json")
	public void validateIntMoreMax() {
		FieldsTestValidator condition = new FieldsTestValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(),
			containsString(ErrorMessagesUtils
				.createFieldValueIsMoreThanMaximum("int1", condition.getApiName())));
	}

	//IntField
	@Test
	@UseResurce("jsonResponses/fieldsTests/FieldsTestIntMinLength.json")
	public void validateIntMinLength() {
		FieldsTestValidatorMinMaxLengthItem condition = new FieldsTestValidatorMinMaxLengthItem();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(),containsString(ErrorMessagesUtils
			.createFieldValueIsLessThanMinLengthMessage("int1", condition.getApiName())));
	}

	//IntField
	@Test
	@UseResurce("jsonResponses/fieldsTests/FieldsTestIntMaxLength.json")
	public void validateIntMaxLength() {
		FieldsTestValidatorMinMaxLengthItem condition = new FieldsTestValidatorMinMaxLengthItem();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(),containsString(ErrorMessagesUtils
			.createFieldValueIsMoreThanMaxLengthMessage("int1", condition.getApiName())));
	}

	//IntArrayField
	@Test
	@UseResurce("jsonResponses/fieldsTests/FieldsTestIntArrayLessMin.json")
	public void validateIntArrayLessMin() {
		FieldsTestValidator condition = new FieldsTestValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils
			.createFieldValueIsLessThanMinimum("intArray1", condition.getApiName())));
	}

	//IntArrayField
	@Test
	@UseResurce("jsonResponses/fieldsTests/FieldsTestIntArrayMoreMax.json")
	public void validateIntArrayMoreMax() {
		FieldsTestValidator condition = new FieldsTestValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils
			.createFieldValueIsMoreThanMaximum("intArray1", condition.getApiName())));
	}

	//IntArrayField
	@Test
	@UseResurce("jsonResponses/fieldsTests/FieldsTestIntArrayMinItem.json")
	public void validateIntArrayMinItem() {
		FieldsTestValidatorMinMaxLengthItem condition = new FieldsTestValidatorMinMaxLengthItem();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(),containsString(ErrorMessagesUtils
			.createArrayIsLessThanMaxItemsMessage("intArray1", condition.getApiName())));
	}

	//IntArrayField
	@Test
	@UseResurce("jsonResponses/fieldsTests/FieldsTestIntArrayMaxItem.json")
	public void validateIntArrayMaxItem() {
		FieldsTestValidatorMinMaxLengthItem condition = new FieldsTestValidatorMinMaxLengthItem();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(),containsString(ErrorMessagesUtils
			.createArrayIsMoreThanMaxItemsMessage("intArray1", condition.getApiName())));
	}

	//IntArrayField
	@Test
	@UseResurce("jsonResponses/fieldsTests/FieldsTestIntArrayMinLength.json")
	public void validateIntArrayMinLength() {
		FieldsTestValidatorMinMaxLengthItem condition = new FieldsTestValidatorMinMaxLengthItem();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(),containsString(ErrorMessagesUtils
			.createFieldValueIsLessThanMinLengthMessage("intArray1", condition.getApiName())));
	}

	//IntArrayField
	@Test
	@UseResurce("jsonResponses/fieldsTests/FieldsTestIntArrayMaxLength.json")
	public void validateIntArrayMaxLength() {
		FieldsTestValidatorMinMaxLengthItem condition = new FieldsTestValidatorMinMaxLengthItem();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(),containsString(ErrorMessagesUtils
			.createFieldValueIsMoreThanMaxLengthMessage("intArray1", condition.getApiName())));
	}


	//NumberArrayField
	@Test
	@UseResurce("jsonResponses/fieldsTests/FieldsTestNumberArrayLessMin.json")
	public void validateNumberArrayLessMin() {
		FieldsTestValidator condition = new FieldsTestValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils
			.createFieldValueIsLessThanMinimum("numberArray1", condition.getApiName())));
	}

	//NumberArrayField
	@Test
	@UseResurce("jsonResponses/fieldsTests/FieldsTestNumberArrayMoreMax.json")
	public void validateNumberArrayMoreMax() {
		FieldsTestValidator condition = new FieldsTestValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils
			.createFieldValueIsMoreThanMaximum("numberArray1", condition.getApiName())));
	}

	//NumberArrayField
	@Test
	@UseResurce("jsonResponses/fieldsTests/FieldsTestArrayNumberArrayMinItem.json")
	public void validateNumberArrayMinItem() {
		FieldsTestValidatorMinMaxLengthItem condition = new FieldsTestValidatorMinMaxLengthItem();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(),containsString(ErrorMessagesUtils
			.createArrayIsLessThanMaxItemsMessage("numberArray1", condition.getApiName())));
	}

	//NumberArrayField
	@Test
	@UseResurce("jsonResponses/fieldsTests/FieldsTestArrayNumberArrayMaxItem.json")
	public void validateNumberArrayMaxItem() {
		FieldsTestValidatorMinMaxLengthItem condition = new FieldsTestValidatorMinMaxLengthItem();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(),containsString(ErrorMessagesUtils
			.createArrayIsMoreThanMaxItemsMessage("numberArray1", condition.getApiName())));
	}

	//NumberArrayField
	@Test
	@UseResurce("jsonResponses/fieldsTests/FieldsTestArrayNumberArrayMinLength.json")
	public void validateNumberArrayMinLength() {
		FieldsTestValidatorMinMaxLengthItem condition = new FieldsTestValidatorMinMaxLengthItem();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(),containsString(ErrorMessagesUtils
			.createFieldValueIsLessThanMinLengthMessage("numberArray1", condition.getApiName())));
	}

	//NumberArrayField
	@Test
	@UseResurce("jsonResponses/fieldsTests/FieldsTestArrayNumberArrayMaxLength.json")
	public void validateNumberArrayMaxLength() {
		FieldsTestValidatorMinMaxLengthItem condition = new FieldsTestValidatorMinMaxLengthItem();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils
				.createFieldValueIsMoreThanMaxLengthMessage("numberArray1", condition.getApiName())));
	}

	//StringField
	@Test
	@UseResurce("jsonResponses/fieldsTests/FieldsTestArrayStringMinLength.json")
	public void validateStringMinLength() {
		FieldsTestValidatorMinMaxLengthItem condition = new FieldsTestValidatorMinMaxLengthItem();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(),containsString(ErrorMessagesUtils
			.createFieldValueIsLessThanMinLengthMessage("string1", condition.getApiName())));
	}

	//StringField
	@Test
	@UseResurce("jsonResponses/fieldsTests/FieldsTestArrayStringMaxLength.json")
	public void validateStringMaxLength() {
		FieldsTestValidatorMinMaxLengthItem condition = new FieldsTestValidatorMinMaxLengthItem();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(),containsString(ErrorMessagesUtils
			.createFieldValueIsMoreThanMaxLengthMessage("string1", condition.getApiName())));
	}

	//StringArrayField
	@Test
	@UseResurce("jsonResponses/fieldsTests/FieldsTestArrayStringArrayMinItem.json")
	public void validateStringArrayMinItem() {
		FieldsTestValidatorMinMaxLengthItem condition = new FieldsTestValidatorMinMaxLengthItem();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(),containsString(ErrorMessagesUtils
			.createArrayIsLessThanMaxItemsMessage("stringArray1", condition.getApiName())));
	}

	//StringArrayField
	@Test
	@UseResurce("jsonResponses/fieldsTests/FieldsTestArrayStringArrayMaxItem.json")
	public void validateStringArrayMaxItem() {
		FieldsTestValidatorMinMaxLengthItem condition = new FieldsTestValidatorMinMaxLengthItem();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(),containsString(ErrorMessagesUtils
			.createArrayIsMoreThanMaxItemsMessage("stringArray1", condition.getApiName())));
	}

	//StringArrayField
	@Test
	@UseResurce("jsonResponses/fieldsTests/FieldsTestArrayStringArrayMinLength.json")
	public void validateStringArrayMinLength() {
		FieldsTestValidatorMinMaxLengthItem condition = new FieldsTestValidatorMinMaxLengthItem();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(),containsString(ErrorMessagesUtils
			.createFieldValueIsLessThanMinLengthMessage("stringArray1", condition.getApiName())));
	}

	//StringArrayField
	@Test
	@UseResurce("jsonResponses/fieldsTests/FieldsTestArrayStringArrayMaxLength.json")
	public void validateStringArrayMaxLength() {
		FieldsTestValidatorMinMaxLengthItem condition = new FieldsTestValidatorMinMaxLengthItem();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils
			.createFieldValueIsMoreThanMaxLengthMessage("stringArray1", condition.getApiName())));
	}

	@ApiName("Fields Test")
	private static class FieldsDefaultValidator extends AbstractJsonAssertingCondition {

		@Override
		public Environment evaluate(Environment environment) {
			JsonElement body = bodyFrom(environment);

			assertField(body,
				new NumberField
					.Builder("number1")
					.build());

			assertField(body,
				new DoubleField
					.Builder("double1")
					.build());

			assertField(body,
				new IntField
					.Builder("int1")
					.build());

			assertField(body,
				new IntArrayField
					.Builder("intArray1")
					.build());

			assertField(body,
				new NumberArrayField
					.Builder("numberArray1")
					.build());

			assertField(body,
				new StringField
					.Builder("string1")
					.build());

			assertField(body,
				new StringArrayField
					.Builder("stringArray1")
					.build());

			return environment;
		}
	}

	@ApiName("Fields Test")
	private static class FieldsTestValidator extends AbstractJsonAssertingCondition {

		@Override
		public Environment evaluate(Environment environment) {
			JsonElement body = bodyFrom(environment);

			assertField(body,
				new NumberField
					.Builder("number1")
					.setMaxValue(2)
					.setMinValue(1.1)
					.build());

			assertField(body,
				new DoubleField
					.Builder("double1")
					.setMaxValue(2.1)
					.setMinValue(1.1)
					.build());

			assertField(body,
				new IntField
					.Builder("int1")
					.setMaxValue(3)
					.setMinValue(1)
					.build());

			assertField(body,
				new IntArrayField
					.Builder("intArray1")
					.setMaxValue(6)
					.setMinValue(3)
					.build());

			assertField(body,
				new NumberArrayField
					.Builder("numberArray1")
					.setMaxValue(3)
					.setMinValue(1.1)
					.build());

			assertField(body,
				new StringField
					.Builder("string1")
					.build());

			assertField(body,
				new StringArrayField
					.Builder("stringArray1")
					.build());

			return environment;
		}
	}

	@ApiName("Fields Test")
	private static class FieldsTestValidatorMinMaxLengthItem extends AbstractJsonAssertingCondition {

		@Override
		public Environment evaluate(Environment environment) {
			JsonElement body = bodyFrom(environment);

			assertField(body,
				new NumberField
					.Builder("number1")
					.setMinLength(2)
					.setMaxLength(3)
					.build());

			assertField(body,
				new DoubleField
					.Builder("double1")
					.setMinLength(2)
					.setMaxLength(4)
					.build());

			assertField(body,
				new IntField
					.Builder("int1")
					.setMinLength(2)
					.setMaxLength(3)
					.build());

			assertField(body,
				new IntArrayField
					.Builder("intArray1")
					.setMinItems(1)
					.setMaxItems(3)
					.setMinLength(2)
					.setMaxLength(3)
					.build());

			assertField(body,
				new NumberArrayField
					.Builder("numberArray1")
					.setMinItems(1)
					.setMaxItems(3)
					.setMinLength(2)
					.setMaxLength(3)
					.build());

			assertField(body,
				new StringField
					.Builder("string1")
					.setMinLength(2)
					.setMaxLength(5)
					.build());

			assertField(body,
				new StringArrayField
					.Builder("stringArray1")
					.setMinItems(1)
					.setMaxItems(3)
					.setMinLength(2)
					.setMaxLength(5)
					.build());

			return environment;
		}
	}
}
