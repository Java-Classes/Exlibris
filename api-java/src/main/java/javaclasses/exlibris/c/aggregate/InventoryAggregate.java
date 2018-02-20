/*
 * Copyright 2018, TeamDev Ltd. All rights reserved.
 *
 * Redistribution and use in source and/or binary forms, with or without
 * modification, must retain the above copyright notice and the following
 * disclaimer.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package javaclasses.exlibris.c.aggregate;

import com.google.protobuf.Empty;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import io.spine.core.React;
import io.spine.server.aggregate.Aggregate;
import io.spine.server.aggregate.Apply;
import io.spine.server.command.Assign;
import io.spine.server.tuple.EitherOfTwo;
import io.spine.server.tuple.Pair;
import javaclasses.exlibris.BookId;
import javaclasses.exlibris.Inventory;
import javaclasses.exlibris.InventoryId;
import javaclasses.exlibris.InventoryItem;
import javaclasses.exlibris.InventoryItemId;
import javaclasses.exlibris.InventoryVBuilder;
import javaclasses.exlibris.Isbn62;
import javaclasses.exlibris.Loan;
import javaclasses.exlibris.LoanId;
import javaclasses.exlibris.Reservation;
import javaclasses.exlibris.Rfid;
import javaclasses.exlibris.UserId;
import javaclasses.exlibris.WriteOffReason;
import javaclasses.exlibris.c.AppendInventory;
import javaclasses.exlibris.c.BookAdded;
import javaclasses.exlibris.c.BookBecameAvailable;
import javaclasses.exlibris.c.BookBorrowed;
import javaclasses.exlibris.c.BookLost;
import javaclasses.exlibris.c.BookReadyToPickup;
import javaclasses.exlibris.c.BookRemoved;
import javaclasses.exlibris.c.BookReturned;
import javaclasses.exlibris.c.BorrowBook;
import javaclasses.exlibris.c.CancelReservation;
import javaclasses.exlibris.c.ExtendLoanPeriod;
import javaclasses.exlibris.c.InventoryAppended;
import javaclasses.exlibris.c.InventoryCreated;
import javaclasses.exlibris.c.InventoryDecreased;
import javaclasses.exlibris.c.InventoryRemoved;
import javaclasses.exlibris.c.LoanBecameOverdue;
import javaclasses.exlibris.c.LoanPeriodExtended;
import javaclasses.exlibris.c.MarkLoanOverdue;
import javaclasses.exlibris.c.MarkReservationExpired;
import javaclasses.exlibris.c.ReportLostBook;
import javaclasses.exlibris.c.ReservationAdded;
import javaclasses.exlibris.c.ReservationBecameLoan;
import javaclasses.exlibris.c.ReservationCanceled;
import javaclasses.exlibris.c.ReservationPickUpPeriodExpired;
import javaclasses.exlibris.c.ReserveBook;
import javaclasses.exlibris.c.ReturnBook;
import javaclasses.exlibris.c.WriteBookOff;
import javaclasses.exlibris.c.rejection.CannotBorrowBook;
import javaclasses.exlibris.c.rejection.CannotCancelMissingReservation;
import javaclasses.exlibris.c.rejection.CannotExtendLoanPeriod;
import javaclasses.exlibris.c.rejection.CannotReserveBook;
import javaclasses.exlibris.c.rejection.CannotReturnMissingBook;
import javaclasses.exlibris.c.rejection.CannotReturnNonBorrowedBook;
import javaclasses.exlibris.c.rejection.CannotWriteMissingBookOff;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static io.spine.time.Time.getCurrentTime;
import static javaclasses.exlibris.c.aggregate.rejection.InventoryAggregateRejections.BorrowBookRejection.throwCannotBorrowBook;
import static javaclasses.exlibris.c.aggregate.rejection.InventoryAggregateRejections.CancelReservationRejection.throwCannotCancelMissingReservation;
import static javaclasses.exlibris.c.aggregate.rejection.InventoryAggregateRejections.ExtendLoanPeriodRejection.throwCannotExtendLoanPeriod;
import static javaclasses.exlibris.c.aggregate.rejection.InventoryAggregateRejections.ReserveBookRejection.throwCannotReserveBook;
import static javaclasses.exlibris.c.aggregate.rejection.InventoryAggregateRejections.ReturnBookRejection.throwCannotReturnMissingBook;
import static javaclasses.exlibris.c.aggregate.rejection.InventoryAggregateRejections.ReturnBookRejection.throwCannotReturnNonBorrowedBook;
import static javaclasses.exlibris.c.aggregate.rejection.InventoryAggregateRejections.WriteBookOffRejection.throwCannotWriteMissingBookOff;

/**
 * The aggregate managing the state of a {@link Inventory}.
 *
 * @author Alexander Karpets
 * @author Paul Ageyev
 */

