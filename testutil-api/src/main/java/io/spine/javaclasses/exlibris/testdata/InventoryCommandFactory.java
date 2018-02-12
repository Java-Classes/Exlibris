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

package io.spine.javaclasses.exlibris.testdata;

import io.spine.net.EmailAddress;
import javaclasses.exlibris.BookId;
import javaclasses.exlibris.InventoryId;
import javaclasses.exlibris.InventoryItemId;
import javaclasses.exlibris.Isbn62;
import javaclasses.exlibris.Rfid;
import javaclasses.exlibris.UserId;
import javaclasses.exlibris.c.AppendInventory;

public class InventoryCommandFactory {

    private InventoryCommandFactory() {
    }

    public static AppendInventory appendInventoryInstance() {
        final BookId bookId = BookId.newBuilder()
                                    .setIsbn62(Isbn62.newBuilder()
                                                     .setValue("123456789"))
                                    .build();
        final InventoryId inventoryId = InventoryId.newBuilder()
                                                   .setBookId(bookId)
                                                   .build();
        final InventoryItemId inventoryItemId = InventoryItemId.newBuilder()
                                                               .setBookId(bookId)
                                                               .setItemNumber(1)
                                                               .build();
        final UserId userId = UserId.newBuilder()
                                    .setEmail(EmailAddress.newBuilder()
                                                          .setValue("petr@gmail.com"))
                                    .build();
        final Rfid rfid = Rfid.newBuilder()
                              .setValue("4321")
                              .build();
        final AppendInventory result = appendInventoryInstance(inventoryId, inventoryItemId, userId,
                                                               rfid);
        return result;
    }

    public static AppendInventory appendInventoryInstance(InventoryId inventoryId,
                                                          InventoryItemId inventoryItemId,
                                                          UserId userId, Rfid rfid) {

        AppendInventory result = AppendInventory.newBuilder()
                                                .setIntentoryId(inventoryId)
                                                .setInventoryItemId(inventoryItemId)
                                                .setLibrarianId(userId)
                                                .setRfid(rfid)
                                                .build();
        return result;
    }
}
