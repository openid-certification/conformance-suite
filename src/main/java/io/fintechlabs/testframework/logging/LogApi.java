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

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBObject;

/**
 * @author jricher
 *
 */
@Controller
public class LogApi {

	@Autowired
	private MongoTemplate mongoTemplate;
	
	@GetMapping(value = "/log", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<List<String>> getAllTests() {
		
		@SuppressWarnings("unchecked")
		List<String> testIds = mongoTemplate.getCollection(DBEventLog.COLLECTION).distinct("testId", BasicDBObjectBuilder.start().get());
		
		return new ResponseEntity<>(testIds, HttpStatus.OK);
		
	}
	
	@GetMapping(value = "/log/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<List<DBObject>> getTestInfo(@PathVariable("id") String id) {
		
		List<DBObject> results = mongoTemplate.getCollection(DBEventLog.COLLECTION).find(BasicDBObjectBuilder.start()
				.add("testId", id)
				.get())
			.sort(BasicDBObjectBuilder.start()
					.add("time", 1)
					.get())
			.toArray();
		
		return new ResponseEntity<>(results, HttpStatus.OK);
		
	}
	
	
}
