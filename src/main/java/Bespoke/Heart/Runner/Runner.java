/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Bespoke.Heart.Runner;

import Bespoke.Heart.Test.ExampleTestCase;
import Bespoke.Heart.Test.TestCaseInterface;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author tlewis
 */
public class Runner implements RunnerCallBack{
    
    private Logger log;
    
    private TestCaseInterface test;

    public Runner() {
        this.log = LoggerFactory.getLogger(Runner.class);
    }
    
    
    
    public void runTests(){
        log.info("Starting test");
        //TODO how do we cleanly instatiate the test?
        test = new ExampleTestCase();       
        Logger slf4jLogger = LoggerFactory.getLogger(test.getName());
        test.configureTestCase("ConfigTestString", slf4jLogger, this);
        test.testSetup();
    }

    @Override
    public void setUpDone() {
        test.testRun();
    }

    @Override
    public void runDone() {
        test.testStop();
    }

    @Override
    public void stopRecived() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
