package org.figsq.cobblemontrade.cobblemontrade.github

import com.google.common.collect.HashBiMap
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.PlayerInventory
import org.figsq.cobblemontrade.cobblemontrade.*

class TradeGui private constructor(val player1: Player, val player2: Player) : InventoryHolder {
    companion object {
        val udqSlot1: HashBiMap<Int, Int> = HashBiMap.create(
            mapOf(
                0 to 36,
                1 to 37,
                2 to 38,
                9 to 45,
                10 to 46,
                11 to 47
            )
        )
        val udqSlot2: HashBiMap<Int, Int> = HashBiMap.create(
            udqSlot1.entries.associate { it.key + 6 to it.value + 6 }
        )
        val button1Slot = listOf(
            18, 19, 20, 27, 28, 29
        )
        val button2Slot = button1Slot.map { it + 6 }
        const val SC_BUTTON_SLOT = 22
        val allButtonSlot = listOf(
            *udqSlot1.keys.toTypedArray(),
            *udqSlot1.values.toTypedArray(),
            *udqSlot2.keys.toTypedArray(),
            *udqSlot2.values.toTypedArray(),
            *button1Slot.toTypedArray(),
            *button2Slot.toTypedArray(),
            SC_BUTTON_SLOT
        )
        val uselessPaneItem = run {
            val itemStack = ItemStack(Material.BLACK_STAINED_GLASS_PANE)
            val itemMeta = itemStack.itemMeta!!
            itemMeta.setDisplayName(" ")
            itemMeta.lore = listOf(" ")
            itemStack.itemMeta = itemMeta
            itemStack
        }
        val emptySlotItem = run {
            val itemStack = ItemStack(Material.BARRIER)
            val itemMeta = itemStack.itemMeta!!
            itemMeta.setDisplayName("§c空槽")
            itemMeta.lore = listOf(" ")
            itemStack.itemMeta = itemMeta
            itemStack
        }
        val CancelButtonItem = run {
            val itemStack = ItemStack(Material.RED_STAINED_GLASS_PANE)
            val itemMeta = itemStack.itemMeta!!
            itemMeta.setDisplayName("§c取消准备")
            itemMeta.lore = listOf(" ")
            itemStack.itemMeta = itemMeta
            itemStack
        }
        val AgreeButtonItem = run {
            val itemStack = ItemStack(Material.GREEN_STAINED_GLASS_PANE)
            val itemMeta = itemStack.itemMeta!!
            itemMeta.setDisplayName("§a确认准备")
            itemMeta.lore = listOf(" ")
            itemStack.itemMeta = itemMeta
            itemStack
        }
        val F2vItem1 = run {
            val itemStack = ItemStack(Material.GLASS_BOTTLE)
            val itemMeta = itemStack.itemMeta!!
            itemMeta.setDisplayName("§6二次确认")
            itemMeta.lore = listOf(" ")
            itemStack.itemMeta = itemMeta
            itemStack
        }
        val F2vItem2 = run {
            val itemStack = ItemStack(Material.EXPERIENCE_BOTTLE)
            val itemMeta = itemStack.itemMeta!!
            itemMeta.setDisplayName("§a二次确认")
            itemMeta.lore = listOf(" ")
            itemStack.itemMeta = itemMeta
            itemStack
        }
        val invMap = hashMapOf<Inventory, TradeGui>()
        fun open(player1: Player, player2: Player): TradeGui {
            val gui = TradeGui(player1, player2)
            invMap[gui.inventory] = gui
            player1.openInventory(gui.inventory)
            player2.openInventory(gui.inventory)
            return gui
        }
    }


    var initialized:Boolean = false
    private val inventory: Inventory = Bukkit.createInventory(this, 6 * 9, "§3§l${player1.name} <——> ${player2.name}")
    private val party1 = player1.getParty()
    private val party2 = player2.getParty()
    private var f2v: Player? = null

