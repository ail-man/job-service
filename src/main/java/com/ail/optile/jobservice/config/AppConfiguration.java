package com.ail.optile.jobservice.config;

import com.ail.optile.jobservice.listener.JobExecutionListener;
import com.ail.optile.jobservice.listener.JobTriggerListener;
import com.ail.optile.jobservice.repository.JobExecutionHistoryRepository;
import com.ail.optile.jobservice.service.JobService;
import com.ail.optile.jobservice.service.JobServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.dozer.DozerBeanMapper;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class AppConfiguration {

    @Autowired
    public void registerJobListener(Scheduler scheduler,
                                    JobExecutionListener jobExecutionListener,
                                    JobTriggerListener jobTriggerListener) {
        try {
            scheduler.getListenerManager().addJobListener(jobExecutionListener);
            scheduler.getListenerManager().addTriggerListener(jobTriggerListener);
        } catch (SchedulerException e) {
            log.error(e.getMessage(), e);
            throw new ApplicationContextException("Unable to configure Quartz Scheduler", e);
        }
    }

    @Bean
    @Autowired
    public JobService jobService(Scheduler scheduler, JobExecutionHistoryRepository historyRepository) {
        return new JobServiceImpl(scheduler, historyRepository);
    }

    @Bean
    public DozerBeanMapper mapper() {
        return new DozerBeanMapper();
    }
}
