package org.figsq.cobblemontrade.cobblemontrade

import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.api.storage.PokemonStoreManager
import com.cobblemon.mod.common.api.storage.party.PlayerPartyStore
import com.cobblemon.mod.common.api.storage.pc.PCStore
import com.cobblemon.mod.common.item.PokemonItem
import com.cobblemon.mod.common.pokemon.Pokemon
import com.google.common.collect.HashBiMap
import me.fullidle.ficore.ficore.common.bukkit.inventory.CraftItemStack
import net.md_5.bungee.api.chat.*
import net.md_5.bungee.api.chat.hover.content.Text
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitTask
import org.figsq.cobblemontrade.cobblemontrade.CobblemonTrade.Companion.applyCache
import org.figsq.cobblemontrade.cobblemontrade.github.TradeGui
import org.figsq.cobblemontrade.cobblemontrade.github.TradeGui.Companion.AgreeButtonItem
import org.figsq.cobblemontrade.cobblemontrade.github.TradeGui.Companion.CancelButtonItem
import org.figsq.cobblemontrade.cobblemontrade.github.TradeGui.Companion.SC_BUTTON_SLOT
import java.lang.reflect.Method


var getPartyMethod: Method? = null
var getPCMethod: Method? = null
var pokemonItemFromMethod: Method? = null
val AGREE_BUTTON_MSG = fun(target: Player): TextComponent {
    return TextComponent("§a[同意]").apply {
        hoverEvent = HoverEvent(HoverEvent.Action.SHOW_TEXT, Text("§a同意/申请"))
        clickEvent = ClickEvent(ClickEvent.Action.RUN_COMMAND, "/cobblemontrade apply ${target.name}")
    }
}

val CANCEL_BUTTON_MSG = fun(target: Player): TextComponent {
    return TextComponent("§c[取消]").apply {
        hoverEvent = HoverEvent(HoverEvent.Action.SHOW_TEXT, Text("§c取消申请"))
        clickEvent = ClickEvent(ClickEvent.Action.RUN_COMMAND, "/cobblemontrade cancelapply ${target.name}")
    }
}

val REFUSE_MSG = fun(target: Player): TextComponent {
    return TextComponent("§c[拒绝]").apply {
        hoverEvent = HoverEvent(HoverEvent.Action.SHOW_TEXT, Text("§c拒绝申请"))
        clickEvent = ClickEvent(ClickEvent.Action.RUN_COMMAND, "/cobblemontrade refuse ${target.name}")
    }
}

val ALREADY_APPLIED_MSG = fun(target: Player): Array<BaseComponent> {
    return ComponentBuilder("§6你已向对方申请过交易了!")
        .append(CANCEL_BUTTON_MSG(target), ComponentBuilder.FormatRetention.NONE).create()
}

val APPLY_MSG = fun(target: Player): Array<BaseComponent> {
    return ComponentBuilder("§6向 ${target.name} 发起交易 等待对方同意")
        .append(CANCEL_BUTTON_MSG(target), ComponentBuilder.FormatRetention.NONE)
        .create()
}

val RECEIVE_APPLY_MSG = fun(target: Player): Array<BaseComponent> {
    return ComponentBuilder("§6${target.name} 向你发起了交易 15s后自动拒绝")
        .append(AGREE_BUTTON_MSG(target), ComponentBuilder.FormatRetention.NONE)
        .append(REFUSE_MSG(target), ComponentBuilder.FormatRetention.NONE)
        .create()
}


fun Player.getParty(): PlayerPartyStore {
    val handle = this.getHandle()
    getPartyMethod = getPartyMethod ?: PokemonStoreManager::class.java.getDeclaredMethod("getParty", handle.javaClass)
    return getPartyMethod!!.invoke(Cobblemon.storage, handle) as PlayerPartyStore
}

fun Player.getPC(): PCStore {
    val handle = this.getHandle()
    getPCMethod = getPCMethod ?: PokemonStoreManager::class.java.getDeclaredMethod("getPC", handle.javaClass)
    return getPCMethod!!.invoke(Cobblemon.storage, handle) as PCStore
}

fun Player.getHandle(): Any {
    return this.javaClass.getDeclaredMethod("getHandle").invoke(this)
}

fun Pokemon.getModelItemStack(): ItemStack {
    pokemonItemFromMethod =
        pokemonItemFromMethod ?: PokemonItem.Companion::class.java.getDeclaredMethod("from", Pokemon::class.java)
    return CraftItemStack.asBukkitCopy(pokemonItemFromMethod!!.invoke(PokemonItem.Companion, this))
}

fun Player.getUdp(tradeGui: TradeGui): HashBiMap<Int, Int>? {
    if (tradeGui.player1 == this) {
        return TradeGui.udqSlot1
    }
    if (tradeGui.player2 == this) {
        return TradeGui.udqSlot2
    }
    return null
}

