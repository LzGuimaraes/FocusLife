package dev.LzGuimaraes.FocusLifeHub.Jornadas;
import java.time.*;
import org.springframework.context.annotation.*;
@Configuration
public class JornadasClockConfig {
    @Bean public Clock jornadasClock() { return Clock.system(ZoneId.of("America/Cuiaba")); }
}
