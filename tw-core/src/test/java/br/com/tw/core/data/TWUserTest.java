package br.com.tw.core.data;

import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TWUserTest {

    @Test
    public void transferIsAtomicAndMarksBothUsersDirty() {
        TWUser sender = new TWUser(UUID.randomUUID(), "sender", 0L, "CARVAO", "NENHUM");
        TWUser recipient = new TWUser(UUID.randomUUID(), "recipient", 0L, "CARVAO", "NENHUM");
        sender.setBalance(100.0);
        sender.setDirty(false);
        recipient.setDirty(false);

        assertTrue(sender.transferBalanceTo(recipient, 35.0));

        assertEquals(65.0, sender.getBalance(), 0.0);
        assertEquals(35.0, recipient.getBalance(), 0.0);
        assertTrue(sender.isDirty());
        assertTrue(recipient.isDirty());
    }

    @Test
    public void transferDoesNotChangeEitherUserWhenFundsAreInsufficient() {
        TWUser sender = new TWUser(UUID.randomUUID(), "sender", 0L, "CARVAO", "NENHUM");
        TWUser recipient = new TWUser(UUID.randomUUID(), "recipient", 0L, "CARVAO", "NENHUM");
        sender.setBalance(20.0);
        sender.setDirty(false);

        assertFalse(sender.transferBalanceTo(recipient, 25.0));

        assertEquals(20.0, sender.getBalance(), 0.0);
        assertEquals(0.0, recipient.getBalance(), 0.0);
        assertFalse(sender.isDirty());
        assertFalse(recipient.isDirty());
    }

    @Test(expected = IllegalArgumentException.class)
    public void negativeWithdrawalIsRejected() {
        TWUser user = new TWUser(UUID.randomUUID(), "player", 10L, "CARVAO", "NENHUM");
        user.removeBalance(-1.0);
    }

    @Test
    public void savingOldSnapshotDoesNotClearNewerChanges() {
        TWUser user = new TWUser(UUID.randomUUID(), "player", 0L, "CARVAO", "NENHUM");
        user.addBalance(10.0);
        TWUserSnapshot snapshot = user.snapshot();
        user.addBalance(5.0);

        user.markSaved(snapshot.getVersion());

        assertTrue(user.isDirty());
        assertEquals(15.0, user.getBalance(), 0.0);
    }
}
