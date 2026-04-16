package com.flashsale.activity.job;

import com.flashsale.activity.service.ActivityService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ActivityPublishScheduler {

    private final ActivityService activityService;

    public ActivityPublishScheduler(ActivityService activityService) {
        this.activityService = activityService;
    }

    @Scheduled(
            fixedDelayString = "${flash-sale.activity.publish-scan-delay:5000}",
            initialDelayString = "${flash-sale.activity.publish-initial-delay:5000}"
    )
    public void publishReadyActivities() {
        activityService.publishReadyActivities();
    }
}
