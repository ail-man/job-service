package com.ail.optile.jobservice.domain;

import com.ail.optile.jobservice.api.JobRequest;
import com.ail.optile.jobservice.quartz.NativeJob;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NativeJobRequest extends NativeJob implements JobRequest {

    private String name;
    private String command;
    private String cron;
    private Integer priority;
}
