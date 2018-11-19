package com.ail.optile.jobservice.api;

import org.quartz.Job;

/**
 * Represents the request for creating new job in {@link JobService}
 */
public interface JobRequest extends Job {

    int DEFAULT_PRIORITY = 5;

    /**
     * The name of a job to be registered in {@link JobService}. Treated as id of the job.
     *
     * @return name of a job
     */
    String getName();

    /**
     * The cron expression of a job to be registered in {@link JobService}. Defines the schedule of job execution.
     *
     * @return cron expression
     */
    String getCron();

    /**
     * The priority of a job to be registered in {@link JobService}. Defines the priority of a trigger in case when
     * the thread pool in {@link JobService} is exhausted.
     * <p>
     * Default priority is {@link 5}
     *
     * @return priority value
     */
    default Integer getPriority() {
        return DEFAULT_PRIORITY;
    }
}
