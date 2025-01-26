package org.figsq.cobblemontrade.cobblemontrade

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.inventory.EquipmentSlot
import org.figsq.cobblemontrade.cobblemontrade.github.TradeGui

object BukkitListener : Listener {
    @EventHandler
    fun invClick(event: InventoryClickEvent) {
        TradeGui.invMap[event.inventory]?.click(event)
    }

    @EventHandler
    fun invClose(event: InventoryCloseEvent) {
        TradeGui.invMap[event.inventory]?.close(event)
    }

    @EventHandler
    fun interact(event: PlayerInteractEntityEvent) {
        if (event.hand == EquipmentSlot.HAND && event.player.isSneaking && event.rightClicked is Player) {
            Bukkit.getPlayer((event.rightClicked as Player).uniqueId)?.let {
                val player = event.player
                player.applyTrade(it)
            }
        }
    }
}