@SuppressWarnings({"ClassWithTooManyMethods", /* Task definition cannot be separated and should
                                                 process all commands and events related to it
                                                 according to the domain model.
                                                 The {@code Aggregate} does it with methods
                                                 annotated as {@code Assign} and {@code Apply}.
                                                 In that case class has too many methods.*/
        "OverlyCoupledClass", /* As each method needs dependencies  necessary to perform execution
                                                 that class also overly coupled.*/


        "unused"})  /* Apply methods are private according to the spine design and not used because there is no directly usage.*/

public class InventoryAggregate extends Aggregate<InventoryId, Inventory, InventoryVBuilder> {
    /**
     * Creates a new instance.
     *
     * <p>Constructors of derived classes should have package access level
     * because of the following reasons:
     * <ol>
     * <li>These constructors are not public API of an application.
     * Commands and aggregate IDs are.
     * <li>These constructors need to be accessible from tests in the same package.
     * </ol>
     *
     * <p>Because of the last reason consider annotating constructors with
     * {@code @VisibleForTesting}. The package access is needed only for tests.
     * Otherwise aggregate constructors (that are invoked by {@link javaclasses.exlibris.repository.InventoryRepository}
     * via Reflection) may be left {@code private}.
     *
     * @param id the ID for the new aggregate
     */
    public InventoryAggregate(InventoryId id) {
        super(id);
    }

    /**
     * {@code AppendInventory} command handler. For details see {@link AppendInventory}.
     *
     * @param cmd — command with the identifier of a specific item.
     * @return the {@code InventoryAppended} event in pair with either {@code BookBecameAvailable} or
     * {@code BookBecameAvailable} events.
     */
    @Assign
    Pair<InventoryAppended, EitherOfTwo<BookBecameAvailable, BookReadyToPickup>> handle(
            AppendInventory cmd) {

        final InventoryId inventoryId = cmd.getInventoryId();
        final InventoryItemId inventoryItemId = cmd.getInventoryItemId();
        final Rfid rfid = cmd.getRfid();
        final UserId userId = cmd.getLibrarianId();

        final InventoryAppended inventoryAppended = InventoryAppended.newBuilder()
                                                                     .setInventoryId(inventoryId)
                                                                     .setInventoryItemId(
                                                                             inventoryItemId)
                                                                     .setRfid(rfid)
                                                                     .setWhenAppended(
                                                                             getCurrentTime())
                                                                     .setLibrarianId(userId)
                                                                     .build();

        final Pair result = Pair.of(inventoryAppended,
                                    becameAvailableOrReadyToPickup(inventoryId, inventoryItemId));

        return result;
    }

    /**
     * {@code WriteBookOff} command handler. For details see {@link WriteBookOff}.
     *
     * @param cmd — command with the reason of the book writing off.
     * @return the {@code WriteBookOff} event.
     * @throws CannotWriteMissingBookOff if that book is missing.
     */
    @Assign
    InventoryDecreased handle(WriteBookOff cmd) throws CannotWriteMissingBookOff {

        List<InventoryItem> inventoryItems = getState().getInventoryItemsList();

        InventoryDecreased result = null;

        for (InventoryItem inventoryItem : inventoryItems) {
            if (inventoryItem.getInventoryItemId()
                             .equals(cmd.getInventoryItemId())) {
                final InventoryId inventoryId = cmd.getInventoryId();
                final InventoryItemId inventoryItemId = cmd.getInventoryItemId();
                final UserId librarianId = cmd.getLibrarianId();
                final WriteOffReason writeOffReason = cmd.getWriteBookOffReason();
                result = InventoryDecreased.newBuilder()
                                           .setInventoryId(inventoryId)
                                           .setInventoryItemId(
                                                   inventoryItemId)
                                           .setWhenDecreased(
                                                   getCurrentTime())
                                           .setLibrarianId(librarianId)
                                           .setWriteOffReason(
                                                   writeOffReason)
                                           .build();
            }
        }
        if (result == null) {
            throwCannotWriteMissingBookOff(cmd);
        }
        return result;
    }

