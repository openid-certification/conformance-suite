/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Bespoke.Heart.Test;

import Bespoke.Heart.Runner.RunnerCallBack;
import org.slf4j.Logger;

/**
 *
 * @author tlewis
 */
public class ExampleTestCase implements TestCaseInterface{
    
    private static final String name = "Test Case 1";
    private RunnerCallBack rcb;
    private Logger log;
    private Object config;
    
    
    

    public String getName() {
        return name;
    }


    public void testSetup() {
        log.info("Setup with configuration: " + config.toString());
        rcb.setUpDone();
    }

    
    public void testRun() {
        log.info("Running test");
        //Do stuff
        rcb.runDone();
    }

    
    public void testStop() {
        log.info("Cleaning up");
        //Do stuff
        
    }


    @Override
    public void configureTestCase(Object config, Logger log, RunnerCallBack rcb) {
        this.config = config;
        this.log = log;
        this.rcb = rcb;
    }


    
    
}
