package com.lach.createswatches;

import com.lach.createswatches.client.TerminalScreen;
import com.lach.createswatches.net.CandidatesPayload;
import net.minecraft.client.Minecraft;

/** Keeps client classes out of common code paths; only called on the client. */
final class ClientBridge {
    static void openApplicator(net.minecraft.world.InteractionHand hand) {
        Minecraft.getInstance().setScreen(new com.lach.createswatches.client.ApplicatorScreen(hand));
    }

    static void candidates(CandidatesPayload p) {
        if (Minecraft.getInstance().screen instanceof TerminalScreen s) s.setCandidates(p);
    }
}