    /**
     * {@code ReserveBook} command handler. For details see {@link ReserveBook}.
     *
     * @param cmd — command with the reason of the book writing off.
     * @return the {@code ReservationAdded} event.
     * @throws CannotReserveBook if that book is missing.
     */
    @Assign
    ReservationAdded handle(ReserveBook cmd) throws CannotReserveBook {

        final List<InventoryItem> inventoryItems = getState().getInventoryItemsList();
        final List<Reservation> reservations = getState().getReservationsList();
        final InventoryId inventoryId = cmd.getInventoryId();
        final UserId userId = cmd.getUserId();

        for (InventoryItem inventoryItem : inventoryItems) {
            if (inventoryItem
                    .getUserId()
                    .equals(cmd.getUserId())) {
                throwCannotReserveBook(cmd, true, false);
            }
        }

        for (Reservation reservation : reservations) {
            if (reservation.getWhoReserved()
                           .equals(cmd.getUserId())) {
                throwCannotReserveBook(cmd, false, true);
            }
        }

        final ReservationAdded result = ReservationAdded.newBuilder()
                                                        .setInventoryId(inventoryId)
                                                        .setForWhomReserved(userId)
                                                        .setWhenCreated(getCurrentTime())
                                                        .build();
        return result;
    }

    /**
     * {@code BorrowBook} command handler. For details see {@link BorrowBook}.
     * You can take a book if books in the library more than reservations, or if
     * as many "next" reservations as books available and among these reservations
     * one is belong to the user.
     *
     * @param cmd — command with the reason of the book writing off.
     * @return the {@code ReservationAdded} event.
     * @throws CannotBorrowBook if that book is missing.
     */
    @Assign
    Pair<BookBorrowed, Optional<ReservationBecameLoan>> handle(BorrowBook cmd) throws
                                                                               CannotBorrowBook {

        final List<InventoryItem> inventoryItems = getState().getInventoryItemsList();

        final int inLibraryCount = getInLibraryCount(inventoryItems);
        final UserId userId = cmd.getUserId();

        for (InventoryItem inventoryItem : inventoryItems) {
            if (inventoryItem.getUserId()
                             .equals(userId)) {
                throwCannotBorrowBook(cmd, true, false);
            }
        }

        if (getState().getReservationsList()
                      .size() >= inLibraryCount &&
                !(userHasReservation(cmd.getUserId(), inLibraryCount))) {
            throwCannotBorrowBook(cmd, false, true);
        }

        final InventoryId inventoryId = cmd.getInventoryId();
        final InventoryItemId inventoryItemId = cmd.getInventoryItemId();

        final BookBorrowed bookBorrowed = BookBorrowed.newBuilder()
                                                      .setInventoryId(inventoryId)
                                                      .setInventoryItemId(inventoryItemId)
                                                      .setWhoBorrowed(userId)
                                                      .setLoanId(LoanId.newBuilder()
                                                                       .setValue(
                                                                               getCurrentTime().getSeconds())
                                                                       .build())
                                                      .setWhenBorrowed(getCurrentTime())
                                                      .build();
        Pair result = Pair.withNullable(bookBorrowed, null);

        if (userHasReservation(cmd.getUserId())) {
            final ReservationBecameLoan reservationBecameLoan = ReservationBecameLoan.newBuilder()
                                                                                     .setInventoryId(
                                                                                             inventoryId)
                                                                                     .setUserId(
                                                                                             userId)
                                                                                     .setWhenBecameLoan(
                                                                                             getCurrentTime())
                                                                                     .build();
            result = Pair.of(bookBorrowed, reservationBecameLoan);
        }
        return result;
    }

    /**
     * Checks for the availability of reservations from the user
     *
     * @param userId — the book that the user is going to borrow.
     * @return true if the such reservation exists.
     */
    private boolean userHasReservation(UserId userId) {
        return !getState().getReservationsList()
                          .isEmpty() && getState().getReservationsList()
                                                  .get(0)
                                                  .getWhoReserved()
                                                  .getEmail()
                                                  .getValue()
                                                  .equals(userId
                                                                  .getEmail()
                                                                  .getValue());
    }

    /**
     * Count the quantity of books in a library.
     *
     * @param inventoryItems — all existing items.
     * @return the quantity of books in a library.
     */
    private int getInLibraryCount(List<InventoryItem> inventoryItems) {
        int inLibraryCount = 0;

        for (InventoryItem inventoryItem : inventoryItems) {
            if (inventoryItem.getInLibrary()) {
                inLibraryCount++;
            }
        }
        return inLibraryCount;
    }

    /**
     * {@code Empty} event handler.
     * Uses when command call an empty event.
     *
     * @param event — the {@code Empty} event message.
     */
    @Apply
    void emptyEvent(Empty event) {
    }

