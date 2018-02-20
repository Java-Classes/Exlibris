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

package javaclasses.exlibris.c.integrational;

import com.google.protobuf.Message;
import io.grpc.stub.StreamObserver;
import io.spine.client.TestActorRequestFactory;
import io.spine.core.Ack;
import io.spine.core.Command;
import io.spine.grpc.StreamObservers;
import io.spine.server.BoundedContext;
import io.spine.server.commandbus.CommandBus;
import javaclasses.exlibris.BookDetailsChange;
import javaclasses.exlibris.c.RemoveBook;
import javaclasses.exlibris.c.aggregate.InventoryCommandTest;
import javaclasses.exlibris.context.BoundedContexts;
import javaclasses.exlibris.testdata.BookCommandFactory;
import javaclasses.exlibris.testdata.BookRejectionsSubscriber;
import javaclasses.exlibris.testdata.InventoryCommandFactory;
import javaclasses.exlibris.testdata.InventoryRejectionsSubscriber;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.spine.protobuf.TypeConverter.toMessage;
import static javaclasses.exlibris.testdata.BookCommandFactory.createBookInstance;
import static javaclasses.exlibris.testdata.BookCommandFactory.removeBookInstance;
import static javaclasses.exlibris.testdata.BookCommandFactory.updateBookInstance;
import static javaclasses.exlibris.testdata.InventoryCommandFactory.appendInventoryInstance;
import static javaclasses.exlibris.testdata.InventoryCommandFactory.borrowBookInstance;
import static javaclasses.exlibris.testdata.InventoryCommandFactory.reserveBookInstance;
import static javaclasses.exlibris.testdata.InventoryCommandFactory.returnBookInstance;
import static javaclasses.exlibris.testdata.InventoryCommandFactory.writeBookOffInstance;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Alexander Karpets
 */
public class SimpleFlows extends InventoryCommandTest<Message> {

    @Override
    @BeforeEach
    public void setUp() {
        super.setUp();
    }

    @Test
    @DisplayName("d")
    void useCase() {
        final TestActorRequestFactory requestFactory =
                TestActorRequestFactory.newInstance(getClass());
        final BookDetailsChange newBookDetails = BookDetailsChange.newBuilder()
                                                                  .setNewBookDetails(
                                                                          BookCommandFactory.bookDetails2)
                                                                  .build();
        final Command addBook = requestFactory.createCommand(toMessage(createBookInstance()));
        final Command updateBook = requestFactory.createCommand(
                toMessage(updateBookInstance(BookCommandFactory.bookId,
                                             BookCommandFactory.userId2,
                                             newBookDetails)));
        final Command appendInventory = requestFactory.createCommand(
                toMessage(appendInventoryInstance()));
        final Command borrowBook = requestFactory.createCommand(toMessage(borrowBookInstance()));
        final Command reserveBook = requestFactory.createCommand(
                toMessage(reserveBookInstance(InventoryCommandFactory.userId2,
                                              InventoryCommandFactory.inventoryId)));
        final Command returnBook = requestFactory.createCommand(toMessage(returnBookInstance()));
        final Command borrowBook2 = requestFactory.createCommand(
                toMessage(borrowBookInstance(InventoryCommandFactory.inventoryId,
                                             InventoryCommandFactory.inventoryItemId,
                                             InventoryCommandFactory.userId2)));
        final Command returnBook2 = requestFactory.createCommand(
                toMessage(returnBookInstance(InventoryCommandFactory.inventoryId,
                                             InventoryCommandFactory.inventoryItemId,
                                             InventoryCommandFactory.userId2)));
        final Command writeBookOff = requestFactory.createCommand(
                toMessage(writeBookOffInstance()));
        final Command removeBook = requestFactory.createCommand(
                toMessage(removeBookInstance(BookCommandFactory.bookId,
                                             BookCommandFactory.librarianId,
                                             RemoveBook.BookRemovalReasonCase.OUTDATED)));
        final BoundedContext boundedContext = BoundedContexts.create();
        final CommandBus commandBus = boundedContext.getCommandBus();
        final StreamObserver<Ack> observer = StreamObservers.noOpObserver();
        final BookRejectionsSubscriber bookRejectionsSubscriber = new BookRejectionsSubscriber();
        final InventoryRejectionsSubscriber inventoryRejectionsSubscriber = new InventoryRejectionsSubscriber();
        boundedContext.getRejectionBus()
                      .register(bookRejectionsSubscriber);
        boundedContext.getRejectionBus()
                      .register(inventoryRejectionsSubscriber);
        commandBus.post(addBook, observer);
        commandBus.post(updateBook, observer);
        commandBus.post(appendInventory, observer);
        commandBus.post(borrowBook, observer);
        commandBus.post(reserveBook, observer);
        commandBus.post(returnBook, observer);
        commandBus.post(borrowBook2, observer);
        commandBus.post(returnBook2, observer);
        commandBus.post(writeBookOff, observer);
        commandBus.post(removeBook, observer);
        assertEquals(false, bookRejectionsSubscriber.wasCalled());
        assertEquals(false, inventoryRejectionsSubscriber.wasCalled());
    }
}
