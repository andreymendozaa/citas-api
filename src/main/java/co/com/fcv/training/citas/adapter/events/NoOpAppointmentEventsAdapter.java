package co.com.fcv.training.citas.adapter.events;

import co.com.fcv.training.citas.application.Ports;

/** Used when no n8n webhook URL is configured: events are dropped and nothing leaves the process. */
class NoOpAppointmentEventsAdapter implements Ports.AppointmentEvents {
    @Override public void publish(Ports.AppointmentStatusChanged event) { }
    @Override public void publish(Ports.RescheduleDecided event) { }
}
