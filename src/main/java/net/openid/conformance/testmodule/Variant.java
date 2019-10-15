package net.openid.conformance.testmodule;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark a method as a 'variant'
 *
 * This is used to indicate that a test module has multiple versions. The user selects which variant they want before
 * create a test module, and the method that has the @Variant annotation is run after the TestModule is
 * constructed but before it is used - ie. before 'configure()'. This allows the test module to configure itself
 * appropriately for that variant.
 *
 * This annotation must be on the final published test module class; the suite does not search in the superclass
 * for Variants. This means subclasses can avoid exposing variants present in parent classes.
 *
 * The method annotated must be 'public'.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Variant {

	/**
	 * Name to be used to refer to this variant in web frontend / API
	 */
	String name();

	/**
	 * List of any additional fields required in the JSON configured when this variant is in use
	 */
	String[] configurationFields() default {};

}
