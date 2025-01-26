package org.figsq.cobblemontrade.cobblemontrade

import com.cobblemon.mod.common.Cobblemon
import me.fullidle.ficore.ficore.common.SomeMethod
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask

class CobblemonTrade: JavaPlugin() {
    companion object {
        lateinit var INSTANCE: CobblemonTrade
        val applyCache = hashMapOf<Player, Pair<Player,BukkitTask>>()
    }

    override fun onEnable() {
        INSTANCE = this
        this.reloadConfig()

        this.getCommand("cobblemontrade")!!.setExecutor(CommandBase)
        this.server.pluginManager.registerEvents(BukkitListener,this)
        EventHandler.register()

        this.logger.info("§3MC-Version:§a ${SomeMethod.getMinecraftVersion()}")
        this.logger.info("§3NMS-Version:§a ${SomeMethod.getNmsVersion()}")
        this.logger.info("§3Cobblemon-Version:§a ${Cobblemon.VERSION}")
        this.logger.info("§3CobblemonTrade-Version:§a ${this.description.version}")
        this.logger.info("§aPlugin enabled!")
    }

    override fun reloadConfig() {
        this.saveDefaultConfig()
        super.reloadConfig()
    }

    override fun onDisable() {
        EventHandler.unregister()
    }
}