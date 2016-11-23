
package io.bspk.testframework.strawman;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;

@SpringBootApplication
public class HeartApplication {

	public static void main(String[] args) {
		SpringApplication.run(HeartApplication.class, args);
	}
}
