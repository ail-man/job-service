package com.ail.optile.jobservice.rest;

import com.ail.optile.jobservice.api.JobService;
import com.ail.optile.jobservice.api.exception.*;
import com.ail.optile.jobservice.domain.NativeJobRequest;
import com.ail.optile.jobservice.rest.dto.NativeJobInfoDto;
import com.ail.optile.jobservice.rest.dto.NativeJobRequestDto;
import com.ail.optile.jobservice.rest.exception.*;
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
    public void createJob(@RequestBody NativeJobRequestDto jobRequest) {
        try {
            jobService.create(mapper.map(jobRequest, NativeJobRequest.class));
        } catch (IncorrectJobRequestException e) {
            log.error(e.getMessage(), e);
            throw new IncorrectJobRequestRestException(e);
        } catch (JobAlreadyExistsException e) {
            log.error(e.getMessage());
            throw new JobAlreadyExistsRestException(e);
        } catch (UnexpectedException e) {
            log.error(e.getMessage(), e);
            throw new UnexpectedRestException(e);
        }
    }

    @PostMapping("/execute/{jobName}")
    public void executeJob(@PathVariable String jobName) {
        try {
            jobService.execute(jobName);
        } catch (JobIsNotFoundException e) {
            log.error(e.getMessage(), e);
            throw new JobIsNotFoundRestException(e);
        } catch (JobIsCurrentlyRunningException e) {
            log.error(e.getMessage(), e);
            throw new JobIsCurrentlyRunningRestException(e);
        } catch (UnexpectedException e) {
            log.error(e.getMessage(), e);
            throw new UnexpectedRestException(e);
        }
    }

    @PostMapping("/update")
    public void updateJob(@RequestBody NativeJobRequestDto updateJobRequest) {
        try {
            jobService.update(mapper.map(updateJobRequest, NativeJobRequest.class));
        } catch (JobIsNotFoundException e) {
            log.error(e.getMessage(), e);
            throw new JobIsNotFoundRestException(e);
        } catch (JobIsCurrentlyRunningException e) {
            log.error(e.getMessage(), e);
            throw new JobIsCurrentlyRunningRestException(e);
        } catch (IncorrectJobRequestException e) {
            log.error(e.getMessage(), e);
            throw new IncorrectJobRequestRestException(e);
        } catch (UnexpectedException e) {
            log.error(e.getMessage(), e);
            throw new UnexpectedRestException(e);
        }
    }

    @PostMapping("/delete/{jobName}")
    public void deleteJob(@PathVariable String jobName) {
        try {
            jobService.delete(jobName);
        } catch (JobIsNotFoundException e) {
            log.error(e.getMessage(), e);
            throw new JobIsNotFoundRestException(e);
        } catch (JobIsCurrentlyRunningException e) {
            log.error(e.getMessage(), e);
            throw new JobIsCurrentlyRunningRestException(e);
        } catch (UnexpectedException e) {
            log.error(e.getMessage(), e);
            throw new UnexpectedRestException(e);
        }
    }

    @GetMapping("/job-info/{jobName}")
    public NativeJobInfoDto getJob(@PathVariable String jobName) {
        try {
            return mapper.map(jobService.getJobInfo(jobName), NativeJobInfoDto.class);
        } catch (JobIsNotFoundException e) {
            log.error(e.getMessage(), e);
            throw new JobIsNotFoundRestException(e);
        } catch (UnexpectedException e) {
            log.error(e.getMessage(), e);
            throw new UnexpectedRestException(e);
        }
    }

    @GetMapping("/jobs")
    public List<NativeJobInfoDto> getAllJobs() {
        try {
            return jobService.getAllJobs().stream()
                    .map(job -> mapper.map(job, NativeJobInfoDto.class))
                    .collect(Collectors.toList());
        } catch (UnexpectedException e) {
            log.error(e.getMessage(), e);
            throw new UnexpectedRestException(e);
        }
    }
}
