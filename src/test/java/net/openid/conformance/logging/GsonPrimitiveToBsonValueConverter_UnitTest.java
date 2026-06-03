package net.openid.conformance.logging;

import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import org.bson.BsonBoolean;
import org.bson.BsonDouble;
import org.bson.BsonInt32;
import org.bson.BsonInt64;
import org.bson.BsonString;
import org.bson.BsonValue;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

public class GsonPrimitiveToBsonValueConverter_UnitTest {

	private final GsonPrimitiveToBsonValueConverter converter = new GsonPrimitiveToBsonValueConverter();

	@Test
	public void convert_nullSource_returnsNull() {
		assertNull(converter.convert(null));
	}

	// --- Integer-valued numbers preserve int-ness ---

	@Test
	public void convert_smallInteger_isBsonInt32() {
		BsonValue result = converter.convert(new JsonPrimitive(900));

		assertInstanceOf(BsonInt32.class, result);
		assertEquals(900, ((BsonInt32) result).getValue());
	}

	@Test
	public void convert_integerAtIntMaxBoundary_isBsonInt32() {
		BsonValue result = converter.convert(new JsonPrimitive(Integer.MAX_VALUE));

		assertInstanceOf(BsonInt32.class, result);
		assertEquals(Integer.MAX_VALUE, ((BsonInt32) result).getValue());
	}

	@Test
	public void convert_integerAtIntMinBoundary_isBsonInt32() {
		BsonValue result = converter.convert(new JsonPrimitive(Integer.MIN_VALUE));

		assertInstanceOf(BsonInt32.class, result);
		assertEquals(Integer.MIN_VALUE, ((BsonInt32) result).getValue());
	}

	@Test
	public void convert_integerOverIntMax_isBsonInt64() {
		// 2^32, just above Integer.MAX_VALUE
		BsonValue result = converter.convert(new JsonPrimitive(4_294_967_296L));

		assertInstanceOf(BsonInt64.class, result);
		assertEquals(4_294_967_296L, ((BsonInt64) result).getValue());
	}

	@Test
	public void convert_integerBelowIntMin_isBsonInt64() {
		BsonValue result = converter.convert(new JsonPrimitive(-4_294_967_296L));

		assertInstanceOf(BsonInt64.class, result);
		assertEquals(-4_294_967_296L, ((BsonInt64) result).getValue());
	}

	// --- Numbers parsed from JSON text (the LazilyParsedNumber path) ---

	@Test
	public void convert_lazilyParsedSmallInteger_isBsonInt32() {
		// Integers parsed from JSON text come through as LazilyParsedNumber-backed primitives.
		JsonPrimitive lazy = JsonParser.parseString("42").getAsJsonPrimitive();

		BsonValue result = converter.convert(lazy);

		assertInstanceOf(BsonInt32.class, result);
		assertEquals(42, ((BsonInt32) result).getValue());
	}

	@Test
	public void convert_lazilyParsedLongInteger_isBsonInt64() {
		JsonPrimitive lazy = JsonParser.parseString("4294967296").getAsJsonPrimitive();

		BsonValue result = converter.convert(lazy);

		assertInstanceOf(BsonInt64.class, result);
		assertEquals(4_294_967_296L, ((BsonInt64) result).getValue());
	}

	@Test
	public void convert_lazilyParsedFractional_isBsonDouble() {
		JsonPrimitive lazy = JsonParser.parseString("1.5").getAsJsonPrimitive();

		BsonValue result = converter.convert(lazy);

		assertInstanceOf(BsonDouble.class, result);
		assertEquals(1.5d, ((BsonDouble) result).getValue());
	}

	@Test
	public void convert_lazilyParsedExponent_isBsonDouble() {
		// 1e10 fits in long but Long.parseLong rejects exponent notation, so the fallback fires.
		JsonPrimitive lazy = JsonParser.parseString("1e10").getAsJsonPrimitive();

		BsonValue result = converter.convert(lazy);

		assertInstanceOf(BsonDouble.class, result);
		assertEquals(1e10d, ((BsonDouble) result).getValue());
	}

	// --- Java-typed Doubles stay double ---

	@Test
	public void convert_javaDouble_isBsonDouble() {
		BsonValue result = converter.convert(new JsonPrimitive(3.14));

		assertInstanceOf(BsonDouble.class, result);
		assertEquals(3.14d, ((BsonDouble) result).getValue());
	}

	// --- Non-numeric primitives unchanged ---

	@Test
	public void convert_booleanTrue_isBsonBoolean() {
		BsonValue result = converter.convert(new JsonPrimitive(true));

		assertInstanceOf(BsonBoolean.class, result);
		assertEquals(true, ((BsonBoolean) result).getValue());
	}

	@Test
	public void convert_booleanFalse_isBsonBoolean() {
		BsonValue result = converter.convert(new JsonPrimitive(false));

		assertInstanceOf(BsonBoolean.class, result);
		assertEquals(false, ((BsonBoolean) result).getValue());
	}

	@Test
	public void convert_string_isBsonString() {
		BsonValue result = converter.convert(new JsonPrimitive("hello"));

		assertInstanceOf(BsonString.class, result);
		assertEquals("hello", ((BsonString) result).getValue());
	}
}