    private boolean userHasReservation(UserId userId, int inLibraryCount) {

        List<Reservation> topReservations = getState().getReservationsList()
                                                      .subList(0, inLibraryCount);
        for (Reservation reservation : topReservations) {
            if (reservation.getWhoReserved()
                           .equals(userId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * {@code MarkLoanOverdue} command handler. For details see {@link MarkLoanOverdue}.
     *
     * @param cmd — command from system that marks the loan as overdue.
     * @return the {@code LoanBecameOverdue} event.
     */
    @Assign
    LoanBecameOverdue handle(MarkLoanOverdue cmd) {

        final InventoryId inventoryId = cmd.getInventoryId();
        final LoanId loanId = cmd.getLoanId();
        final LoanBecameOverdue result = LoanBecameOverdue.newBuilder()
                                                          .setInventoryId(inventoryId)
                                                          .setLoanId(loanId)
                                                          .setWhenExpected(getCurrentTime())
                                                          .build();
        return result;
    }

    /**
     * {@code ExtendLoanPeriod} command handler. For details see {@link ExtendLoanPeriod}.
     *
     * @param cmd — command with the id of the loan
     *            that the user is going to extend.
     * @return the {@code LoanPeriodExtended} event.
     * @throws CannotExtendLoanPeriod if the loan period extension isn’t possible.
     */
    @Assign
    LoanPeriodExtended handle(ExtendLoanPeriod cmd) throws CannotExtendLoanPeriod {

        final List<Reservation> reservations = getState().getReservationsList();

        if (!getState().getReservationsList()
                       .isEmpty()) {
            throwCannotExtendLoanPeriod(cmd);
        }

        final InventoryId inventoryId = cmd.getInventoryId();
        final LoanId loanId = cmd.getLoanId();
        final UserId userId = cmd.getUserId();

        final List<Loan> loansList = getState().getLoansList();
        final int loanPosition = getLoanPosition(loansList, loanId);
        final Timestamp previousDueDate = getState().getLoans(loanPosition)
                                                    .getWhenDue();

        // Two weeks before new due on date.
        final long secondsInTwoWeeks = previousDueDate.getSeconds() +
                60 * 60 * 24 * 14;

        final Timestamp newDueDate = Timestamp.newBuilder()
                                              .setSeconds(secondsInTwoWeeks)
                                              .build();
        final LoanPeriodExtended result = LoanPeriodExtended.newBuilder()
                                                            .setInventoryId(inventoryId)
                                                            .setLoanId(loanId)
                                                            .setUserId(userId)
                                                            .setPreviousDueDate(previousDueDate)
                                                            .setNewDueDate(newDueDate)
                                                            .build();
        return result;
    }

    /**
     * Give the position of a necessary loan.
     *
     * @param loansList — all existing loans.
     * @param loanId    — an identifier of the necessary loan.
     * @return the loan position.
     */
    private int getLoanPosition(List<Loan> loansList, LoanId loanId) {
        int loanPosition = 0;

        for (int i = 0; i < loansList.size(); i++) {
            if (loansList.get(i)
                         .getLoanId()
                         .equals(loanId)) {
                loanPosition = i;
            }
        }
        return loanPosition;
    }

    /**
     * {@code CancelReservation} command handler. For details see {@link CancelReservation}.
     *
     * @param cmd — command with the id of the reservation
     *            that the user is going to cancel.
     * @return the {@code ReservationCanceled} event.
     * @throws CannotCancelMissingReservation if the reservation is missing.
     */
    @Assign
    ReservationCanceled handle(CancelReservation cmd) throws CannotCancelMissingReservation {

        final List<Reservation> reservations = getState().getReservationsList();

        if (!userHasReservation(cmd.getUserId())) {
            throwCannotCancelMissingReservation(cmd);
        }

        final InventoryId inventoryId = cmd.getInventoryId();
        final UserId userId = cmd.getUserId();
        final ReservationCanceled result = ReservationCanceled.newBuilder()
                                                              .setInventoryId(inventoryId)
                                                              .setWhoCanceled(userId)
                                                              .setWhenCanceled(
                                                                      getCurrentTime())
                                                              .build();
        return result;
    }

    /**
     * {@code MarkReservationExpired} command handler. For details see {@link MarkReservationExpired}.
     *
     * @param cmd — system command that contains an identifier of the expired reservation.
     * @return the {@code ReservationPickUpPeriodExpired} event.
     */
    @Assign
    ReservationPickUpPeriodExpired handle(MarkReservationExpired cmd) {

        final InventoryId inventoryId = cmd.getInventoryId();
        final UserId userId = cmd.getUserId();
        final ReservationPickUpPeriodExpired result = ReservationPickUpPeriodExpired.newBuilder()
                                                                                    .setInventoryId(
                                                                                            inventoryId)
                                                                                    .setUserId(
                                                                                            userId)
                                                                                    .setWhenExpired(
                                                                                            getCurrentTime())
                                                                                    .build();
        return result;
    }

    /**
     * {@code ReturnBook} command handler. For details see {@link ReturnBook}.
     *
     * @param cmd — command with an identifier of the book
     *            that the user is going to return.
     * @return the {@code InventoryAppended} event in pair with either {@code BookBecameAvailable} or
     * {@code BookBecameAvailable} events.
     * @throws CannotReturnNonBorrowedBook if the book isn’t borrowed by the user.
     * @throws CannotReturnMissingBook     if the book is missing.
     */
    @Assign
    Pair<InventoryAppended, EitherOfTwo<BookBecameAvailable, BookReadyToPickup>> handle(
            ReturnBook cmd) throws CannotReturnNonBorrowedBook,
                                   CannotReturnMissingBook {
        if (!inventoryItemExists(cmd.getInventoryItemId())) {
            throwCannotReturnMissingBook(cmd);
        }
        if (!userBorrowedBook(cmd.getUserId())) {
            throwCannotReturnNonBorrowedBook(cmd);
        }

        final InventoryId inventoryId = cmd.getInventoryId();
        final InventoryItemId inventoryItemId = cmd.getInventoryItemId();
        final UserId userId = cmd.getUserId();

        final BookReturned bookReturned = BookReturned.newBuilder()
                                                      .setInventoryId(inventoryId)
                                                      .setInventoryItemId(inventoryItemId)
                                                      .setWhoReturned(userId)
                                                      .setWhenReturned(getCurrentTime())
                                                      .build();

        final Pair result = Pair.of(bookReturned,
                                    becameAvailableOrReadyToPickup(inventoryId, inventoryItemId));

        return result;
    }

    /**
     * {@code ReportLostBook} command handler. For details see {@link ReportLostBook}.
     *
     * @param cmd — command that contains an identifier of the lost book
     *            and the user who lost it.
     * @return the {@code BookLost} event.
     */
    @Assign
    BookLost handle(ReportLostBook cmd) {

        final InventoryId inventoryId = cmd.getInventoryId();
        final InventoryItemId inventoryItemId = cmd.getInventoryItemId();
        final UserId userId = cmd.getWhoLost();
        final BookLost result = BookLost.newBuilder()
                                        .setInventoryId(inventoryId)
                                        .setInventoryItemId(inventoryItemId)
                                        .setWhoLost(userId)
                                        .setWhenReported(getCurrentTime())
                                        .build();
        return result;
    }

    /**
     * React on a {@code BookAdded} event.
     *
     * @param event — stimulus for reacting.
     * @return the {@code InventoryCreated} event.
     */
    @React
    InventoryCreated on(BookAdded event) {

        final InventoryCreated result = InventoryCreated.newBuilder()
                                                        .setInventoryId(InventoryId.newBuilder()
                                                                                   .setBookId(
                                                                                           event.getBookId())
                                                                                   .build())
                                                        .setWhenCreated(getCurrentTime())
                                                        .build();
        return result;
    }

    /**
     * {@code InventoryCreated} event handler. For details see {@link InventoryCreated}.
     *
     * @param event — the {@code InventoryCreated} event message.
     */
    @Apply
    void inventoryCreated(InventoryCreated event) {
        getBuilder().setInventoryId(event.getInventoryId());
    }

    /**
     * React on a {@code BookRemoved} event.
     *
     * @param event — stimulus for reacting.
     * @return the {@code InventoryRemoved} event.
     */
    @React
    InventoryRemoved on(BookRemoved event) {

        final InventoryRemoved result = InventoryRemoved.newBuilder()
                                                        .setInventoryId(InventoryId.newBuilder()
                                                                                   .setBookId(
                                                                                           event.getBookId())
                                                                                   .build())
                                                        .setWhenRemoved(getCurrentTime())
                                                        .build();
        return result;
    }

    /**
     * {@code InventoryRemoved} event handler. For details see {@link InventoryRemoved}.
     *
     * @param event — the {@code InventoryRemoved} event message.
     */
    @Apply
    void inventoryRemoved(InventoryRemoved event) {
        getBuilder().clearInventoryId()
                    .clearInventoryItems();
    }

    /**
     * {@code InventoryAppended} event handler. For details see {@link InventoryAppended}.
     *
     * @param event — the {@code InventoryAppended} event message.
     */
    @Apply
    void inventoryAppended(InventoryAppended event) {

        final InventoryItem newInventoryItem = InventoryItem.newBuilder()
                                                            .setInLibrary(true)
                                                            .setInventoryItemId(
                                                                    event.getInventoryItemId())
                                                            .setInLibrary(true)
                                                            .build();
        getBuilder().addInventoryItems(newInventoryItem);
    }

    /**
     * {@code BookBecameAvailable} event handler. For details see {@link BookBecameAvailable}.
     *
     * @param event — the {@code BookBecameAvailable} event message.
     */
    @Apply
    void bookBecameAvailable(BookBecameAvailable event) {

        final List<InventoryItem> inventoryItems = getBuilder().getInventoryItems();

        final InventoryItemId inventoryItemId = event.getInventoryItemId();
        final int availableBookIndex = getItemPosition(inventoryItems, inventoryItemId);
        getBuilder().setInventoryItems(availableBookIndex, InventoryItem.newBuilder()
                                                                        .setInventoryItemId(
                                                                                inventoryItemId)
                                                                        .setInLibrary(true)
                                                                        .build());
    }

    /**
     * A book becomes available for a user.
     *
     * This method does not change the state of an aggregate but this event is necessary for the read side.
     *
     * @param event Book is ready to pickup for a user who is next in a queue.
     */
    @SuppressWarnings("all")
    @Apply
    void bookReadyToPickup(BookReadyToPickup event) {
    }

    /**
     * {@code ReservationBecameLoan} event handler. For details see {@link ReservationBecameLoan}.
     *
     * @param event — the {@code ReservationBecameLoan} event message.
     */
    @Apply
    void reservationBecameLoan(ReservationBecameLoan event) {

        final List<Reservation> reservations = getBuilder().getReservations();

        for (int i = 0; i < reservations.size(); i++) {
            Reservation reservation = reservations.get(i);
            if (reservation.getWhoReserved()
                           .equals(event.getUserId())) {
                getBuilder().removeReservations(i);
            }
        }
    }

    /**
     * {@code InventoryDecreased} event handler. For details see {@link InventoryDecreased}.
     *
     * @param event — the {@code InventoryDecreased} event message.
     */
    @Apply
    void inventoryDecreased(InventoryDecreased event) {

        final List<InventoryItem> inventoryItems = getBuilder().getInventoryItems();

        final InventoryItemId inventoryItemId = event.getInventoryItemId();
        final int decreaseItemPosition = getItemPosition(inventoryItems, inventoryItemId);

        getBuilder().removeInventoryItems(decreaseItemPosition);
    }

    /**
     * {@code ReservationAdded} event handler. For details see {@link ReservationAdded}.
     *
     * @param event — the {@code ReservationAdded} event message.
     */
    @Apply
    void reservationAdded(ReservationAdded event) {
        final Isbn62 isbn62 = event.getInventoryId()
                                   .getBookId()
                                   .getIsbn62();
        final Reservation newReservation = Reservation.newBuilder()
                                                      .setBookId(BookId.newBuilder()
                                                                       .setIsbn62(isbn62))
                                                      .setWhenCreated(event.getWhenCreated())
                                                      .setWhoReserved(event.getForWhomReserved())
                                                      .build();
        getBuilder().addReservations(newReservation);
    }

    /**
     * {@code BookBorrowed} event handler. For details see {@link BookBorrowed}.
     *
     * @param event — the {@code BookBorrowed} event message.
     */
    @Apply
    void bookBorrowed(BookBorrowed event) {

        final List<InventoryItem> inventoryItems = getBuilder().getInventoryItems();

        final InventoryItemId inventoryItemId = event.getInventoryItemId();
        final int borrowItemPosition = getItemPosition(inventoryItems, inventoryItemId);

        final InventoryItem borrowedItem = InventoryItem.newBuilder()
                                                        .setBorrowed(true)
                                                        .setUserId(event.getWhoBorrowed())
                                                        .setInventoryItemId(
                                                                inventoryItemId)
                                                        .build();

        // The loan period time in seconds.
        // This period is equals two weeks.
        // secondsInMinute * minutesInHour * hoursInDay * daysInTwoWeeks = 1209600.
        final int loanPeriod = 1209600;

        final Loan loan = Loan.newBuilder()
                              .setLoanId(event.getLoanId())
                              .setInventoryItemId(inventoryItemId)
                              .setOverdue(false)
                              .setWhoBorrowed(event.getWhoBorrowed())
                              .setWhenTaken(getCurrentTime())
                              .setWhenDue(Timestamp.newBuilder()
                                                   .setSeconds(System.currentTimeMillis() / 1000 +
                                                                       loanPeriod)
                                                   .build())
                              .build();

        getBuilder().setInventoryItems(borrowItemPosition, borrowedItem)
                    .addLoans(loan);
    }

    /**
     * Give the position of a necessary item.
     *
     * @param inventoryItems  — all existing items.
     * @param inventoryItemId — an identifier of the necessary item.
     * @return the item position.
     */
    private int getItemPosition(List<InventoryItem> inventoryItems,
                                InventoryItemId inventoryItemId) {
        int borrowItemPosition = 0;
        for (int i = 0; i < inventoryItems.size(); i++) {
            InventoryItem item = inventoryItems.get(i);
            if (item.getInventoryItemId()
                    .equals(inventoryItemId)
                    ) {
                borrowItemPosition = i;
            }
        }
        return borrowItemPosition;
    }

    /**
     * {@code LoanBecameOverdue} event handler. For details see {@link LoanBecameOverdue}.
     *
     * @param event — the {@code LoanBecameOverdue} event message.
     */
    @Apply
    void loanBecameOverdue(LoanBecameOverdue event) {

        final List<Loan> loans = getBuilder().getLoans();

        final int loanPosition = getLoanPosition(loans, event.getLoanId());
        getBuilder().setLoans(loanPosition, Loan.newBuilder(loans.get(loanPosition))
                                                .setOverdue(true)
                                                .build());

    }

    /**
     * {@code LoanPeriodExtended} event handler. For details see {@link LoanPeriodExtended}.
     *
     * @param event — the {@code LoanPeriodExtended} event message.
     */
    @Apply
    void loanPeriodExtended(LoanPeriodExtended event) {

        final List<Loan> loans = getBuilder().getLoans();

        final LoanId loanId = event.getLoanId();
        final int loanPosition = getLoanPosition(loans, loanId);

        final Loan previousLoan = getBuilder().getLoans()
                                              .get(loanPosition);

        final Loan loan = Loan.newBuilder(previousLoan)
                              .setWhenDue(event.getNewDueDate())
                              .setOverdue(false)
                              .build();

        getBuilder().setLoans(loanPosition, loan);
    }

    /**
     * {@code ReservationCanceled} event handler. For details see {@link ReservationCanceled}.
     *
     * @param event — the {@code ReservationCanceled} event message.
     */
    @Apply
    void reservationCanceled(ReservationCanceled event) {

        final List<Reservation> reservations = getBuilder().getReservations();

        final UserId whoCanceled = event.getWhoCanceled();
        final int reservationCancelIndex = getReservationPosition(reservations, whoCanceled);

        getBuilder().removeReservations(reservationCancelIndex);
    }

    /**
     * Give the position of a necessary reservation.
     *
     * @param reservations — all existing reservations.
     * @param whoCanceled  — an identifier of the necessary user.
     * @return the reservation position.
     */
    private int getReservationPosition(List<Reservation> reservations, UserId whoCanceled) {
        int reservationCancelIndex = 0;
        for (int i = 0; i < reservations.size(); i++) {
            Reservation reservation = reservations.get(i);
            if (reservation.getWhoReserved()
                           .equals(whoCanceled)) {
                reservationCancelIndex = i;
            }
        }
        return reservationCancelIndex;
    }

    /**
     * {@code ReservationPickUpPeriodExpired} event handler. For details see {@link ReservationPickUpPeriodExpired}.
     *
     * @param event — the {@code ReservationPickUpPeriodExpired} event message.
     */
    @Apply
    void reservationPickUpPeriodExpired(ReservationPickUpPeriodExpired event) {

        final List<Reservation> reservations = getBuilder().getReservations();

        final UserId userId = event.getUserId();
        final int reservationPosition = getReservationPosition(reservations, userId);

        getBuilder().removeReservations(reservationPosition);

    }

    /**
     * {@code BookReturned} event handler. For details see {@link BookReturned}.
     *
     * @param event — the {@code BookReturned} event message.
     */
    @Apply
    void bookReturned(BookReturned event) {

        final List<InventoryItem> inventoryItems = getBuilder().getInventoryItems();

        final int returnedItemPosition = getReturnedItemPosition(event, inventoryItems);
        final InventoryItem newInventoryItem = InventoryItem.newBuilder()
                                                            .setInventoryItemId(
                                                                    event.getInventoryItemId())
                                                            .setInLibrary(true)
                                                            .build();
        getBuilder().setInventoryItems(returnedItemPosition, newInventoryItem);

        final int loanIndex = getLoanIndex(event);
        getBuilder().removeLoans(loanIndex);
    }

    /**
     * Give the position of a returned book.
     *
     * @param event          — {@code BookReturned} event.
     * @param inventoryItems — an identifier of the necessary item.
     * @return the item position.
     */
    private int getReturnedItemPosition(BookReturned event, List<InventoryItem> inventoryItems) {
        int returnedItemPosition = 0;
        for (int i = 0; i < inventoryItems.size(); i++) {
            InventoryItem item = inventoryItems.get(i);
            if (item.getUserId()
                    .equals(event.getWhoReturned()
                    ) && item.getBorrowed()) {
                returnedItemPosition = i;
            }
        }
        return returnedItemPosition;
    }

    /**
     * Give the position of a returned book loan.
     *
     * @param event — {@code BookReturned} event.
     * @return the loan position.
     */
    private int getLoanIndex(BookReturned event) {
        int loanIndex = 0;
        List<Loan> loans = getBuilder().getLoans();
        for (int i = 0; i < loans.size(); i++) {
            Loan loan = loans.get(i);
            if (loan.getWhoBorrowed()
                    .equals(event.getWhoReturned()
                    )) {
                loanIndex = i;
            }
        }
        return loanIndex;
    }

    /**
     * {@code BookLost} event handler. For details see {@link BookLost}.
     *
     * @param event — the {@code BookLost} event message.
     */
    @Apply
    void bookLost(BookLost event) {

        final List<InventoryItem> inventoryItems = getBuilder().getInventoryItems();

        final InventoryItemId inventoryItemId = event.getInventoryItemId();

        final int bookLostItemPosition = getItemPosition(inventoryItems, inventoryItemId);

        final InventoryItem inventoryItem = InventoryItem.newBuilder(
                inventoryItems.get(bookLostItemPosition))
                                                         .setLost(true)
                                                         .build();

        getBuilder().setInventoryItems(bookLostItemPosition, inventoryItem);
    }

    /**
     * Check if the user borrowed the book.
     *
     * @param userId — an identifier of the user that could borrow the book.
     * @return true if the user did.
     */
    private boolean userBorrowedBook(UserId userId) {

        for (InventoryItem item : getState().getInventoryItemsList()) {
            if (item.getUserId()
                    .equals(userId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if the inventory item exists.
     *
     * @param inventoryItemId — an identifier of the item that should exist.
     * @return true if the item exists.
     */
    private boolean inventoryItemExists(InventoryItemId inventoryItemId) {

        for (InventoryItem item : getState().getInventoryItemsList()) {
            if (item.getInventoryItemId()
                    .equals(inventoryItemId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check the book for reservations.
     * If the book has no reservations then the book {@code becameAvailable},
     * in other way — {@code readyToPickUp}.
     *
     * @param inventoryId     — an identifier of the inventory.
     * @param inventoryItemId — an identifier of the specific item.
     * @return either {@code BookBecameAvailable} or {@code BookReadyToPickup}.
     */
    private Message becameAvailableOrReadyToPickup(InventoryId inventoryId,
                                                   InventoryItemId inventoryItemId) {

        if (getState().getReservationsList()
                      .isEmpty()) {
            final BookBecameAvailable bookBecameAvailable = BookBecameAvailable.newBuilder()
                                                                               .setInventoryId(
                                                                                       inventoryId)
                                                                               .setInventoryItemId(
                                                                                       inventoryItemId)
                                                                               .setWhenBecameAvailable(
                                                                                       getCurrentTime())
                                                                               .build();
            return bookBecameAvailable;
        } else {
            final Timestamp currentTime = getCurrentTime();
            final UserId nextInQueue = getState().getReservationsList()
                                                 .get(0)
                                                 .getWhoReserved();

            // User has two days to pickup the book.
            final long secondInTwoDays = currentTime.getSeconds() + 60 * 60 * 24 * 2;
            final BookReadyToPickup bookReadyToPickup = BookReadyToPickup.newBuilder()
                                                                         .setInventoryId(
                                                                                 inventoryId)
                                                                         .setInventoryItemId(
                                                                                 inventoryItemId)
                                                                         .setForWhom(nextInQueue)
                                                                         .setWhenBecameReadyToPickup(
                                                                                 currentTime)
                                                                         .setPickUpDeadline(
                                                                                 Timestamp.newBuilder()
                                                                                          .setSeconds(
                                                                                                  secondInTwoDays)
                                                                                          .build())
                                                                         .build();
            return bookReadyToPickup;
        }
    }
}
