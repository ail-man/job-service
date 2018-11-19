package com.ail.optile.jobservice.it;

import com.ail.optile.jobservice.api.JobInfo;
import com.ail.optile.jobservice.api.JobService;
import com.ail.optile.jobservice.api.exception.IncorrectJobRequestException;
import com.ail.optile.jobservice.api.exception.JobAlreadyExistsException;
import com.ail.optile.jobservice.api.exception.JobIsCurrentlyRunningException;
import com.ail.optile.jobservice.api.exception.JobIsNotFoundException;
import com.ail.optile.jobservice.domain.NativeJobInfo;
import com.ail.optile.jobservice.domain.NativeJobRequest;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import static com.ail.optile.jobservice.Utils.pingLocalhostCommand;
import static junit.framework.TestCase.fail;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

@RunWith(SpringRunner.class)
@SpringBootTest
@TestPropertySource("classpath:application_test.properties")
@Slf4j
public class NativeJobIntegrationTest {

    @Autowired
    private JobService jobService;

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

    // TODO check that all cases are covered

    @Test
    public void testIncorrectJobRequestException() {
        final String jobName = "job1";

        NativeJobRequest jobRequest = NativeJobRequest.builder()
                .name(jobName)
                .command("dir")
                .cron("dsADAS")
                .build();

        try {
            jobService.create(jobRequest);
            fail("Should throw " + IncorrectJobRequestException.class + " exception");
        } catch (Exception e) {
            assertThat(e.getClass(), equalTo(IncorrectJobRequestException.class));
        }
    }

    @Test
    public void testJobAlreadyExistsException() {
        final String jobName = "job1";

        NativeJobRequest jobRequest = NativeJobRequest.builder()
                .name(jobName)
                .command("dir")
                .cron("0 0 0 1 JAN ? 2099-2099")
                .build();

        try {
            jobService.create(jobRequest);
            jobService.create(jobRequest);
            fail("Should throw " + JobAlreadyExistsException.class + " exception");
        } catch (Exception e) {
            assertThat(e.getClass(), equalTo(JobAlreadyExistsException.class));
        }
    }

    @Test
    public void testJobNotFoundException() {
        assertThat(jobService.getAllJobs().size(), is(0));

        try {
            jobService.getJobInfo("notExisting");
            fail("Should throw " + JobIsNotFoundException.class + " exception");
        } catch (Exception e) {
            assertThat(e.getClass(), equalTo(JobIsNotFoundException.class));
        }

        try {
            jobService.update(NativeJobRequest.builder()
                    .name("notExisting")
                    .command("dir")
                    .cron("0 0 0 1 JAN ? 2099-2099")
                    .build());
            fail("Should throw " + JobIsNotFoundException.class + " exception");
        } catch (Exception e) {
            assertThat(e.getClass(), equalTo(JobIsNotFoundException.class));
        }

        try {
            jobService.execute("notExisting");
            fail("Should throw " + JobIsNotFoundException.class + " exception");
        } catch (Exception e) {
            assertThat(e.getClass(), equalTo(JobIsNotFoundException.class));
        }

        try {
            jobService.delete("notExisting");
            fail("Should throw " + JobIsNotFoundException.class + " exception");
        } catch (Exception e) {
            assertThat(e.getClass(), equalTo(JobIsNotFoundException.class));
        }
    }

    @Test
    public void testJobIsCurrentlyRunningException() throws Exception {
        final String jobName = "job1";

        NativeJobRequest jobRequest = NativeJobRequest.builder()
                .name(jobName)
                .command(pingLocalhostCommand(3))
                .cron("0 0 0 1 JAN ? 2099-2099")
                .build();

        try {
            jobService.create(jobRequest);
            jobService.execute(jobName);
            Thread.sleep(500);
            jobService.execute(jobName);
            fail("Should throw " + JobIsCurrentlyRunningException.class + " exception");
        } catch (Exception e) {
            assertThat(e.getClass(), equalTo(JobIsCurrentlyRunningException.class));
        }

        Thread.sleep(3000);

        try {
            jobService.execute(jobName);
            Thread.sleep(500);
            jobService.update(jobRequest);
            fail("Should throw " + JobIsCurrentlyRunningException.class + " exception");
        } catch (Exception e) {
            assertThat(e.getClass(), equalTo(JobIsCurrentlyRunningException.class));
        }

        Thread.sleep(3000);

        try {
            jobService.execute(jobName);
            Thread.sleep(500);
            jobService.delete(jobName);
            fail("Should throw " + JobIsCurrentlyRunningException.class + " exception");
        } catch (Exception e) {
            assertThat(e.getClass(), equalTo(JobIsCurrentlyRunningException.class));
        }

        Thread.sleep(3000);
    }