    init {
        //初始化界面
        ((0..53) - (allButtonSlot.toSet() - SC_BUTTON_SLOT)).forEach {
            inventory.setItem(it, uselessPaneItem)
        }
        udqSlot1.values.forEachIndexed { index, i ->
            inventory.setItem(i, party1.get(index)?.getModelItemStack() ?: emptySlotItem)
        }
        udqSlot2.values.forEachIndexed { index, i ->
            inventory.setItem(i, party2.get(index)?.getModelItemStack() ?: emptySlotItem)
        }
        player1.setAgree(this, false)
        player2.setAgree(this, false)

        initialized = true
    }

    override fun getInventory(): Inventory {
        return inventory
    }

    fun click(event: InventoryClickEvent) {
        event.isCancelled = true
        if (event.clickedInventory is PlayerInventory) return
        val currentItem = event.currentItem
        val slot = event.slot
        if (currentItem == null || currentItem.type == Material.AIR || event.slot >= event.inventory.size) return
        val player = event.whoClicked as Player
        //点击宝可梦
        if (slot in player.getCanClickSlot(this)!!) {
            val udp = player.getUdp(this)!!
            val tb = player.getTradeButton(this)!!
            if (currentItem.type.name == "COBBLEMON_POKEMON_MODEL") {
                player.setAgree(this, false)
                if (slot in udp.values) {
                    //选出
                    inventory.setItem(slot, emptySlotItem)
                    inventory.setItem(udp.inverse()[slot]!!, currentItem)
                    player.sendTitle("§c选            取","",7,100,7)
                    return
                }
                if (slot in udp) {
                    //收回
                    inventory.setItem(slot, null)
                    inventory.setItem(udp[slot]!!, currentItem)
                    player.sendTitle("§c取            回","",7,100,7)
                }
                return
            }
            if (slot in tb) {
                //点击同意取消按钮
                if (player.isAgree(this))
                    player.setAgree(this, false)
                else {
                    player.setAgree(this, true)
                }
                return
            }
            if (slot == SC_BUTTON_SLOT) {
                if (currentItem.type == F2vItem1.type) {
                    player.sendTitle("§a等待对            方准备","",7,12000,7)
                    player.tradeOther(this).sendTitle("§c对方已            二次就绪","",7,12000,7)
                    inventory.setItem(SC_BUTTON_SLOT, F2vItem2)
                    f2v = player
                    return
                }
                if (currentItem.type == F2vItem2.type) {
                    //二次验证检查
                    if (f2v == player) return
                    //完成交易
                    completeTrade()
                    return
                }
            }
            return
        }
        player.sendTitle("§c这不是你            点的位置","",7,200,7)
    }

    fun close(event: InventoryCloseEvent) {
        val player = event.player as Player
        player.sendTitle("§6你主动取消了交易","",7,60,7)
        clearCache()
        player.tradeOther(this).run {
            this.closeInventory()
            this.sendTitle("§6对方主动取消了交易","",7,60,7)
        }
    }

    fun check() {
        if (player1.isAgree(this) && player2.isAgree(this)) {
            player1.sendTitle("§c二次            准备","",7,12000,7)
            player2.sendTitle("§c二次            准备","",7,12000,7)
            inventory.setItem(SC_BUTTON_SLOT, F2vItem1)
            return
        }
        inventory.setItem(SC_BUTTON_SLOT, uselessPaneItem)
        f2v = null
    }

    private fun completeTrade() {
        val temp = udqSlot1.keys.mapIndexedNotNull { index, i ->
            inventory.getItem(i)?.run {
                val poke = party1.get(index)!!
                party1.remove(poke)
                poke
            }
        }
        udqSlot2.keys.forEachIndexed { index, i ->
            inventory.getItem(i)?.run {
                val poke = party2.get(index)!!
                party2.remove(poke)
                party1.add(poke)
            }
        }
        temp.forEach {
            party2.add(it)
        }
        clearCache()
        player1.closeInventory()
        player2.closeInventory()
        player1.sendTitle("§a交易完成","",7,60,7)
        player2.sendTitle("§a交易完成","",7,60,7)
    }

    private fun clearCache(){
        invMap.remove(this.inventory)
    }
}