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

import com.google.protobuf.Message;
import io.spine.server.aggregate.Aggregate;
import io.spine.server.aggregate.Apply;
import io.spine.server.command.Assign;
import javaclasses.exlibris.Book;
import javaclasses.exlibris.BookDetails;
import javaclasses.exlibris.BookDetailsChange;
import javaclasses.exlibris.BookId;
import javaclasses.exlibris.BookVBuilder;
import javaclasses.exlibris.UserId;
import javaclasses.exlibris.c.AddBook;
import javaclasses.exlibris.c.BookAdded;
import javaclasses.exlibris.c.BookRemoved;
import javaclasses.exlibris.c.BookUpdated;
import javaclasses.exlibris.c.RemoveBook;
import javaclasses.exlibris.c.UpdateBook;
import javaclasses.exlibris.c.rejection.BookAlreadyExists;
import javaclasses.exlibris.c.rejection.CannotRemoveMissingBook;
import javaclasses.exlibris.c.rejection.CannotUpdateMissingBook;

import java.util.List;

import static io.spine.time.Time.getCurrentTime;
import static java.util.Collections.singletonList;
import static javaclasses.exlibris.c.aggregate.rejection.BookAggregateRejections.AddBookRejection.throwBookAlreadyExists;
import static javaclasses.exlibris.c.aggregate.rejection.BookAggregateRejections.RemoveBookRejection.throwCannotRemoveMissingBook;
import static javaclasses.exlibris.c.aggregate.rejection.BookAggregateRejections.UpdateBookRejection.throwCannotUpdateMissingBook;

/**
 * The aggregate managing the state of a {@link Book}.
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

public class BookAggregate extends Aggregate<BookId, Book, BookVBuilder> {
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
     * Otherwise aggregate constructors (that are invoked by {@link javaclasses.exlibris.repository.BookRepository}
     * via Reflection) may be left {@code private}.
     *
     * @param id the ID for the new aggregate
     */
    public BookAggregate(BookId id) {
        super(id);
    }

    @Assign
    List<? extends Message> handle(AddBook cmd) throws BookAlreadyExists {

        final BookId bookId = cmd.getBookId();

        if (cmd.getBookDetails()
               .equals(getState().getBookDetails())) {

            throwBookAlreadyExists(cmd);
        }

        final UserId userId = cmd.getLibrarianId();
        final BookDetails bookDetails = cmd.getBookDetails();

        final BookAdded result = BookAdded.newBuilder()
                                          .setBookId(bookId)
                                          .setLibrarianId(userId)
                                          .setDetails(bookDetails)
                                          .setWhenAdded(getCurrentTime())
                                          .build();

        return singletonList(result);
    }

    @Assign
    List<? extends Message> handle(UpdateBook cmd) throws CannotUpdateMissingBook {

        if (!cmd.getBookId()
                .getIsbn62()
                .getValue()
                .equals(getState().getBookId()
                                  .getIsbn62()
                                  .getValue())) {

            throwCannotUpdateMissingBook(cmd);
        }

        final BookId bookId = cmd.getBookId();
        final UserId userId = cmd.getLibrarianId();

        final BookDetailsChange bookDetails = cmd.getBookDetails();

        final BookUpdated result = BookUpdated.newBuilder()
                                              .setBookId(bookId)
                                              .setLibrarianId(userId)
                                              .setBookDetailsChange(bookDetails)
                                              .setWhenUpdated(getCurrentTime())
                                              .build();

        return singletonList(result);
    }

    @Assign
    List<? extends Message> handle(RemoveBook cmd) throws CannotRemoveMissingBook {

        final BookId bookId = cmd.getBookId();

        if (!cmd.getBookId()
                .equals(getState().getBookId())) {
            throwCannotRemoveMissingBook(cmd);
        }

        final UserId userId = cmd.getLibrarianId();

        final String customReason = cmd.getCustomReason();

        final BookRemoved.Builder bookRemoved = BookRemoved.newBuilder()
                                                           .setBookId(bookId)
                                                           .setLibrarianId(userId)
                                                           .setWhenRemoved(getCurrentTime());

        switch (cmd.getBookRemovalReasonCase()) {
            case OUTDATED: {
                return singletonList(bookRemoved
                                             .setOutdated(true)
                                             .build());
            }
            case CUSTOM_REASON: {
                return singletonList(bookRemoved
                                             .setCustomReason(customReason)
                                             .build());
            }
            case BOOKREMOVALREASON_NOT_SET: {
                throw new IllegalArgumentException("The book cannot be removed without reason.");
            }
        }
        return singletonList(bookRemoved.build());
    }

    @Apply
    private void bookAdded(BookAdded event) {

        final BookId bookId = event.getBookId();
        final BookDetails bookDetails = event.getDetails();

        getBuilder().setBookId(bookId)
                    .setBookDetails(bookDetails);
    }

    @Apply
    private void bookUpdated(BookUpdated event) {

        final BookId bookId = event.getBookId();

        final BookDetailsChange bookDetails = event.getBookDetailsChange();

        getBuilder().setBookId(bookId)
                    .setBookDetails(bookDetails.getNewBookDetails());

    }

    @Apply
    private void bookRemoved(BookRemoved event) {

        getBuilder().clearBookId()
                    .clearBookDetails();
    }
}

