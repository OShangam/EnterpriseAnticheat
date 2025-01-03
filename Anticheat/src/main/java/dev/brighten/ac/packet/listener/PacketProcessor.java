package dev.brighten.ac.packet.listener;

import dev.brighten.ac.Anticheat;
import dev.brighten.ac.packet.ProtocolVersion;
import dev.brighten.ac.packet.listener.functions.PacketListener;
import dev.brighten.ac.packet.wrapper.PacketConverter;
import dev.brighten.ac.packet.wrapper.PacketType;
import dev.brighten.ac.packet.wrapper.impl.Processor_18;
import dev.brighten.ac.utils.MiscUtils;
import lombok.Getter;
import lombok.val;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/*
    An asynchronous processor for packets.
 */
public class PacketProcessor {
    private final Map<PacketType, List<ListenerEntry>>
            processors = new HashMap<>();
    private final Map<PacketType, List<ListenerEntry>>
            asyncProcessors = new HashMap<>();

    @Getter
    private final PacketConverter packetConverter;

    public PacketProcessor() {
        switch (ProtocolVersion.getGameVersion()) {
            case V1_8_9: {
                packetConverter = new Processor_18();
                break;
            }
            default: {
                packetConverter = null;
                break;
            }
        }
    }

    public PacketListener process(PacketListener listener, PacketType... types) {
        return process(EventPriority.NORMAL, listener, types);
    }

    public PacketListener process(EventPriority priority, PacketListener listener, PacketType... types) {
        ListenerEntry entry = new ListenerEntry(priority, listener);
        synchronized (processors) {
            for (PacketType type : types) {
                processors.compute(type, (key, list) -> {
                    if(list == null) list = new CopyOnWriteArrayList<>();

                    list.add(entry);
                    list.sort(Comparator.comparing(t -> t.getPriority().getSlot()));

                    return list;
                });
            }
        }

        return listener;
    }

    public PacketListener process(EventPriority priority, PacketListener listener) {
        return process(priority, listener, PacketType.UNKNOWN);
    }

    public PacketListener processAsync(PacketListener listener, PacketType... types) {
        return processAsync(EventPriority.NORMAL, listener, types);
    }

    public PacketListener processAsync(EventPriority priority, PacketListener listener) {
        return processAsync(priority, listener, PacketType.UNKNOWN);
    }

    public PacketListener processAsync(EventPriority priority, PacketListener listener,
                                            PacketType... types) {
        ListenerEntry entry = new ListenerEntry(priority, listener);
        synchronized (asyncProcessors) {
            for (PacketType type : types) {
                asyncProcessors.compute(type, (key, list) -> {
                    if(list == null) list = new CopyOnWriteArrayList<>();

                    list.add(entry);
                    list.sort(Comparator.comparing(t -> t.getPriority().getSlot()));

                    return list;
                });
            }
        }

        return listener;
    }

    public boolean removeListener(PacketListener listener) {
        boolean removedListener = false;
        synchronized (processors) {
            int iterations = 0;
            for (List<ListenerEntry> list : processors.values()) {
                for (Iterator<ListenerEntry> it = list.iterator(); it.hasNext(); ) {
                    ListenerEntry entry = it.next();

                    iterations++;
                    if(entry.getListener() == listener) {
                        it.remove();
                        Anticheat.INSTANCE.getLogger().info("Removed listener in " + iterations + " iterations.");
                        removedListener = true;
                        break;
                    }
                }
            }
        }
        synchronized (asyncProcessors) {
            int iterations = 0;
            for (List<ListenerEntry> list : processors.values()) {
                for (Iterator<ListenerEntry> it = list.iterator(); it.hasNext(); ) {
                    ListenerEntry entry = it.next();

                    iterations++;
                    if(entry.getListener() == listener) {
                        it.remove();
                        Anticheat.INSTANCE.getLogger().info("Removed listener in " + iterations + " iterations.");
                        removedListener = true;
                        break;
                    }
                }
            }
        }

        return removedListener;
    }

    public Object call(Player player, Object packet, PacketType type) {
        if(packet == null) return false;

        PacketInfo info = new PacketInfo(player, packet, type, System.currentTimeMillis()),
                asyncInfo;
        boolean cancelled = false;
        if(processors.containsKey(type) || processors.containsKey(PacketType.UNKNOWN)) {
            val list = MiscUtils.combine(processors.get(type), processors.get(PacketType.UNKNOWN));

            for (ListenerEntry tuple : list) {
                try {
                    tuple.getListener().onEvent(info);

                    if (info.isCancelled()) {
                        cancelled = true;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        if(asyncProcessors.containsKey(type) || asyncProcessors.containsKey(PacketType.UNKNOWN)) {
            asyncInfo = new PacketInfo(player, PacketType.processType(type, packet), type, info.getTimestamp());
            asyncInfo.setCancelled(cancelled);
            val list = MiscUtils.combine(asyncProcessors.get(type),
                    asyncProcessors.get(PacketType.UNKNOWN));

            for (ListenerEntry tuple : list) {
                tuple.getListener().onEvent(asyncInfo);

                if (asyncInfo.isCancelled()) {
                    cancelled = true;
                }
            }
        }
        return cancelled ? null : info.getPacket();
    }

    public void shutdown() {
        processors.clear();
        asyncProcessors.clear();
    }
}
