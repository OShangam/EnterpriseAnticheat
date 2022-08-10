package dev.brighten.ac.handler.compat;

import dev.brighten.ac.handler.compat.impl.CompatHandler1_13;
import dev.brighten.ac.handler.compat.impl.CompatHandler1_8;
import dev.brighten.ac.handler.compat.impl.CompatHandler1_9;
import dev.brighten.ac.packet.ProtocolVersion;
import org.bukkit.entity.Player;

public abstract class CompatHandler {

    public abstract boolean isRiptiding(Player player);

    public abstract boolean isGliding(Player player);

    private static CompatHandler instance;

    public static CompatHandler getInstance() {
        if (instance == null) {
            if(ProtocolVersion.getGameVersion().isOrAbove(ProtocolVersion.V1_13)) {
                return new CompatHandler1_13();
            } else if(ProtocolVersion.getGameVersion().isOrAbove(ProtocolVersion.V1_9)) {
                return new CompatHandler1_9();
            } else return new CompatHandler1_8();
        }

        return instance;
    }
}
