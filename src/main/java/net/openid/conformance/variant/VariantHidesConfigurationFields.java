package net.openid.conformance.variant;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Repeatable(VariantHidesConfigurationFieldsContainer.class)
public @interface VariantHidesConfigurationFields {

	Class<? extends Enum<?>> parameter();
	String value();
	String[] configurationFields();

}
