package dev.brighten.ac.check.impl.world;

import dev.brighten.ac.check.WAction;
import dev.brighten.ac.check.Check;
import dev.brighten.ac.check.CheckData;
import dev.brighten.ac.api.check.CheckType;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.packet.ProtocolVersion;
import dev.brighten.ac.packet.wrapper.in.WPacketPlayInBlockPlace;
import dev.brighten.ac.packet.wrapper.in.WPacketPlayInFlying;
import dev.brighten.ac.utils.*;
import dev.brighten.ac.utils.annotation.Async;
import dev.brighten.ac.utils.math.cond.MaxDouble;
import dev.brighten.ac.utils.world.BlockData;
import dev.brighten.ac.utils.world.CollisionBox;
import dev.brighten.ac.utils.world.types.RayCollision;
import dev.brighten.ac.utils.world.types.SimpleCollisionBox;
import org.bukkit.Location;
import org.bukkit.block.Block;

import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

@CheckData(name = "Block (A)", checkId = "blocka", type = CheckType.INTERACT)
public class BlockA extends Check {
    public BlockA(APlayer player) {
        super(player);
    }

    private final MaxDouble verbose = new MaxDouble(20);
    private Queue<Tuple<Block, SimpleCollisionBox>> blockPlacements = new LinkedBlockingQueue<>();

    @Async
    WAction<WPacketPlayInBlockPlace> blockPlace = packet -> {
        Location loc = packet.getBlockPos().toBukkitVector().toLocation(player.getBukkitPlayer().getWorld());
        Optional<Block> optionalBlock = BlockUtils.getBlockAsync(loc);

        if(!optionalBlock.isPresent()) return;

        final Block block = optionalBlock.get();
        CollisionBox box = BlockData.getData(block.getType()).getBox(block, player.getPlayerVersion());

        debug(packet.getBlockPos().toString());
        if(!(box instanceof SimpleCollisionBox)) {
            debug("Not SimpleCollisionBox: " + box.getClass().getSimpleName() + ";" + block.getType());
            return;
        }

        final SimpleCollisionBox simpleBox = ((SimpleCollisionBox) box);

        if(Math.abs(simpleBox.maxY - simpleBox.minY) != 1.
                || Math.abs(simpleBox.maxX - simpleBox.minX) != 1.
                || Math.abs(simpleBox.maxZ - simpleBox.minZ) != 1.) {
            debug("not full block: x=%.1f y=%.1f z=%.1f",
                    Math.abs(simpleBox.maxX - simpleBox.minX),
                    Math.abs(simpleBox.maxY - simpleBox.minY),
                    Math.abs(simpleBox.maxZ - simpleBox.minZ));
            return;
        }

        blockPlacements.add(new Tuple<>(block, simpleBox.expand(0.1)));
    };

    @Async
    WAction<WPacketPlayInFlying> flying = packet -> {
        Tuple<Block, SimpleCollisionBox> tuple;

        while((tuple = blockPlacements.poll()) != null) {
            final SimpleCollisionBox box = tuple.two.copy().expand(0.025);
            final Block block = tuple.one;

            final KLocation to = player.getMovement().getTo().getLoc().clone(),
                    from = player.getMovement().getFrom().getLoc().clone();

            to.y += player.getInfo().sneaking ? (ProtocolVersion.getGameVersion().isOrAbove(ProtocolVersion.V1_14)
                    ? 1.27f : 1.54f) : 1.62f;
            from.y += player.getInfo().lsneaking ? (ProtocolVersion.getGameVersion().isOrAbove(ProtocolVersion.V1_14)
                    ? 1.27f : 1.54f) : 1.62f;

            final RayCollision rayTo = new RayCollision(to.toVector(),
                    MathUtils.getDirection(to)),
                    rayFrom = new RayCollision(from.toVector(),
                            MathUtils.getDirection(from));

            final boolean collided = rayTo.isCollided(box) || rayFrom.isCollided(box);

            if (!collided) {
                if (verbose.add() > 4) {
                    flag("to=[x=%.1f y=%.1f z=%.1f yaw=%.1f pitch=%.1f] loc=[%.1f,%.1f,%.1f]",
                            to.x, to.y, to.z, to.yaw, from.pitch,
                            block.getLocation().getX(), block.getLocation().getY(), block.getLocation().getZ());
                }
            } else verbose.subtract(0.33);

            debug("collided=%s verbose=%s", collided, verbose.value());
        }
    };
}