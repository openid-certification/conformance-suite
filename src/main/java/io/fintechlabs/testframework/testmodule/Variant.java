package io.fintechlabs.testframework.testmodule;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author jricher
 *
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Variant {

	String name();

	Accessory[] accessories() default {};

	String[] configurationFields() default {};

}
