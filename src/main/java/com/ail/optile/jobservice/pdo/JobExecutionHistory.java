package com.ail.optile.jobservice.pdo;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.Date;

@NoArgsConstructor
@Entity
@Table(
        indexes = {
                @Index(name = "jobexecutionhistory_jobname_idx", columnList = "jobname")
        }
)
public class JobExecutionHistory {

    @Getter
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Getter
    @Column(nullable = false, updatable = false)
    private String jobName;

    @Getter
    @Column(nullable = false, updatable = false)
    private Date completionDate;

    @Getter
    @Column(nullable = false, updatable = false)
    private Result result;

    @Builder
    public JobExecutionHistory(String jobName, Date completionDate, Result result) {
        this.jobName = jobName;
        this.completionDate = completionDate;
        this.result = result;
    }

    public enum Result {
        SUCCESS,
        FAILED
    }
}
