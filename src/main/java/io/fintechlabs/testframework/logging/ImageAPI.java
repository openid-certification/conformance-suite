/*******************************************************************************
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/

package io.fintechlabs.testframework.logging;

import java.io.IOException;
import java.util.Date;

import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.CriteriaDefinition;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.mongodb.BasicDBObjectBuilder;

/**
 * @author jricher
 *
 */
@Controller
public class ImageAPI {

	@Autowired
	private MongoTemplate mongoTemplate;

	@PostMapping(path = "/log/{id}/imgfile")
	public ResponseEntity<Object> uploadImageToNewLogEntry(@RequestBody String encoded,
			@PathVariable(name="id") String testId) throws IOException {
		
		// create a new entry in the database
		BasicDBObjectBuilder documentBuilder = BasicDBObjectBuilder.start()
				.add("_id", testId + "-" + RandomStringUtils.randomAlphanumeric(32))
				.add("testId", testId)
				.add("src", "_image-api")
				.add("time", new Date().getTime())
				.add("img", encoded);

		mongoTemplate.insert(documentBuilder.get(), DBEventLog.COLLECTION);
		
		return new ResponseEntity<>(HttpStatus.NO_CONTENT);
	}
	
	@PostMapping(path = "/log/{id}/imgfile/{placeholder}")
	public ResponseEntity<Object> uploadImageToExitingLogEntry(@RequestBody String encoded,
			@PathVariable(name="id") String testId,
			@PathVariable(name="placeholder") String placeholder) throws IOException {
		
		
		// find the existing entity
		Query query = Query.query(
				Criteria.where("testId").is(testId).andOperator(
						Criteria.where("upload").is(placeholder)));
		
		Update update = new Update();
		update.unset("upload");
		update.set("img", encoded);

		mongoTemplate.updateFirst(query, update, DBEventLog.COLLECTION);
		
		return new ResponseEntity<>(HttpStatus.NO_CONTENT);
	}
	
	
}
