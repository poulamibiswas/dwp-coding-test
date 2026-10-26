package uk.gob.dwp.uc.pairtest;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.TicketService;
import uk.gov.dwp.uc.pairtest.TicketServiceImpl;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

class TicketServiceImplTest {

    private TicketPaymentService paymentService;
    private SeatReservationService reservationService;
    private TicketService ticketService;

    @BeforeEach
    void setUp() {
        paymentService = mock(TicketPaymentService.class);
        reservationService = mock(SeatReservationService.class);

        ticketService = new TicketServiceImpl(
                paymentService, reservationService);
    }

    @Test
    void calculatesPaymentAndSeatsCorrectly() {
        ticketService.purchaseTickets(
                Long.valueOf(1),
                new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 2),
                new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 2),
                new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 1));

        verify(paymentService).makePayment(1, 80);
        verify(reservationService).reserveSeat(1, 4);
    }

    @Test
    void infantsDoNotRequireSeparateSeats() {
        ticketService.purchaseTickets(
                Long.valueOf(1),
                new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 1),
                new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 2));

        verify(paymentService).makePayment(1, 25);
        verify(reservationService).reserveSeat(1, 1);
    }

    @Test
    void rejectsChildTicketsWithoutAdult() {
        assertThrows(
                InvalidPurchaseException.class,
                () -> ticketService.purchaseTickets(
                        Long.valueOf(1),
                        new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 1)));

        verifyNoInteractions(paymentService, reservationService);
    }

    @Test
    void rejectsMoreThan25Tickets() {
        assertThrows(
                InvalidPurchaseException.class,
                () -> ticketService.purchaseTickets(
                        Long.valueOf(1),
                        new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 1),
                        new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 25)));

        verifyNoInteractions(paymentService, reservationService);
    }

    @Test
    void rejectsInvalidAccount() {
        assertThrows(
                InvalidPurchaseException.class,
                () -> ticketService.purchaseTickets(
                        Long.valueOf(0),
                        new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 1)));

        verifyNoInteractions(paymentService, reservationService);
    }
}