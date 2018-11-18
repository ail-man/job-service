package com.ail.optile.jobservice.repository;

import com.ail.optile.jobservice.pdo.JobExecutionHistory;
import org.springframework.data.repository.CrudRepository;

public interface JobExecutionHistoryRepository extends CrudRepository<JobExecutionHistory, Long> {

    JobExecutionHistory findTopByJobNameOrderByCompletionDateDesc(String jobName);

    void deleteAllByJobName(String jobName);
}
