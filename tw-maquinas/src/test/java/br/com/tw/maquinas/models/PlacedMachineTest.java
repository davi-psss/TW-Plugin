package br.com.tw.maquinas.models;

import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertEquals;

public class PlacedMachineTest {

    @Test
    public void snapshotRemainsStableAfterMachineChanges() {
        PlacedMachine machine = new PlacedMachine("world;1;64;2", UUID.randomUUID(),
                "Player", "CARVAO", 2L, 10L, 50.0);

        PlacedMachine snapshot = machine.snapshot();
        machine.addFuel(5L);
        machine.addStoredEarnings(25.0);
        machine.addStack(1L);

        assertEquals(10L, snapshot.getFuel());
        assertEquals(50.0, snapshot.getStoredEarnings(), 0.0);
        assertEquals(2L, snapshot.getStackAmount());
    }
}
