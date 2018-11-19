package com.ail.optile.jobservice.rest;

import com.ail.optile.jobservice.api.JobInfo;
import com.ail.optile.jobservice.api.JobRequest;
import com.ail.optile.jobservice.api.JobService;
import com.ail.optile.jobservice.api.exception.IncorrectJobRequestException;
import com.ail.optile.jobservice.api.exception.JobAlreadyExistsException;
import com.ail.optile.jobservice.api.exception.JobIsCurrentlyRunningException;
import com.ail.optile.jobservice.api.exception.JobIsNotFoundException;
import com.ail.optile.jobservice.domain.NativeJobInfo;
import com.google.common.collect.ImmutableList;
import org.dozer.DozerBeanMapper;
import org.dozer.Mapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebMvcTest(JobServiceRestController.class)
public class JobServiceRestControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private JobService jobService;

    @MockBean
    private Mapper mapper;

    @Before
    public void init() {
        DozerBeanMapper dozerBeanMapper = new DozerBeanMapper();

        given(mapper.map(any(), any())).willAnswer(invocationOnMock -> {
            Object from = invocationOnMock.getArgument(0);
            Object to = invocationOnMock.getArgument(1);
            if (to instanceof Class<?>) {
                return dozerBeanMapper.map(from, (Class<?>) to);
            } else {
                dozerBeanMapper.map(from, to);
                return null;
            }
        });
    }

    @Test
    public void testIncorrectJobRequestRestException() throws Exception {
        doThrow(new IncorrectJobRequestException(new IllegalArgumentException()))
                .when(jobService).create(any(JobRequest.class));
        mvc.perform(post("/job-service/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\n" +
                        "\t\"name\":\"job1\",\n" +
                        "\t\"command\":\"ping localhost -c 4\",\n" +
                        "\t\"cron\":\"\"\n" +
                        "}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void testCreateJob() throws Exception {
        mvc.perform(post("/job-service/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\n" +
                        "\t\"name\":\"job1\",\n" +
                        "\t\"command\":\"ping localhost -c 4\",\n" +
                        "\t\"cron\":\"0/30 * * * * ?\"\n" +
                        "}"))
                .andExpect(status().isOk());
    }

    @Test
    public void testJobAlreadyExistsRestException() throws Exception {
        doThrow(new JobAlreadyExistsException()).when(jobService).create(any(JobRequest.class));
        mvc.perform(post("/job-service/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\n" +
                        "\t\"name\":\"job1\",\n" +
                        "\t\"command\":\"ping localhost -c 4\",\n" +
                        "\t\"cron\":\"0/30 * * * * ?\"\n" +
                        "}"))
                .andExpect(status().isConflict());
    }

    @Test
    public void testExecuteJob() throws Exception {
        doNothing().when(jobService).execute("job1");
        mvc.perform(post("/job-service/execute/job1"))
                .andExpect(status().isOk());

        doThrow(new JobIsNotFoundException()).when(jobService).execute("job2");
        mvc.perform(post("/job-service/execute/job2"))
                .andExpect(status().isNotFound());

        doThrow(new JobIsCurrentlyRunningException()).when(jobService).execute("job3");
        mvc.perform(post("/job-service/execute/job3"))
                .andExpect(status().isImUsed());
    }

    @Test
    public void testUpdateJob() throws Exception {
        mvc.perform(post("/job-service/update")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\n" +
                        "\t\"name\":\"job1\",\n" +
                        "\t\"command\":\"ping localhost -c 4\",\n" +
                        "\t\"cron\":\"0/30 * * * * ?\"\n" +
                        "}"))
                .andExpect(status().isOk());

        doThrow(new JobIsNotFoundException()).when(jobService).update(any(JobRequest.class));
        mvc.perform(post("/job-service/update")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\n" +
                        "\t\"name\":\"job2\",\n" +
                        "\t\"command\":\"ping localhost -c 4\",\n" +
                        "\t\"cron\":\"0/20 * * * * ?\"\n" +
                        "}"))
                .andExpect(status().isNotFound());

        doThrow(new JobIsCurrentlyRunningException()).when(jobService).update(any(JobRequest.class));
        mvc.perform(post("/job-service/update")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\n" +
                        "\t\"name\":\"job2\",\n" +
                        "\t\"command\":\"ping localhost -c 4\",\n" +
                        "\t\"cron\":\"0/20 * * * * ?\"\n" +
                        "}"))
                .andExpect(status().isImUsed());
    }

    @Test
    public void testDeleteJob() throws Exception {
        doNothing().when(jobService).delete("job1");
        mvc.perform(post("/job-service/delete/job1"))
                .andExpect(status().isOk());

        doThrow(new JobIsNotFoundException()).when(jobService).delete("job2");
        mvc.perform(post("/job-service/delete/job2"))
                .andExpect(status().isNotFound());

        doThrow(new JobIsCurrentlyRunningException()).when(jobService).delete("job3");
        mvc.perform(post("/job-service/delete/job3"))
                .andExpect(status().isImUsed());
    }

    @Test
    public void testGetJobInfo() throws Exception {
        NativeJobInfo job = NativeJobInfo.builder()
                .name("job1")
                .command("ping localhost -c 5")
                .cron("0/20 * * * * ?")
                .priority(10)
                .state(NativeJobInfo.State.QUEUED)
                .build();

        given(jobService.getJobInfo("job1")).willReturn(job);

        mvc.perform(get("/job-service/job-info/job1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("job1")))
                .andExpect(jsonPath("$.command", is("ping localhost -c 5")))
                .andExpect(jsonPath("$.cron", is("0/20 * * * * ?")))
                .andExpect(jsonPath("$.state", is("QUEUED")))
                .andExpect(jsonPath("$.priority", is(10)));
    }

    @Test
    public void testGetJobs() throws Exception {
        List<JobInfo> jobs = ImmutableList.of(
                NativeJobInfo.builder()
                        .name("job1")
                        .command("ping localhost -c 5")
                        .cron("0/20 * * * * ?")
                        .state(NativeJobInfo.State.FAILED)
                        .priority(5)
                        .build(),
                NativeJobInfo.builder()
                        .name("job2")
                        .command("echo 'hello' >> test")
                        .cron("0/30 * * * * ?")
                        .state(NativeJobInfo.State.RUNNING)
                        .priority(10)
                        .build()
        );

        given(jobService.getAllJobs()).willReturn(jobs);

        mvc.perform(get("/job-service/jobs")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))

                .andExpect(jsonPath("$[0].name", is("job1")))
                .andExpect(jsonPath("$[0].command", is("ping localhost -c 5")))
                .andExpect(jsonPath("$[0].cron", is("0/20 * * * * ?")))
                .andExpect(jsonPath("$[0].state", is("FAILED")))
                .andExpect(jsonPath("$[0].priority", is(5)))

                .andExpect(jsonPath("$[1].name", is("job2")))
                .andExpect(jsonPath("$[1].command", is("echo 'hello' >> test")))
                .andExpect(jsonPath("$[1].cron", is("0/30 * * * * ?")))
                .andExpect(jsonPath("$[1].state", is("RUNNING")))
                .andExpect(jsonPath("$[1].priority", is(10)));
    }
}
