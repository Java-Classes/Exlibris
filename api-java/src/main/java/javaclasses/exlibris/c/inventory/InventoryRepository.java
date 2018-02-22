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

package javaclasses.exlibris.c.inventory;

import com.google.common.collect.ImmutableSet;
import com.google.protobuf.Message;
import io.spine.server.aggregate.AggregateRepository;
import io.spine.server.route.EventRoute;
import javaclasses.exlibris.BookId;
import javaclasses.exlibris.InventoryId;
import javaclasses.exlibris.c.BookAdded;
import javaclasses.exlibris.c.BookRemoved;

import java.util.Set;

/**
 * @author Alexander Karpets
 * @author Dmytry Dyachenko
 */
public class InventoryRepository extends AggregateRepository<InventoryId, InventoryAggregate> {

    public InventoryRepository() {
        getEventRouting().replaceDefault((EventRoute<InventoryId, Message>) (message, context) -> {
            if (message instanceof BookAdded) {
                return getInventoryIds((BookAdded) message);
            }
            if (message instanceof BookRemoved) {
                return getInventoryIds((BookRemoved) message);
            }
            throw new IllegalArgumentException("Cannot route the unreacted event.");
        });
    }

    private static Set<InventoryId> getInventoryIds(BookRemoved message) {
        final BookId bookId = message.getBookId();

        final ImmutableSet<InventoryId> inventoryIds =
                ImmutableSet.of(InventoryId.newBuilder()
                                           .setBookId(bookId)
                                           .build());
        return inventoryIds;
    }

    private static Set<InventoryId> getInventoryIds(BookAdded message) {
        final BookId bookId = message.getBookId();

        final ImmutableSet<InventoryId> inventoryIds =
                ImmutableSet.of(InventoryId.newBuilder()
                                           .setBookId(bookId)
                                           .build());
        return inventoryIds;
    }

}
