package dev.LzGuimaraes.FocusLifeHub.Email;

import java.time.LocalDate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DailyNotificationJob {

    private static final Logger log = LoggerFactory.getLogger(DailyNotificationJob.class);

    @Scheduled(cron = "0 0 8 * * *", zone = "America/Cuiaba")
    public void run() {
        log.info("Job diário executado - {}", LocalDate.now());
    }
}
