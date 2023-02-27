package dev.brighten.ac.check.impl.misc;

import dev.brighten.ac.check.Check;
import dev.brighten.ac.check.WCancellable;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.packet.handler.HandlerAbstract;
import dev.brighten.ac.packet.wrapper.objects.WrappedWatchableObject;
import dev.brighten.ac.packet.wrapper.out.WPacketPlayOutEntityMetadata;

//@CheckData(name = "HealthSpoof", checkId = "healthspoof", type = CheckType.EXPLOIT)
public class HealthSpoof extends Check {

    public HealthSpoof(APlayer player) {
        super(player);
    }

    WCancellable<WPacketPlayOutEntityMetadata> event = packet -> {
        if(packet.getEntityId() == player.getBukkitPlayer().getEntityId()) return false;

        for (WrappedWatchableObject watchedObject : packet.getWatchedObjects()) {
            if (watchedObject.getDataValueId() == 6 && watchedObject.getWatchedObject() instanceof Float) {
                watchedObject.setWatchedObject(1f);

                HandlerAbstract.getHandler().sendPacketSilently(player, packet.getPacket());
                return true;
            }
        }
        return false;
    };
}
