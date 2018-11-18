package com.ail.optile.jobservice.listener;

import com.ail.optile.jobservice.pdo.JobExecutionHistory;
import com.ail.optile.jobservice.repository.JobExecutionHistoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Objects;

@Component
@Slf4j
public class JobExecutionListener implements JobListener {

    private static final String LISTENER_NAME = "jobExecutionListener";
    private static final int EXECUTION_SUCCESS_CODE = 0;

    private final JobExecutionHistoryRepository historyRepository;

    @Autowired
    public JobExecutionListener(JobExecutionHistoryRepository historyRepository) {
        this.historyRepository = historyRepository;
    }

    @Override
    public String getName() {
        return LISTENER_NAME;
    }

    @Override
    public void jobToBeExecuted(JobExecutionContext context) {
    }

    @Override
    public void jobExecutionVetoed(JobExecutionContext context) {
    }

    @Override
    public void jobWasExecuted(JobExecutionContext context, JobExecutionException jobException) {
        JobExecutionHistory jobExecutionHistory = JobExecutionHistory.builder()
                .jobName(context.getJobDetail().getKey().getName())
                .completionDate(new Date())
                .result(Objects.equals(context.getResult(), EXECUTION_SUCCESS_CODE)
                        ? JobExecutionHistory.Result.SUCCESS
                        : JobExecutionHistory.Result.FAILED)
                .build();
        historyRepository.save(jobExecutionHistory);
        log.info("Job '{}' result: {}", context.getJobDetail().getKey(), context.getResult());
    }
}
