package com.ail.optile.jobservice.api;

import com.ail.optile.jobservice.api.exception.IncorrectJobRequestException;
import com.ail.optile.jobservice.api.exception.JobAlreadyExistsException;
import com.ail.optile.jobservice.api.exception.JobIsCurrentlyRunningException;
import com.ail.optile.jobservice.api.exception.JobIsNotFoundException;

import java.util.List;

public interface JobService {

    /**
     * Registers new job in this Job Service.
     *
     * @param jobRequest request object to register as a job
     * @throws IncorrectJobRequestException is thrown when the request is in incorrect format
     * @throws JobAlreadyExistsException    is thrown when job with {@link JobRequest#getName()} already exists
     */
    void create(JobRequest jobRequest) throws IncorrectJobRequestException, JobAlreadyExistsException;

    /**
     * Executes the job registered in this Job Service immediately.
     *
     * @param jobName the name of the job to be executed
     * @throws JobIsNotFoundException         is thrown when the job with name {@link JobRequest#getName()}
     *                                        isn't registered in the Job Service
     * @throws JobIsCurrentlyRunningException is thrown when the job is currently running.
     */
    void execute(String jobName) throws JobIsNotFoundException, JobIsCurrentlyRunningException;

    /**
     * Updates the job with incoming job name.
     * The {@link JobRequest#getName()} in jobRequest should return the name of the job registered in Job Service.
     *
     * @param jobRequest request object to update existing job
     * @throws JobIsNotFoundException         is thrown when the job with name {@link JobRequest#getName()}
     *                                        isn't registered in the Job Service
     * @throws IncorrectJobRequestException   is thrown when the request is in incorrect format
     * @throws JobIsCurrentlyRunningException is thrown when job currently is in RUNNING state
     */
    void update(JobRequest jobRequest) throws JobIsNotFoundException, IncorrectJobRequestException, JobIsCurrentlyRunningException;

    /**
     * Deletes the job registered in Job Service by the job name
     *
     * @param jobName name of a job to remove
     * @throws JobIsNotFoundException         is thrown when the job with incoming jobName isn't registered in the Job Service
     * @throws JobIsCurrentlyRunningException is thrown when job currently is in RUNNING state
     */
    void delete(String jobName) throws JobIsNotFoundException, JobIsCurrentlyRunningException;

    /**
     * Fetches info for the job registered in Job Service.
     *
     * @param jobName the name of the job to get info
     * @return info object
     * @throws JobIsNotFoundException is thrown when the job with incoming jobName isn't registered in the Job Service
     */
    JobInfo getJobInfo(String jobName) throws JobIsNotFoundException;

    /**
     * Fetches all jobs registered in Job Service.
     *
     * @return list of job info objects
     */
    List<JobInfo> getAllJobs();
}
