package com.ail.optile.jobservice;

import com.ail.optile.jobservice.domain.Job;
import com.google.common.collect.ImmutableList;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest
@TestPropertySource("classpath:application_test.properties")
@AutoConfigureMockMvc
public class JobServiceIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Test
    public void testGetJobs() {
        List<Job> jobs = ImmutableList.of(
                Job.builder()
                        .name("job1")
                        .command("ping localhost -c 5")
                        .cron("0/20 * * * * ?")
                        .build(),
                Job.builder()
                        .name("job2")
                        .command("echo 'hello' >> test")
                        .cron("0/30 * * * * ?")
                        .build()
        );

//        mvc.perform(get("/job-service/jobs")
//                .contentType(MediaType.APPLICATION_JSON))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$", hasSize(2)))
//
//                .andExpect(jsonPath("$[0].name", is("job1")))
//                .andExpect(jsonPath("$[0].command", is("ping localhost -c 5")))
//                .andExpect(jsonPath("$[0].cron", is("0/20 * * * * ?")))
//
//                .andExpect(jsonPath("$[1].name", is("job2")))
//                .andExpect(jsonPath("$[1].command", is("echo 'hello' >> test")))
//                .andExpect(jsonPath("$[1].cron", is("0/30 * * * * ?")));
    }
}
