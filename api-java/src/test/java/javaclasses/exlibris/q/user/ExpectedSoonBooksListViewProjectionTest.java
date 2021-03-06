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

package javaclasses.exlibris.q.user;

import javaclasses.exlibris.ExpectedSoonBooksListViewId;
import javaclasses.exlibris.c.BookAdded;
import javaclasses.exlibris.c.InventoryAppended;
import javaclasses.exlibris.q.ExpectedSoonItem;
import javaclasses.exlibris.q.ExpectedSoonItemStatus;
import javaclasses.exlibris.q.ProjectionTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.spine.Identifier.newUuid;
import static io.spine.server.projection.ProjectionEventDispatcher.dispatch;
import static javaclasses.exlibris.testdata.BookEventFactory.bookAddedInstance;
import static javaclasses.exlibris.testdata.InventoryEventFactory.inventoryAppendedInstance;
import static javaclasses.exlibris.testdata.TestValues.AUTHOR;
import static javaclasses.exlibris.testdata.TestValues.BOOK_CATEGORY;
import static javaclasses.exlibris.testdata.TestValues.BOOK_ID;
import static javaclasses.exlibris.testdata.TestValues.BOOK_SYNOPSIS;
import static javaclasses.exlibris.testdata.TestValues.BOOK_TITLE;
import static javaclasses.exlibris.testdata.TestValues.COVER_URL;
import static javaclasses.exlibris.testdata.TestValues.ISBN;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ExpectedSoonBooksListViewProjectionTest extends ProjectionTest {

    private ExpectedSoonBooksListViewProjection projection;

    @BeforeEach
    void setUp() {
        final ExpectedSoonBooksListViewId bookListId = ExpectedSoonBooksListViewId.newBuilder()
                                                                                  .setValue(
                                                                                          newUuid())
                                                                                  .build();
        projection = new ExpectedSoonBooksListViewProjection(bookListId);
    }

    @Nested
    @DisplayName("BookAdded event should be interpreted by ExpectedSoonBooksListViewProjection and")
    class BookAddedEvent {

        @Test
        @DisplayName("add BookItem to the list of books that are expected soon")
        void addBookItem() {
            final BookAdded bookAdded = bookAddedInstance();
            dispatch(projection, createEvent(bookAdded));

            final List<ExpectedSoonItem> books = projection.getState()
                                                           .getBookItemList();
            assertEquals(1, books.size());
            final ExpectedSoonItem bookItem = books.get(0);
            assertEquals(BOOK_ID, bookItem.getBookId());
            assertEquals(BOOK_TITLE, bookItem.getTitle());
            assertEquals(AUTHOR, bookItem.getAuthorsList()
                                         .get(0));
            assertEquals(ISBN, bookItem.getIsbn());
            assertEquals(COVER_URL, bookItem.getCoverUrl());
            assertEquals(BOOK_CATEGORY, bookItem.getCategories(0));
            assertEquals(BOOK_SYNOPSIS, bookItem.getSynopsis());
            Assertions.assertEquals(ExpectedSoonItemStatus.EXPECTED_SOON, bookItem.getStatus());
        }
    }

    @Nested
    @DisplayName("InventoryAppended event should be interpreted by ExpectedSoonBooksListViewProjection and")
    class InventoryAppendedEvent {

        @Test
        @DisplayName("remove ExpectedSoonItem from the list of books that are expected soon")
        void increaseNumberOfAvailableBooks() {
            final BookAdded bookAdded = bookAddedInstance();

            final InventoryAppended inventoryAppended = inventoryAppendedInstance();

            dispatch(projection, createEvent(bookAdded));
            dispatch(projection, createEvent(inventoryAppended));

            final List<ExpectedSoonItem> books = projection.getState()
                                                           .getBookItemList();
            assertEquals(0, books.size());
        }
    }

}