    @Test(expected = JobIsNotFoundException.class)
    public void testJobLifecycle() throws Exception {
        final String jobName = "job1";

        NativeJobRequest jobRequest = NativeJobRequest.builder()
                .name(jobName)
                .command(pingLocalhostCommand(4))
                .cron("0 0 0 1 JAN ? 2099-2099") // run once on the January 1st, 2099
                .build();

        jobService.create(jobRequest);

        NativeJobInfo jobInfo = (NativeJobInfo) jobService.getJobInfo(jobName);
        assertThat(jobInfo.getName(), is(jobName));
        assertThat(jobInfo.getCommand(), is(pingLocalhostCommand(4)));
        assertThat(jobInfo.getCron(), is("0 0 0 1 JAN ? 2099-2099"));
        assertThat(jobInfo.getPriority(), is(5)); // default priority
        assertThat(jobInfo.getState(), is(NativeJobInfo.State.QUEUED)); // job was not run yet

        jobService.execute(jobName);

        Thread.sleep(1000);

        jobInfo = (NativeJobInfo) jobService.getJobInfo(jobName);
        assertThat(jobInfo.getState(), is(NativeJobInfo.State.RUNNING)); // job is currently running

        Thread.sleep(5000);

        jobInfo = (NativeJobInfo) jobService.getJobInfo(jobName);
        assertThat(jobInfo.getState(), is(NativeJobInfo.State.SUCCESS)); // last job run finished successfully

        jobRequest.setCommand("0123456789abcdef");
        jobRequest.setCron("0 0 0 1 FEB ? 2098-2098");
        jobRequest.setPriority(10);

        jobService.update(jobRequest);

        jobInfo = (NativeJobInfo) jobService.getJobInfo(jobName);
        assertThat(jobInfo.getCommand(), is("0123456789abcdef"));
        assertThat(jobInfo.getCron(), is("0 0 0 1 FEB ? 2098-2098"));
        assertThat(jobInfo.getPriority(), is(10));

        jobService.execute(jobName);
        Thread.sleep(1000);

        jobInfo = (NativeJobInfo) jobService.getJobInfo(jobName);
        assertThat(jobInfo.getState(), is(NativeJobInfo.State.FAILED)); // last job run finished with error

        jobService.delete(jobName);

        jobService.getJobInfo(jobName);
    }

    @Test
    public void testGetAllJobs() throws Exception {
        List<JobInfo> allJobs = jobService.getAllJobs();

        assertThat(allJobs.size(), is(0));

        NativeJobRequest job1 = NativeJobRequest.builder()
                .name("job1")
                .command("dir")
                .cron("0 0 0 1 JAN ? 2099-2099")
                .build();

        NativeJobRequest job2 = NativeJobRequest.builder()
                .name("job2")
                .command(pingLocalhostCommand(4))
                .cron("0 0 0 1 FEB ? 2098-2098")
                .priority(10)
                .build();

        jobService.create(job1);
        jobService.execute("job1");
        jobService.create(job2);

        Thread.sleep(500);

        allJobs = jobService.getAllJobs();
        assertThat(allJobs.size(), is(2));

        int hitCount = 0;
        for (JobInfo jobInfo : allJobs) {
            NativeJobInfo nativeJobInfo = (NativeJobInfo) jobInfo;
            if (Objects.equals(jobInfo.getName(), "job1")) {
                assertThat(nativeJobInfo.getCommand(), is("dir"));
                assertThat(nativeJobInfo.getCron(), is("0 0 0 1 JAN ? 2099-2099"));
                assertThat(nativeJobInfo.getPriority(), is(5)); // default priority
                assertThat(nativeJobInfo.getState(), is(NativeJobInfo.State.SUCCESS)); // job was not run yet
                hitCount++;
            }
            if (Objects.equals(jobInfo.getName(), "job2")) {
                assertThat(nativeJobInfo.getCommand(), is(pingLocalhostCommand(4)));
                assertThat(nativeJobInfo.getCron(), is("0 0 0 1 FEB ? 2098-2098"));
                assertThat(nativeJobInfo.getPriority(), is(10)); // default priority
                assertThat(nativeJobInfo.getState(), is(NativeJobInfo.State.QUEUED)); // job was not run yet
                hitCount++;
            }
        }
        assertThat(hitCount, is(2));

        jobService.delete("job1");
        jobService.delete("job2");

        allJobs = jobService.getAllJobs();
        assertThat(allJobs.size(), is(0));
    }

