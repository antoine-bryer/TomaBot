package com.tomabot.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.quartz.SchedulerFactoryBeanCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
@RequiredArgsConstructor
public class QuartzConfig {

    private final AutowiringSpringBeanJobFactory jobFactory;
    private final DataSource dataSource;

    @Bean
    public SchedulerFactoryBeanCustomizer schedulerFactoryBeanCustomizer() {
        return factory -> {
            factory.setDataSource(dataSource);
            factory.setJobFactory(jobFactory);
            factory.setOverwriteExistingJobs(true);
            factory.setWaitForJobsToCompleteOnShutdown(true);
        };
    }

}