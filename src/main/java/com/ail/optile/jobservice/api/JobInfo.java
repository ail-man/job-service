package com.ail.optile.jobservice.api;

/**
 * Represents the job information which can be retrieved from {@link JobService}
 */
public interface JobInfo {

    /**
     * The name of a job registered in {@link JobService}.
     *
     * @return job's name
     */
    String getName();

    /**
     * The cron expression of a job registered in {@link JobService}.
     *
     * @return job's cron expression
     */
    String getCron();

    /**
     * The priority of a job registered in {@link JobService}.
     *
     * @return job's priority value
     */
    Integer getPriority();

    /**
     * The current state of a job registered in {@link JobService}.
     * <p>
     * <p>{@link State#QUEUED} - job wasn't executed and awaits to be triggered.
     * <p>{@link State#RUNNING} - job is currently running.
     * <p>{@link State#SUCCESS} - last job execution was successful.
     * <p>{@link State#FAILED} - last job execution was failed.
     *
     * @return state of a job
     */
    State getState();

    /**
     * Possible Job states
     */
    enum State {
        QUEUED,
        RUNNING,
        SUCCESS,
        FAILED
    }
}
