package com.jjchmielewski.tftarena.initializer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class GraphBuilder {

    @Autowired
    DataCollector dataCollector;


    @PostConstruct
    public void init() throws InterruptedException {

        //dataCollector.start();

    }
}