    // TODO test via REST with full application up
    @Ignore
    @Test
    public void testSheduling() throws Exception {
        assertThat(jobService.getAllJobs().size(), is(0));

        String jobName = "job1";

        NativeJobRequest job1 = NativeJobRequest.builder()
                .name(jobName)
                .command(pingLocalhostCommand(5))
                .cron("0/10 * * ? * * *")
                .build();


        log.info("[SHCEDULING_LOG] Current time: {}", LocalDateTime.now());

        int currentSeconds = LocalDateTime.now().getSecond();
        int secondsToWait = 11 - (currentSeconds) % 10;

        log.info("[SHCEDULING_LOG] Sleep for {} seconds", secondsToWait);
        Thread.sleep(secondsToWait * 1000L);

        log.info("[SHCEDULING_LOG] Creating jobs: {}", LocalDateTime.now());
        jobService.create(job1);
        log.info("[SHCEDULING_LOG] Jobs are created: {}", LocalDateTime.now());

        currentSeconds = LocalDateTime.now().getSecond();
        secondsToWait = 11 - currentSeconds % 10;

        log.info("[SHCEDULING_LOG] Sleep for {} seconds", secondsToWait);
        Thread.sleep(secondsToWait * 1000L);

        JobInfo jobInfo = jobService.getJobInfo(jobName);

        assertThat(jobInfo.getState(), is(NativeJobInfo.State.RUNNING));

        Thread.sleep(10000);
    }

    // TODO test via REST with full application up
    @Ignore
    @Test
    public void testPriorityExecution() throws Exception {
        assertThat(jobService.getAllJobs().size(), is(0));

        NativeJobRequest job1 = NativeJobRequest.builder()
                .name("job1")
                .command(pingLocalhostCommand(10))
                .cron("0/20 * * ? * * *")
                .priority(5)
                .build();

        NativeJobRequest job2 = NativeJobRequest.builder()
                .name("job2")
                .command(pingLocalhostCommand(10))
                .cron("0/20 * * ? * * *")
                .priority(10)
                .build();

        NativeJobRequest job3 = NativeJobRequest.builder()
                .name("job3")
                .command(pingLocalhostCommand(10))
                .cron("0/20 * * ? * * *")
                .priority(15)
                .build();

        log.info("[PRIORITY_LOG] Current time: {}", LocalDateTime.now());

        int currentSeconds = LocalDateTime.now().getSecond();
        int secondsToWait = 20 - (currentSeconds) % 20 + 1;

        log.info("[PRIORITY_LOG] Sleep for {} seconds", secondsToWait);
        Thread.sleep(secondsToWait * 1000L);
        log.info("[PRIORITY_LOG] Creating jobs: {}", LocalDateTime.now());

        jobService.create(job1);
        jobService.create(job2);
        jobService.create(job3);

        log.info("[PRIORITY_LOG] Jobs are created: {}", LocalDateTime.now());

        currentSeconds = LocalDateTime.now().getSecond();
        secondsToWait = 20 - currentSeconds % 20 + 2;

        log.info("[PRIORITY_LOG] Sleep for {} seconds", secondsToWait);
        Thread.sleep(secondsToWait * 1000L);

        List<JobInfo> allJobs = jobService.getAllJobs();
        assertThat(allJobs.size(), is(3));

        int runningJobs = 0;
        for (JobInfo jobInfo : allJobs) {
            if (Objects.equals(jobInfo.getState(), NativeJobInfo.State.RUNNING)) {
                runningJobs++;
            }
        }
        assertThat(runningJobs, is(1));

        int hitCount = 0;
        for (JobInfo jobInfo : allJobs) {
            if (Objects.equals(jobInfo.getName(), "job1")) {
                assertThat(jobInfo.getState(), is(NativeJobInfo.State.QUEUED));
                hitCount++;
            }
            if (Objects.equals(jobInfo.getName(), "job2")) {
                assertThat(jobInfo.getState(), is(NativeJobInfo.State.QUEUED));
                hitCount++;
            }
            if (Objects.equals(jobInfo.getName(), "job3")) {
                assertThat(jobInfo.getState(), is(NativeJobInfo.State.RUNNING));
                hitCount++;
            }
        }
        assertThat(hitCount, is(3));

        Thread.sleep(20000);
    }
}
