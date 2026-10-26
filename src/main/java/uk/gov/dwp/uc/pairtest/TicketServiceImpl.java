package uk.gov.dwp.uc.pairtest;

import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

public class TicketServiceImpl implements TicketService {

    private static final int MAX_TICKETS = 25;
    private static final int ADULT_PRICE = 25;
    private static final int CHILD_PRICE = 15;

    private final TicketPaymentService paymentService;
    private final SeatReservationService reservationService;

    public TicketServiceImpl(
            TicketPaymentService paymentService,
            SeatReservationService reservationService) {
        this.paymentService = paymentService;
        this.reservationService = reservationService;
    }

    @Override
    public void purchaseTickets(
            Long accountId,
            TicketTypeRequest... ticketTypeRequests)
            throws InvalidPurchaseException {

        validateRequest(accountId, ticketTypeRequests);

        int adults = 0;
        int children = 0;
        int infants = 0;
        int totalTickets = 0;

        for (TicketTypeRequest request : ticketTypeRequests) {
            int count = request.getNoOfTickets();
            totalTickets += count;

            switch (request.getTicketType()) {
                case ADULT -> adults += count;
                case CHILD -> children += count;
                case INFANT -> infants += count;
            }
        }

        if (totalTickets == 0) {
            throw new InvalidPurchaseException(
                    "At least one ticket must be requested");
        }

        if (adults == 0 && (children > 0 || infants > 0)) {
            throw new InvalidPurchaseException(
                    "Child and infant tickets require an adult ticket");
        }

        int totalAmountToPay = adults * ADULT_PRICE + children * CHILD_PRICE;

        int totalSeatsToAllocate = adults + children;

        paymentService.makePayment(accountId, totalAmountToPay);

        reservationService.reserveSeat(
                accountId, totalSeatsToAllocate);
    }

    private void validateRequest(
            Long accountId,
            TicketTypeRequest... requests)
            throws InvalidPurchaseException {

        if (accountId == null || accountId <= 0) {
            throw new InvalidPurchaseException(
                    "Account ID must be greater than zero");
        }

        if (requests == null || requests.length == 0) {
            throw new InvalidPurchaseException(
                    "At least one ticket request is required");
        }

        int totalTickets = 0;

        for (TicketTypeRequest request : requests) {
            if (request == null) {
                throw new InvalidPurchaseException(
                        "Ticket request cannot be null");
            }

            if (request.getTicketType() == null) {
                throw new InvalidPurchaseException(
                        "Ticket type cannot be null");
            }

            int count = request.getNoOfTickets();

            if (count < 0) {
                throw new InvalidPurchaseException(
                        "Ticket count cannot be negative");
            }

            if (count > MAX_TICKETS - totalTickets) {
                throw new InvalidPurchaseException(
                        "Cannot purchase more than 25 tickets");
            }

            totalTickets += count;
        }
    }
}
