package io.fintechlabs.testframework.testmodule;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface PublishTestModule {

	String testName();

	String displayName();

	String profile() default "SAMPLE";

	String[] configurationFields() default {};

	String summary() default "";

	Variant[] variants() default {};

}
