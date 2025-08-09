package com.baria.bot.service;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReminderJob implements Job {
    private static final Logger log = LoggerFactory.getLogger(ReminderJob.class);
    @Override
    public void execute(JobExecutionContext context) {
        log.info("Executing reminder job {}", context.getJobDetail().getKey());
    }
}
