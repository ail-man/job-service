package com.ail.optile.jobservice.service;

import com.ail.optile.jobservice.api.JobInfo;
import com.ail.optile.jobservice.api.JobRequest;
import com.ail.optile.jobservice.api.JobService;
import com.ail.optile.jobservice.api.exception.*;
import com.ail.optile.jobservice.domain.JavaJobInfo;
import com.ail.optile.jobservice.domain.NativeJobInfo;
import com.ail.optile.jobservice.domain.NativeJobRequest;
import com.ail.optile.jobservice.pdo.JobExecutionHistory;
import com.ail.optile.jobservice.quartz.NativeJob;
import com.ail.optile.jobservice.repository.JobExecutionHistoryRepository;
import com.ail.optile.jobservice.rest.exception.UnexpectedRestException;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.quartz.impl.matchers.GroupMatcher;
import org.quartz.utils.Key;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
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
    @Transactional
    public void create(JobRequest jobRequest) throws IncorrectJobRequestException, JobAlreadyExistsException {
        if (checkJobExists(jobRequest.getName())) {
            log.error("Job '{}' is already exists", jobRequest.getName());
            throw new JobAlreadyExistsException();
        }

        JobDetail jobDetail = buildJobDetail(jobRequest);
        Trigger trigger = buildCronTrigger(jobRequest);

        try {
            scheduler.scheduleJob(jobDetail, trigger);
            log.info("Job '{}' created", jobRequest.getName());
        } catch (SchedulerException e) {
            log.error(e.getMessage(), e);
            throw new UnexpectedException(e);
        }
    }

    @Override
    public void execute(String jobName) throws JobIsNotFoundException, JobIsCurrentlyRunningException {
        throwJobIsNotFoundExceptionIfJobNotExists(jobName);

        try {
            throwExceptionIfRunning(jobName);

            scheduler.triggerJob(JobKey.jobKey(jobName, Key.DEFAULT_GROUP));
            log.info("Job '{}' executed", jobName);
        } catch (SchedulerException e) {
            log.error(e.getMessage(), e);
            throw new UnexpectedException(e);
        }
    }

    private void throwExceptionIfRunning(String jobName) throws SchedulerException, JobIsCurrentlyRunningException {
        if (checkJobIsCurrentlyRunning(jobName)) {
            log.error("Job '{}' is currently running", jobName);
            throw new JobIsCurrentlyRunningException();
        }
    }

    private boolean checkJobIsCurrentlyRunning(String jobName) throws SchedulerException {
        for (JobExecutionContext jobExecutionContext : scheduler.getCurrentlyExecutingJobs()) {
            if (Objects.equals(jobName, jobExecutionContext.getJobDetail().getKey().getName())) {
                return true;
            }
        }
        return false;
    }

    private void throwJobIsNotFoundExceptionIfJobNotExists(String jobName) throws JobIsNotFoundException {
        if (!checkJobExists(jobName)) {
            log.error("Job '{}' is not found", jobName);
            throw new JobIsNotFoundException();
        }
    }

    @Override
    @Transactional
    public void update(JobRequest jobRequest) throws JobIsNotFoundException, IncorrectJobRequestException, JobIsCurrentlyRunningException {
        throwJobIsNotFoundExceptionIfJobNotExists(jobRequest.getName());

        try {
            throwExceptionIfRunning(jobRequest.getName());
            scheduler.deleteJob(JobKey.jobKey(jobRequest.getName(), Key.DEFAULT_GROUP));
        } catch (SchedulerException e) {
            log.error(e.getMessage(), e);
            throw new UnexpectedException(e);
        }

        JobDetail jobDetail = buildJobDetail(jobRequest);
        Trigger trigger = buildCronTrigger(jobRequest);

        try {
            scheduler.scheduleJob(jobDetail, trigger);
            log.info("Job '{}' updated", jobRequest.getName());
        } catch (SchedulerException e) {
            log.error(e.getMessage(), e);
            throw new UnexpectedException(e);
        }
    }

    @Override
    @Transactional
    public void delete(String jobName) throws JobIsNotFoundException, JobIsCurrentlyRunningException {
        throwJobIsNotFoundExceptionIfJobNotExists(jobName);

        try {
            throwExceptionIfRunning(jobName);

            scheduler.deleteJob(JobKey.jobKey(jobName, Key.DEFAULT_GROUP));
            historyRepository.deleteAllByJobName(jobName);
            log.info("Job '{}' deleted", jobName);
        } catch (SchedulerException e) {
            log.error(e.getMessage(), e);
            throw new UnexpectedException(e);
        }
    }

    @Override
    public JobInfo getJobInfo(String jobName) throws JobIsNotFoundException {
        throwJobIsNotFoundExceptionIfJobNotExists(jobName);

        try {
            return getJobInfo(JobKey.jobKey(jobName, Key.DEFAULT_GROUP));
        } catch (SchedulerException e) {
            log.error(e.getMessage(), e);
            throw new UnexpectedRestException(e);
        }
    }

    @Override
    public List<JobInfo> getAllJobs() {
        try {
            List<JobInfo> jobs = new ArrayList<>();

            for (JobKey jobKey : scheduler.getJobKeys(GroupMatcher.groupEquals(Key.DEFAULT_GROUP))) {
                jobs.add(getJobInfo(jobKey));
            }

            return jobs;
        } catch (SchedulerException e) {
            log.error(e.getMessage(), e);
            throw new UnexpectedException(e);
        }
    }

    private JobInfo getJobInfo(JobKey jobKey) throws SchedulerException {
        String jobName = jobKey.getName();
        Class jobClass = getJobClass(jobKey);
        String jobCron = getJobCron(jobKey);
        Integer jobPriority = getJobPriority(jobKey);
        JobInfo.State jobState = getJobState(jobKey);

        if (NativeJob.class.equals(jobClass)) {
            String jobCommand = getJobCommand(jobKey);
            return NativeJobInfo.builder()
                    .name(jobName)
                    .command(jobCommand)
                    .cron(jobCron)
                    .state(jobState)
                    .priority(jobPriority)
                    .build();
        } else {
            return JavaJobInfo.builder()
                    .name(jobName)
                    .cron(jobCron)
                    .state(jobState)
                    .priority(jobPriority)
                    .build();
        }
    }

    private Class getJobClass(JobKey jobKey) throws SchedulerException {
        JobDetail jobDetail = scheduler.getJobDetail(jobKey);
        return jobDetail.getJobClass();
    }

    private JobInfo.State getJobState(JobKey jobKey) throws SchedulerException {
        if (isJobRunning(jobKey)) {
            return JobInfo.State.RUNNING;
        }

        JobExecutionHistory jobExecutionHistory =
                historyRepository.findTopByJobNameOrderByCompletionDateDesc(jobKey.getName());

        if (Objects.nonNull(jobExecutionHistory)) {
            JobExecutionHistory.Result result = jobExecutionHistory.getResult();
            switch (result) {
                case SUCCESS:
                    return JobInfo.State.SUCCESS;
                case FAILED:
                    return JobInfo.State.FAILED;
            }
        }

        return JobInfo.State.QUEUED;
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

    private boolean checkJobExists(String jobName) {
        try {
            return scheduler.checkExists(JobKey.jobKey(jobName, Key.DEFAULT_GROUP));
        } catch (SchedulerException e) {
            log.error(e.getMessage(), e);
            throw new UnexpectedException(e);
        }
    }

    private CronTrigger buildCronTrigger(JobRequest jobRequest) throws IncorrectJobRequestException {
        try {
            return TriggerBuilder.newTrigger()
                    .withSchedule(CronScheduleBuilder.cronSchedule(jobRequest.getCron()))
                    .withPriority(
                            jobRequest.getPriority() != null ? jobRequest.getPriority() : Trigger.DEFAULT_PRIORITY)
                    .build();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new IncorrectJobRequestException(e);
        }
    }

    private JobDetail buildJobDetail(JobRequest jobRequest) throws IncorrectJobRequestException {
        try {
            if (jobRequest instanceof NativeJobRequest) {
                return JobBuilder.newJob(NativeJob.class)
                        .withIdentity(jobRequest.getName(), Key.DEFAULT_GROUP)
                        .usingJobData(NativeJob.PROP_COMMAND, ((NativeJobRequest) jobRequest).getCommand())
                        .usingJobData(NativeJob.PROP_CONSUME_STREAMS, true)
                        .build();
            } else {
                return JobBuilder.newJob(jobRequest.getClass())
                        .withIdentity(jobRequest.getName(), Key.DEFAULT_GROUP)
                        .build();
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new IncorrectJobRequestException(e);
        }
    }
}
