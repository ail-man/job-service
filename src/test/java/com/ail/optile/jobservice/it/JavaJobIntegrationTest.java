package com.ail.optile.jobservice.it;

import com.ail.optile.jobservice.api.JobRequest;
import com.ail.optile.jobservice.api.JobService;
import com.ail.optile.jobservice.api.exception.JobIsCurrentlyRunningException;
import com.ail.optile.jobservice.api.exception.JobIsNotFoundException;
import com.ail.optile.jobservice.domain.JavaJobInfo;
import com.ail.optile.jobservice.domain.JavaJobRequest;
import com.ail.optile.jobservice.domain.NativeJobInfo;
import com.ail.optile.jobservice.domain.exception.ExecutionException;
import com.ail.optile.jobservice.domain.exception.RollbackException;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import static junit.framework.TestCase.fail;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@RunWith(SpringRunner.class)
@SpringBootTest
@TestPropertySource("classpath:application_test.properties")
@Slf4j
public class JavaJobIntegrationTest {

    @Autowired
    private JobService jobService;

    // TODO more tests

    @Before
    public void init() {
        jobService.getAllJobs().forEach(job -> {
            try {
                jobService.delete(job.getName());
            } catch (JobIsNotFoundException | JobIsCurrentlyRunningException e) {
                log.error(e.getMessage(), e);
                fail("This should not happen");
            }
        });
    }

    @Test
    public void testJobLifecycle() throws Exception {
        final String jobName = "job1";

        JobRequest jobRequest = JavaJobRequestImpl.builder()
                .name(jobName)
                .cron("0 0 0 1 JAN ? 2099-2099")
                .build();

        jobService.create(jobRequest);

        JavaJobInfo jobInfo = (JavaJobInfo) jobService.getJobInfo(jobName);
        assertThat(jobInfo.getName(), is(jobName));
        assertThat(jobInfo.getCron(), is("0 0 0 1 JAN ? 2099-2099"));
        assertThat(jobInfo.getPriority(), is(5)); // default priority
        assertThat(jobInfo.getState(), is(NativeJobInfo.State.QUEUED)); // job was not run yet

        jobService.execute(jobName);

        Thread.sleep(500);

        jobInfo = (JavaJobInfo) jobService.getJobInfo(jobName);
        assertThat(jobInfo.getState(), is(NativeJobInfo.State.FAILED));

        Thread.sleep(500);

        jobService.delete(jobName);
    }

    @NoArgsConstructor
    public static final class JavaJobRequestImpl extends JavaJobRequest {

        @Builder
        public JavaJobRequestImpl(
                String name,
                String cron,
                Integer priority) {
            super(name, cron, priority);
        }

        @Override
        public void executeJob(JobExecutionContext jobExecutionContext) throws ExecutionException {
            System.out.println("!!!TEST EXECUTE!!!");
            throw new ExecutionException("OOPS!!!");
        }

        @Override
        public void rollbackJob(JobExecutionContext jobExecutionContext) throws RollbackException {
            System.out.println("!!!TEST ROLLBACK!!!");
        }
    }
}

