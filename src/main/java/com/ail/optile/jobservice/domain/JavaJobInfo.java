package com.ail.optile.jobservice.domain;

import com.ail.optile.jobservice.api.JobInfo;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JavaJobInfo implements JobInfo {

    private String name;
    private String cron;
    private Integer priority;
    private JobInfo.State state;
}
