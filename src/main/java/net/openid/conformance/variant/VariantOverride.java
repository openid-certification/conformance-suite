package net.openid.conformance.variant;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Allows a test module to have a variant that is different to the variant selected for the TestPlan */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Repeatable(VariantOverrideContainer.class)
public @interface VariantOverride {

	Class<? extends Enum<?>> parameter();
	String value();

}
