package net.openid.conformance.configuration;

import net.openid.conformance.logging.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.Set;

@Configuration
public class BasicConfiguration {

	@Bean
	@Primary
	public EventLog eventLog(DBEventLog eventLog, @Value("${fintechlabs.sanitise.logs:false}") boolean sanitise) {
		if(sanitise) {
			return new SanitisingEventLog(eventLog, jsonObjectSanitiser(), mapSanitiser());
		} else {
			return eventLog;
		}
	}

	@Bean
	public JsonObjectSanitiser jsonObjectSanitiser() {
		return new JsonObjectSanitiser(Set.of(new PrivateKeyLeafVisitor(), new JwksLeafNodeVisitor()));
	}

	@Bean
	public MapSanitiser mapSanitiser() {
		return new MapSanitiser(Set.of(new PrivateKeyLeafVisitor(), new JwksLeafNodeVisitor(), new TestConfigLeafNodeVisitor()));
	}

}
