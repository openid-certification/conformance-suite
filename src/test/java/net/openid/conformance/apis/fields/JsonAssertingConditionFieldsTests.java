package net.openid.conformance.apis.fields;

import com.google.gson.JsonObject;
import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.UseResurce;
import net.openid.conformance.util.field.DoubleField;
import net.openid.conformance.util.field.IntArrayField;
import net.openid.conformance.util.field.IntField;
import net.openid.conformance.util.field.NumberArrayField;
import net.openid.conformance.util.field.NumberField;
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
		assertThat(error.getMessage(), containsString(condition.createFieldValueIsLessThanMinimum("number1")));
	}

	//NumberField
	@Test
	@UseResurce("jsonResponses/fieldsTests/FieldsTestNumberMoreMax.json")
	public void validateNumberMoreMax() {
		FieldsTestValidator condition = new FieldsTestValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createFieldValueIsMoreThanMaximum("number1")));
	}

	//DoubleField
	@Test
	@UseResurce("jsonResponses/fieldsTests/FieldsTestDoubleLessMin.json")
	public void validateDoubleLessMin() {
		FieldsTestValidator condition = new FieldsTestValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createFieldValueIsLessThanMinimum("double1")));
	}

	//DoubleField
	@Test
	@UseResurce("jsonResponses/fieldsTests/FieldsTestDoubleMoreMax.json")
	public void validateDoubleMoreMax() {
		FieldsTestValidator condition = new FieldsTestValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createFieldValueIsMoreThanMaximum("double1")));
	}

	//IntField
	@Test
	@UseResurce("jsonResponses/fieldsTests/FieldsTestIntLessMin.json")
	public void validateIntLessMin() {
		FieldsTestValidator condition = new FieldsTestValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createFieldValueIsLessThanMinimum("int1")));
	}

	//IntField
	@Test
	@UseResurce("jsonResponses/fieldsTests/FieldsTestIntMoreMax.json")
	public void validateIntMoreMax() {
		FieldsTestValidator condition = new FieldsTestValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createFieldValueIsMoreThanMaximum("int1")));
	}

	//IntArrayField
	@Test
	@UseResurce("jsonResponses/fieldsTests/FieldsTestIntArrayLessMin.json")
	public void validateIntArrayLessMin() {
		FieldsTestValidator condition = new FieldsTestValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createFieldValueIsLessThanMinimum("intArray1")));
	}

	//IntArrayField
	@Test
	@UseResurce("jsonResponses/fieldsTests/FieldsTestIntArrayMoreMax.json")
	public void validateIntArrayMoreMax() {
		FieldsTestValidator condition = new FieldsTestValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createFieldValueIsMoreThanMaximum("intArray1")));
	}

	//NumberArrayField
	@Test
	@UseResurce("jsonResponses/fieldsTests/FieldsTestNumberArrayLessMin.json")
	public void validateNumberArrayLessMin() {
		FieldsTestValidator condition = new FieldsTestValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createFieldValueIsLessThanMinimum("numberArray1")));
	}

	//NumberArrayField
	@Test
	@UseResurce("jsonResponses/fieldsTests/FieldsTestNumberArrayMoreMax.json")
	public void validateNumberArrayMoreMax() {
		FieldsTestValidator condition = new FieldsTestValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createFieldValueIsMoreThanMaximum("numberArray1")));
	}

	@ApiName("Fields Test")
	private static class FieldsDefaultValidator extends AbstractJsonAssertingCondition {

		@Override
		public Environment evaluate(Environment environment) {
			JsonObject body = bodyFrom(environment);

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

			return environment;
		}
	}

	@ApiName("Fields Test")
	private static class FieldsTestValidator extends AbstractJsonAssertingCondition {

		@Override
		public Environment evaluate(Environment environment) {
			JsonObject body = bodyFrom(environment);

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

			return environment;
		}
	}
}
