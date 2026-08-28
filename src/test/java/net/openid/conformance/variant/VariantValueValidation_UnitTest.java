package net.openid.conformance.variant;

import net.openid.conformance.testmodule.TestModule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import variantvalidationfixtures.BrokenVariantFixtures;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Startup validation of variant value strings: any annotation (or plan-level exclusion)
 * referencing a value that doesn't exist on the parameter's enum must fail fast, so a
 * renamed or mistyped value can't silently disable the rule that referenced it.
 *
 * The broken fixtures live outside the {@code net.openid} scan root so they are only
 * seen when this test constructs holders for them directly.
 */
class VariantValueValidation_UnitTest {

	private static VariantService variantService;

	@BeforeAll
	static void setUp() {
		// also implicitly proves every real module, plan and parameter in the codebase validates
		variantService = new VariantService(holder -> true);
	}

	private void assertRejected(Class<? extends TestModule> moduleClass, String expectedSource) {
		assertRejected(() -> variantService.new TestModuleHolder(moduleClass), expectedSource);
	}

	private void assertRejected(Executable constructHolder, String expectedSource) {
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class, constructHolder);
		assertTrue(e.getMessage().contains("no_such_value"), e.getMessage());
		assertTrue(e.getMessage().contains(expectedSource), e.getMessage());
	}

	@Test
	void variantNotApplicableValueMustExist() {
		assertRejected(BrokenVariantFixtures.BadNotApplicableModule.class, "@VariantNotApplicable");
	}

	@Test
	void variantConfigurationFieldsValueMustExist() {
		assertRejected(BrokenVariantFixtures.BadConfigurationFieldsModule.class, "@VariantConfigurationFields");
	}

	@Test
	void variantHidesConfigurationFieldsValueMustExist() {
		assertRejected(BrokenVariantFixtures.BadHidesConfigurationFieldsModule.class, "@VariantHidesConfigurationFields");
	}

	@Test
	void variantSetupValueMustExist() {
		assertRejected(BrokenVariantFixtures.BadSetupModule.class, "@VariantSetup");
	}

	@Test
	void variantNotApplicableWhenValuesMustExist() {
		assertRejected(BrokenVariantFixtures.BadNotApplicableWhenValuesModule.class, "@VariantNotApplicableWhen");
	}

	@Test
	void variantNotApplicableWhenHasValuesMustExist() {
		assertRejected(BrokenVariantFixtures.BadNotApplicableWhenHasValuesModule.class, "@VariantNotApplicableWhen");
	}

	@Test
	void variantParameterDefaultValueMustExist() {
		assertRejected(() -> new VariantService.ParameterHolder<>(BrokenVariantFixtures.BadDefaultParam.class), "defaultValue");
	}

	@Test
	void planExclusionValueMustExist() {
		assertRejected(() -> variantService.new TestPlanHolder(BrokenVariantFixtures.BadExclusionPlan.class), "variantsNotApplicable");
	}

	@Test
	void planExclusionParameterMustBeVariantParameter() {
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
				() -> variantService.new TestPlanHolder(BrokenVariantFixtures.NotAVariantParameterPlan.class));
		assertTrue(e.getMessage().contains("NotAVariantParameterPlan"), e.getMessage());
		assertTrue(e.getMessage().contains("not a variant parameter"), e.getMessage());
	}

	@Test
	void validAnnotationValuesAreAccepted() {
		assertDoesNotThrow(() -> variantService.new TestModuleHolder(BrokenVariantFixtures.GoodModule.class));
	}
}