fun Player.getTradeButton(tradeGui: TradeGui): List<Int>? {
    if (tradeGui.player1 == this) {
        return TradeGui.button1Slot
    }
    if (tradeGui.player2 == this) {
        return TradeGui.button2Slot
    }
    return null
}

fun Player.getCanClickSlot(tradeGui: TradeGui): List<Int>? {
    if (tradeGui.player1 == this || tradeGui.player2 == this) {
        val udp = getUdp(tradeGui)
        val tradeButton = getTradeButton(tradeGui)
        return listOf(
            *udp!!.keys.toTypedArray(),
            *udp.values.toTypedArray(),
            *tradeButton!!.toTypedArray(),
            SC_BUTTON_SLOT
        )
    }
    return null
}

fun Player.isAgree(tradeGui: TradeGui): Boolean {
    val tb = getTradeButton(tradeGui) ?: return false
    return tradeGui.inventory.getItem(tb[0])?.type == AgreeButtonItem.type
}

fun Player.setAgree(tradeGui: TradeGui, agree: Boolean) {
    val tb = getTradeButton(tradeGui) ?: throw IllegalArgumentException("玩家没有在 变量 tradeGui 中!!!")
    val itemStack = if (agree) AgreeButtonItem else CancelButtonItem
    tb.forEach { tradeGui.inventory.setItem(it, itemStack) }

    if (!tradeGui.initialized) return

    if (agree) {
        this.sendTitle("§a你已            准备", "", 7, 12000, 7)
        val other = tradeOther(tradeGui)
        if (!other.isAgree(tradeGui)) {
            other.sendTitle("§c对方准            备就绪", "", 7, 12000, 7)
        }
    } else {
        this.sendTitle("§c你取消            了准备", "", 7, 100, 7)
        val other = tradeOther(tradeGui)
        if (other.isAgree(tradeGui)) {
            other.sendTitle("§c重新            准备", "", 7, 100, 7)
            other.setAgree(tradeGui, false)
        }
    }
    tradeGui.check()
}

fun Player.tradeOther(tradeGui: TradeGui): Player {
    if (tradeGui.player1 == this || tradeGui.player2 == this)
        return if (tradeGui.player1 == this)
            tradeGui.player2
        else
            tradeGui.player1
    throw IllegalArgumentException("玩家不在变量tradeGui的交易中!")
}

fun Player.applyTrade(target: Player) {
    //对方处于交易状态
    TradeGui.invMap.values.forEach {
        if (it.player1 == target || it.player2 == target) {
            this.sendMessage("§c对方正在交易中,请稍后再试")
            return
        }
    }

    //对面也发送了申请
    applyCache[target]?.let { pair ->
        //target提前发过申请
        if (pair.first == this) {
            //静默取消掉对方对自己的交易申请
            target.cancelApply(this, false)
            //让对方拒绝掉他的其余申请
            target.refuseAllTrade()
            //拒绝掉向自己的申请
            this.refuseAllTrade()
            //直接打开交易界面
            TradeGui.open(target, this)
            //提示
            target.sendMessage("§a玩家:${this.name} 同意了你的交易申请")
            this.sendMessage("§a你同意了玩家:${target.name} 的交易申请")
            return
        }
    }

    //检查申请
    applyCache[this]?.let {
        //已申请过了
        if (it.first == target) {
            this.spigot().sendMessage(*ALREADY_APPLIED_MSG(target))
            return
        }
        //取消掉原本申请
        cancelApply(it.first, true)
    }
    //申请
    applyCache[this] = target to Bukkit.getScheduler().runTaskLater(CobblemonTrade.INSTANCE, Runnable {
        target.refuseTrade(this)
    }, 5 * 20L)
    this.spigot().sendMessage(*APPLY_MSG(target))
    target.spigot().sendMessage(*RECEIVE_APPLY_MSG(this))
}

fun Player.refuseTrade(target: Player): Boolean {
    if (target.cancelApply(this, false)) {
        this.sendMessage("§c拒绝${target.name}的交易申请")
        target.sendMessage("§c玩家:${this.name}拒绝了你的交易申请")
        return true
    }
    return false
}

fun Player.refuseAllTrade() {
    applyCache.entries.forEach {
        if (it.value.first == this) this.refuseTrade(it.key)
    }
}

fun Player.cancelApply(target: Player, needSend: Boolean): Boolean {
    applyCache[this]?.let {
        if (it.first == target) {
            it.second.cancel()
            if (needSend) {
                this.sendMessage("§e取消对${it.first.name}的交易申请")
                it.first.sendMessage("§e玩家:${this.name}已取消你的交易申请")
            }
            applyCache.remove(this)
            return true
        }
    }
    return false
}

fun Player.getLastApplyCache(): Pair<Player, BukkitTask>? {
    applyCache[this]?.let {
        return it
    }
    return null
}