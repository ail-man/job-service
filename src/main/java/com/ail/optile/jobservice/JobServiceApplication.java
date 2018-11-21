package com.ail.optile.jobservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
// TODO check with the article https://juliuskrah.com/tutorial/2017/09/26/dynamic-job-scheduling-with-quartz-and-spring/
public class JobServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(JobServiceApplication.class, args);
    }
}
