package com.vatika.workorder.shared.config;

import io.micrometer.cloudwatch2.CloudWatchConfig;
import io.micrometer.cloudwatch2.CloudWatchMeterRegistry;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.config.MeterFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;

import java.time.Duration;

@Configuration
@Profile({"staging", "prod"})
public class CloudWatchMetricsConfig {

    @Bean
    public CloudWatchAsyncClient cloudWatchAsyncClient() {
        return CloudWatchAsyncClient.builder().region(Region.CA_CENTRAL_1).build();
    }

    @Bean
    public CloudWatchMeterRegistry cloudWatchMeterRegistry(CloudWatchAsyncClient client) {
        CloudWatchConfig config = new CloudWatchConfig() {
            @Override public String get(String key) { return null; }
            @Override public String namespace() { return "WorkOrderPlatform"; }
            @Override public Duration step() { return Duration.ofMinutes(1); }
        };
        CloudWatchMeterRegistry registry = new CloudWatchMeterRegistry(config, Clock.SYSTEM, client);
        // allowlist: only send what the dashboard needs (CloudWatch bills per metric)
        registry.config().meterFilter(MeterFilter.denyUnless(id ->
                id.getName().startsWith("http.server.requests")
                        || id.getName().startsWith("hikaricp.connections")));
        return registry;
    }
}
