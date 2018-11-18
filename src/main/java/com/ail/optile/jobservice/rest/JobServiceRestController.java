package com.ail.optile.jobservice.rest;

import com.ail.optile.jobservice.domain.Job;
import com.ail.optile.jobservice.dto.JobInfo;
import com.ail.optile.jobservice.dto.JobRequest;
import com.ail.optile.jobservice.service.JobService;
import lombok.extern.slf4j.Slf4j;
import org.dozer.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/job-service")
@Slf4j
public class JobServiceRestController {

    private final JobService jobService;
    private final Mapper mapper;

    @Autowired
    public JobServiceRestController(JobService jobService, Mapper mapper) {
        this.jobService = jobService;
        this.mapper = mapper;
    }

    @PostMapping("/create")
    public void createJob(@RequestBody JobRequest jobRequest) {
        jobService.create(mapper.map(jobRequest, Job.class));
    }

    @PostMapping("/execute/{jobName}")
    public void executeJob(@PathVariable String jobName) {
        jobService.execute(jobName);
    }

    @PostMapping("/update")
    public void executeJob(@RequestBody JobRequest updateJobRequest) {
        jobService.update(mapper.map(updateJobRequest, Job.class));
    }

    @PostMapping("/delete/{jobName}")
    public void deleteJob(@PathVariable String jobName) {
        jobService.delete(jobName);
    }

    @GetMapping("/job-info/{jobName}")
    public JobInfo getJob(@PathVariable String jobName) {
        return mapper.map(jobService.getJobInfo(jobName), JobInfo.class);
    }

    @GetMapping("/jobs")
    public List<JobInfo> getAllJobs() {
        return jobService.getAllJobs().stream()
                .map(job -> mapper.map(job, JobInfo.class))
                .collect(Collectors.toList());
    }
}
