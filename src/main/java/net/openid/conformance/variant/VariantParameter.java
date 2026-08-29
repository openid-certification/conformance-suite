package net.openid.conformance.variant;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface VariantParameter {

	String name();

	String displayName();

	String description() default "";

	String defaultValue() default "";

	/**
	 * Sort order for the UI; lower values sort first. Parameters sharing a variant name
	 * must declare the same value (enforced when VariantService is constructed); ties
	 * sort by variant name.
	 */
	int sortOrder() default 1000;

}
