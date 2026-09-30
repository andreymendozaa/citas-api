package co.com.fcv.training.citas.adapter.events;

import co.com.fcv.training.citas.application.Ports;
import org.springframework.stereotype.Component;

// TODO S5: replace with the real HTTP webhook adapter; do not relocate where
// SchedulingJdbcAdapter publishes AppointmentStatusChanged when that happens.
@Component
class NoOpAppointmentEventsAdapter implements Ports.AppointmentEvents {
    @Override public void publish(Ports.AppointmentStatusChanged event) { }
}
