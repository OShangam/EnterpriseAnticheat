package dev.brighten.ac.handler;

import dev.brighten.ac.Anticheat;
import dev.brighten.ac.packet.ProtocolVersion;
import dev.brighten.ac.packet.wrapper.objects.EnumParticle;
import dev.brighten.ac.utils.ItemBuilder;
import dev.brighten.ac.utils.Materials;
import dev.brighten.ac.utils.annotation.Init;
import dev.brighten.ac.utils.world.BlockData;
import dev.brighten.ac.utils.world.EntityData;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ComponentBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Init
public class BBRevealHandler implements Listener {

    private final Set<Block> blocksToShow = new HashSet<>();
    private final Set<Entity> entitiesToShow = new HashSet<>();

    public static BBRevealHandler INSTANCE;

    private static final ItemStack wand = new ItemBuilder(Material.BLAZE_ROD).name("&6Box Wand")
            .amount(1).build();

    public BBRevealHandler() {
        INSTANCE = this;
        runShowTask();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent event) {
        if(!event.getPlayer().getItemInHand().isSimilar(wand)) return;

        if(event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if(Materials.checkFlag(event.getClickedBlock().getType(), Materials.COLLIDABLE)) {
                if(blocksToShow.contains(event.getClickedBlock())) {
                    blocksToShow.remove(event.getClickedBlock());
                    event.getPlayer().spigot().sendMessage(new ComponentBuilder("No longer showing block: ")
                            .color(ChatColor.RED).append(event.getClickedBlock().getType().name()).color(ChatColor.WHITE)
                            .create());
                } else {
                    blocksToShow.add(event.getClickedBlock());
                    event.getPlayer().spigot().sendMessage(new ComponentBuilder("Now showing block: ")
                            .color(ChatColor.GREEN).append(event.getClickedBlock().getType().name()).color(ChatColor.WHITE)
                            .create());
                }
            } else {
                event.getPlayer().spigot().sendMessage(new ComponentBuilder("Block is not collidable!")
                        .color(ChatColor.RED)
                        .create());
            }
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEntityEvent event) {
        if(!event.getPlayer().getItemInHand().isSimilar(wand)) return;

        if(entitiesToShow.contains(event.getRightClicked())) {
            entitiesToShow.remove(event.getRightClicked());
            event.getPlayer().spigot().sendMessage(new ComponentBuilder("No longer showing entity "
                    + event.getRightClicked().getName() + ".")
                    .color(ChatColor.RED).create());
            event.setCancelled(true);
        } else {
            entitiesToShow.add(event.getRightClicked());
            event.getPlayer().spigot().sendMessage(new ComponentBuilder("Now showing entity "
                    + event.getRightClicked().getName() + ".")
                    .color(ChatColor.GREEN).create());
            event.setCancelled(true);
        }
    }

    public void giveWand(Player player) {
        player.getInventory().addItem(wand);
    }

    private void runShowTask() {
        Anticheat.INSTANCE.getScheduler().scheduleAtFixedRate(() -> {
            blocksToShow.forEach(b -> BlockData.getData(b.getType()).getBox(b, ProtocolVersion.getGameVersion())
                    .draw(EnumParticle.FLAME, Bukkit.getOnlinePlayers().toArray(new Player[0])));
            entitiesToShow.forEach(e -> {
                EntityData.getEntityBox(e.getLocation(), e)
                        .draw(EnumParticle.FLAME, Bukkit.getOnlinePlayers().toArray(new Player[0]));
            });
        }, 3000, 250, TimeUnit.MILLISECONDS);
    }

}
