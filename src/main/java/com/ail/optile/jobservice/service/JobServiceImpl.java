package com.ail.optile.jobservice.service;

import com.ail.optile.jobservice.domain.Job;
import com.ail.optile.jobservice.exception.JobIsCurrentlyRunningException;
import com.ail.optile.jobservice.exception.JobNotFoundException;
import com.ail.optile.jobservice.exception.JobServiceException;
import com.ail.optile.jobservice.pdo.JobExecutionHistory;
import com.ail.optile.jobservice.quartz.NativeJob;
import com.ail.optile.jobservice.repository.JobExecutionHistoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.quartz.impl.matchers.GroupMatcher;
import org.quartz.utils.Key;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


@Slf4j
public class JobServiceImpl implements JobService {

    private final Scheduler scheduler;
    private final JobExecutionHistoryRepository historyRepository;

    @Autowired
    public JobServiceImpl(Scheduler scheduler, JobExecutionHistoryRepository historyRepository) {
        this.scheduler = scheduler;
        this.historyRepository = historyRepository;
    }

    @Override
    public void create(Job job) {
        JobDetail jobDetail = buildJobDetail(job);
        Trigger trigger = buildCronTrigger(job);

        try {
            scheduler.scheduleJob(jobDetail, trigger);
            log.info("Job '{}' created", job.getName());
        } catch (SchedulerException e) {
            log.error(e.getMessage(), e);
            throw new JobServiceException(e);
        }
    }

    @Override
    public void execute(String jobName) {
        try {
            checkJobExists(jobName);

            for (JobExecutionContext jobExecutionContext : scheduler.getCurrentlyExecutingJobs()) {
                if (Objects.equals(jobName, jobExecutionContext.getJobDetail().getKey().getName())) {
                    log.error("Job '{}' is currently running", jobName);
                    throw new JobIsCurrentlyRunningException();
                }
            }

            scheduler.triggerJob(JobKey.jobKey(jobName, Key.DEFAULT_GROUP));
            log.info("Job '{}' executed", jobName);
        } catch (SchedulerException e) {
            log.error(e.getMessage(), e);
            throw new JobServiceException(e);
        }
    }

    @Override
    @Transactional
    public void update(Job job) {
        try {
            checkJobExists(job.getName());

            scheduler.deleteJob(JobKey.jobKey(job.getName(), Key.DEFAULT_GROUP));

            JobDetail jobDetail = buildJobDetail(job);
            Trigger trigger = buildCronTrigger(job);

            scheduler.scheduleJob(jobDetail, trigger);
            log.info("Job '{}' updated", job.getName());
        } catch (SchedulerException e) {
            log.error(e.getMessage(), e);
            throw new JobServiceException(e);
        }
    }

    @Override
    @Transactional
    public void delete(String jobName) {
        try {
            checkJobExists(jobName);

            scheduler.deleteJob(JobKey.jobKey(jobName, Key.DEFAULT_GROUP));
            historyRepository.deleteAllByJobName(jobName);
            log.info("Job '{}' deleted", jobName);
        } catch (SchedulerException e) {
            log.error(e.getMessage(), e);
            throw new JobServiceException(e);
        }
    }

    @Override
    public Job getJobInfo(String jobName) {
        try {
            checkJobExists(jobName);
            return getJob(JobKey.jobKey(jobName, Key.DEFAULT_GROUP));
        } catch (SchedulerException e) {
            log.error(e.getMessage(), e);
            throw new JobServiceException(e);
        }
    }

    @Override
    public List<Job> getAllJobs() {
        try {
            List<Job> jobs = new ArrayList<>();

            for (JobKey jobKey : scheduler.getJobKeys(GroupMatcher.groupEquals(Key.DEFAULT_GROUP))) {
                jobs.add(getJob(jobKey));
            }

            return jobs;
        } catch (SchedulerException e) {
            log.error(e.getMessage(), e);
            throw new JobServiceException(e);
        }
    }

    private void checkJobExists(String jobName) throws SchedulerException {
        if (!scheduler.checkExists(JobKey.jobKey(jobName, Key.DEFAULT_GROUP))) {
            throw new JobNotFoundException();
        }
    }

    private CronTrigger buildCronTrigger(Job job) {
        return TriggerBuilder.newTrigger()
                .withSchedule(CronScheduleBuilder.cronSchedule(job.getCron()))
                .withPriority(job.getPriority() != null ? job.getPriority() : Trigger.DEFAULT_PRIORITY)
                .build();
    }

    private JobDetail buildJobDetail(Job job) {
        return JobBuilder.newJob(NativeJob.class)
                .withIdentity(job.getName(), Key.DEFAULT_GROUP)
                .usingJobData(NativeJob.PROP_COMMAND, job.getCommand())
                .usingJobData(NativeJob.PROP_CONSUME_STREAMS, true)
                .build();
    }

    private Job getJob(JobKey jobKey) throws SchedulerException {
        String jobName = jobKey.getName();
        String jobCommand = getJobCommand(jobKey);
        String jobCron = getJobCron(jobKey);
        Integer jobPriority = getJobPriority(jobKey);
        Job.State jobState = getJobState(jobKey);

        return Job.builder()
                .name(jobName)
                .command(jobCommand)
                .cron(jobCron)
                .state(jobState)
                .priority(jobPriority)
                .build();
    }

    private Job.State getJobState(JobKey jobKey) throws SchedulerException {
        if (isJobRunning(jobKey)) {
            return Job.State.RUNNING;
        }

        JobExecutionHistory jobExecutionHistory =
                historyRepository.findTopByJobNameOrderByCompletionDateDesc(jobKey.getName());

        if (Objects.nonNull(jobExecutionHistory)) {
            JobExecutionHistory.Result result = jobExecutionHistory.getResult();
            switch (result) {
                case SUCCESS:
                    return Job.State.SUCCESS;
                case FAILED:
                    return Job.State.FAILED;
            }
        }

        return Job.State.QUEUED;
    }

    private boolean isJobRunning(JobKey jobKey) throws SchedulerException {
        List<JobExecutionContext> currentlyExecutingJobs = scheduler.getCurrentlyExecutingJobs();
        for (JobExecutionContext jobExecutionContext : currentlyExecutingJobs) {
            String jobName = jobExecutionContext.getJobDetail().getKey().getName();
            String groupName = jobExecutionContext.getJobDetail().getKey().getGroup();
            if (Objects.equals(jobKey.getName(), jobName) && Objects.equals(jobKey.getGroup(), groupName)) {
                return true;
            }
        }
        return false;
    }

    private String getJobCommand(JobKey jobKey) throws SchedulerException {
        JobDetail jobDetail = scheduler.getJobDetail(jobKey);
        JobDataMap jobDataMap = jobDetail.getJobDataMap();
        return (String) jobDataMap.get(NativeJob.PROP_COMMAND);
    }

    @SuppressWarnings("unchecked")
    private String getJobCron(JobKey jobKey) throws SchedulerException {
        List<Trigger> triggers = (List<Trigger>) scheduler.getTriggersOfJob(jobKey);
        for (Trigger trigger : triggers) {
            if (trigger instanceof CronTrigger) {
                CronTrigger cronTrigger = (CronTrigger) trigger;
                return cronTrigger.getCronExpression();
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private Integer getJobPriority(JobKey jobKey) throws SchedulerException {
        return scheduler.getTriggersOfJob(jobKey).get(0).getPriority();
    }
}
