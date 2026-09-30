package co.com.fcv.training.citas.adapter.events;

import co.com.fcv.training.citas.application.Ports;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Bridges the in-process Spring event to the output port only after the enclosing transaction commits. */
@Component
class AppointmentEventsListener {
    private final Ports.AppointmentEvents port;
    AppointmentEventsListener(Ports.AppointmentEvents port) { this.port = port; }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void onStatusChanged(Ports.AppointmentStatusChanged event) { port.publish(event); }
}
