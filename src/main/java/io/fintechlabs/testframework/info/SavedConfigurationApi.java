package io.fintechlabs.testframework.info;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import com.mongodb.DBObject;

/**
 * @author jricher
 *
 */
@Controller
public class SavedConfigurationApi {

	@Autowired
	private SavedConfigurationService savedConfigurationService;

	@GetMapping(value = "/lastconfig", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Object> getLastConfig() {

		DBObject config = savedConfigurationService.getLastConfigForCurrentUser();

		if (config == null) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		} else {
			return new ResponseEntity<>(config, HttpStatus.OK);
		}

	}

}
