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

import com.google.protobuf.Message;
import javaclasses.exlibris.Inventory;
import javaclasses.exlibris.c.MarkReservationExpired;
import javaclasses.exlibris.c.ReservationPickUpPeriodExpired;
import javaclasses.exlibris.c.ReserveBook;
import javaclasses.exlibris.testdata.InventoryCommandFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.spine.server.aggregate.AggregateMessageDispatcher.dispatchCommand;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Paul Ageyev
 */
@DisplayName("MarkReservationExpiredCommandTest command should be interpreted by InventoryAggregate and")
public class MarkReservationExpiredCommandTest extends InventoryCommandTest<MarkReservationExpired> {

    @Override
    @BeforeEach
    public void setUp() {
        super.setUp();
    }

    @Test
    @DisplayName("produce ReservationPickUpPeriodExpired event")
    void produceEvent() {
        dispatchReserveBook();

        final MarkReservationExpired markReservationExpired =
                InventoryCommandFactory.markReservationExpiredInstance();
        final List<? extends Message> messageList =
                dispatchCommand(aggregate, envelopeOf(markReservationExpired));
        assertEquals(1, messageList.size());
        assertEquals(ReservationPickUpPeriodExpired.class, messageList.get(0)
                                                                      .getClass());
    }

    @Test
    @DisplayName("remove the expired reservation")
    void reservationPickUpPeriodExpired() {
        dispatchReserveBook();

        final MarkReservationExpired reservationExpired =
                InventoryCommandFactory.markReservationExpiredInstance();
        dispatchCommand(aggregate, envelopeOf(reservationExpired));
        final Inventory state = aggregate.getState();
        final int reservationsCount = state.getReservationsCount();
        assertEquals(0, reservationsCount);
    }

    void dispatchReserveBook() {
        final ReserveBook reserveBook = InventoryCommandFactory.reserveBookInstance();
        dispatchCommand(aggregate, envelopeOf(reserveBook));
    }
}
