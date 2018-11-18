package com.ail.optile.jobservice.it;

import com.ail.optile.jobservice.domain.Job;
import com.ail.optile.jobservice.exception.JobNotFoundException;
import com.ail.optile.jobservice.service.JobService;
import lombok.extern.slf4j.Slf4j;
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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@RunWith(SpringRunner.class)
@SpringBootTest
@TestPropertySource("classpath:application_test.properties")
@Slf4j
public class JobServiceIntegrationTest {

    @Autowired
    private JobService jobService;

    @Test(expected = JobNotFoundException.class)
    public void testJobLifecycle() throws Exception {
        final String jobName = "job1";

        Job job = Job.builder()
                .name(jobName)
                .command(pingLocalhostCommand(4))
                .cron("0 0 0 1 JAN ? 2099-2099") // run once on the January 1st, 2099
                .build();

        jobService.create(job);

        Job jobInfo = jobService.getJobInfo(jobName);
        assertThat(jobInfo.getName(), is(jobName));
        assertThat(jobInfo.getCommand(), is(pingLocalhostCommand(4)));
        assertThat(jobInfo.getCron(), is("0 0 0 1 JAN ? 2099-2099"));
        assertThat(jobInfo.getPriority(), is(5)); // default priority
        assertThat(jobInfo.getState(), is(Job.State.QUEUED)); // job was not run yet

        jobService.execute(jobName);

        Thread.sleep(1000);

        jobInfo = jobService.getJobInfo(jobName);
        assertThat(jobInfo.getState(), is(Job.State.RUNNING)); // job is currently running

        Thread.sleep(5000);

        jobInfo = jobService.getJobInfo(jobName);
        assertThat(jobInfo.getState(), is(Job.State.SUCCESS)); // last job run finished successfully

        job.setCommand("0123456789abcdef");
        job.setCron("0 0 0 1 FEB ? 2098-2098");
        job.setPriority(10);

        jobService.update(job);

        jobInfo = jobService.getJobInfo(jobName);
        assertThat(jobInfo.getCommand(), is("0123456789abcdef"));
        assertThat(jobInfo.getCron(), is("0 0 0 1 FEB ? 2098-2098"));
        assertThat(jobInfo.getPriority(), is(10));

        jobService.execute(jobName);
        Thread.sleep(1000);

        jobInfo = jobService.getJobInfo(jobName);
        assertThat(jobInfo.getState(), is(Job.State.FAILED)); // last job run finished with error

        jobService.delete(jobName);

        jobService.getJobInfo(jobName);
    }

    @Test
    public void testGetAllJobs() {
        List<Job> allJobs = jobService.getAllJobs();

        assertThat(allJobs.size(), is(0));

        Job job1 = Job.builder()
                .name("job1")
                .command("dir")
                .cron("0 0 0 1 JAN ? 2099-2099")
                .build();

        Job job2 = Job.builder()
                .name("job2")
                .command(pingLocalhostCommand(4))
                .cron("0 0 0 1 FEB ? 2098-2098")
                .priority(10)
                .build();

        jobService.create(job1);
        jobService.execute("job1");
        jobService.create(job2);

        allJobs = jobService.getAllJobs();
        assertThat(allJobs.size(), is(2));

        int hitCount = 0;
        for (Job jobInfo : allJobs) {
            if (Objects.equals(jobInfo.getName(), "job1")) {
                assertThat(jobInfo.getCommand(), is("dir"));
                assertThat(jobInfo.getCron(), is("0 0 0 1 JAN ? 2099-2099"));
                assertThat(jobInfo.getPriority(), is(5)); // default priority
                assertThat(jobInfo.getState(), is(Job.State.SUCCESS)); // job was not run yet
                hitCount++;
            }
            if (Objects.equals(jobInfo.getName(), "job2")) {
                assertThat(jobInfo.getCommand(), is(pingLocalhostCommand(4)));
                assertThat(jobInfo.getCron(), is("0 0 0 1 FEB ? 2098-2098"));
                assertThat(jobInfo.getPriority(), is(10)); // default priority
                assertThat(jobInfo.getState(), is(Job.State.QUEUED)); // job was not run yet
                hitCount++;
            }
        }
        assertThat(hitCount, is(2));

        jobService.delete("job1");
        jobService.delete("job2");

        allJobs = jobService.getAllJobs();
        assertThat(allJobs.size(), is(0));
    }

    @Test
    public void testShedulingAndPriorityExecution() throws Exception {
        assertThat(jobService.getAllJobs().size(), is(0));

        Job job1 = Job.builder()
                .name("job1")
                .command(pingLocalhostCommand(10))
                .cron("0/20 * * ? * * *")
                .priority(5)
                .build();

        Job job2 = Job.builder()
                .name("job2")
                .command(pingLocalhostCommand(10))
                .cron("0/20 * * ? * * *")
                .priority(10)
                .build();

        Job job3 = Job.builder()
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

        List<Job> allJobs = jobService.getAllJobs();
        assertThat(allJobs.size(), is(3));

        int runningJobs = 0;
        for (Job jobInfo : allJobs) {
            if (Objects.equals(jobInfo.getState(), Job.State.RUNNING)) {
                runningJobs++;
            }
        }
        assertThat(runningJobs, is(1));

//        int hitCount = 0;
//        for (Job jobInfo : allJobs) {
//            if (Objects.equals(jobInfo.getName(), "job1")) {
//                assertThat(jobInfo.getState(), is(Job.State.QUEUED));
//                hitCount++;
//            }
//            if (Objects.equals(jobInfo.getName(), "job2")) {
//                assertThat(jobInfo.getState(), is(Job.State.QUEUED));
//                hitCount++;
//            }
//            if (Objects.equals(jobInfo.getName(), "job3")) {
//                assertThat(jobInfo.getState(), is(Job.State.RUNNING));
//                hitCount++;
//            }
//        }
//        assertThat(hitCount, is(3));
    }
}
