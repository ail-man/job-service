package com.ail.optile.jobservice.domain;

import com.ail.optile.jobservice.api.JobRequest;
import com.ail.optile.jobservice.domain.exception.ExecutionException;
import com.ail.optile.jobservice.domain.exception.RollbackException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public abstract class JavaJobRequest implements JobRequest {

    public static final int RESULT_CODE_SUCCESS = 0;
    public static final int RESULT_CODE_FAILED = 1;

    private String name;
    private String cron;
    private Integer priority;

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        try {
            executeJob(jobExecutionContext);
            jobExecutionContext.setResult(RESULT_CODE_SUCCESS);
        } catch (ExecutionException ee) {
            jobExecutionContext.setResult(RESULT_CODE_FAILED);
            log.error(ee.getMessage(), ee);

            try {
                rollbackJob(jobExecutionContext);
            } catch (RollbackException re) {
                log.error(re.getMessage(), re);
            }

            throw new JobExecutionException(ee);
        }
    }

    public abstract void executeJob(JobExecutionContext jobExecutionContext) throws ExecutionException;

    public abstract void rollbackJob(JobExecutionContext jobExecutionContext) throws RollbackException;
}
