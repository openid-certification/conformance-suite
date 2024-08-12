package net.openid.conformance.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class AdditiveUrlBasedCorsConfigurationSourceTest {

	@Test
	public void does_not_allow_using_the_set_configuration_method(){
		assertThrows(RuntimeException.class, () -> {
			new AdditiveUrlBasedCorsConfigurationSource().setCorsConfigurations(null);
		});
	}
}
