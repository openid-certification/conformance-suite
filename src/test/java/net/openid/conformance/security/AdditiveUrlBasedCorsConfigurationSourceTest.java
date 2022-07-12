package net.openid.conformance.security;

import org.junit.Test;

public class AdditiveUrlBasedCorsConfigurationSourceTest {

	@Test(expected = RuntimeException.class)
	public void does_not_allow_using_the_set_configuration_method(){
		new AdditiveUrlBasedCorsConfigurationSource().setCorsConfigurations(null);
	}
}
