package com.ail.optile.jobservice.service;

import com.ail.optile.jobservice.domain.Job;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface JobService {

    void create(Job job);

    void execute(String jobName);

    void update(Job job);

    void delete(String jobName);

    Job getJobInfo(String jobName);

    List<Job> getAllJobs();
}
