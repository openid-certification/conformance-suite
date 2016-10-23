package Bespoke.Heart.Runner;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value="/runner")
public class RunnerController{

    @RequestMapping(value = "runTest", method = RequestMethod.GET)
    ResponseEntity<?> runTest(){
        Runner runner = new Runner();
        runner.runTests(); //TODO find output and report back
                
        return new ResponseEntity<>("ran tests" , HttpStatus.OK);
                
    }
}