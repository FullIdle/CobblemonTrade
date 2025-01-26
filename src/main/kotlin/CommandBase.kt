package org.figsq.cobblemontrade.cobblemontrade

import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player

object CommandBase : TabExecutor {
    private val subList = listOf(
        "apply", "cancelapply", "reload", "refuse", "help"
    )

    private const val HELPMSG =
        "§8[§7Cobblemon§rTrade§8]§7-命令帮助\n" + "§8 -§r apply <玩家名>         §7申请与玩家进行交易\n" + "§8 -§r cancelapply <玩家名>   §7取消对玩家的申请\n" + "§8 -§r cancelapply           §7取消申请\n" + "§8 -§r refuse <玩家名>        §7拒绝与玩家进行交易\n" + "§8 -§r refuse                §7拒绝所有申请\n" + "§8 -§r reload                §7重载配置文件\n" + "§8 -§r help                  §7查看本帮助"

    override fun onCommand(sender: CommandSender, cmd: Command, label: String, args: Array<out String>): Boolean {
        args.getOrNull(0)?.let { subCmd ->
            if (subCmd in subList && subCmd != "help") {
                val permission = "cobblemontrade.cmd.$subCmd"
                if (!sender.hasPermission(permission)) {
                    sender.sendMessage("§cYou don't have permission to use this command.")
                    return false
                }
                when (subCmd) {
                    "reload" -> {
                        CobblemonTrade.INSTANCE.reloadConfig()
                        return false
                    }

                    else -> {
                        if (sender !is Player) {
                            sender.sendMessage("§c该命令非玩家不可用!")
                            return false
                        }
                        val target = args.getOrNull(1)?.let(Bukkit::getPlayer)
                        when (subCmd) {
                            "apply" -> {
                                if (target == null) {
                                    sender.sendMessage("§c请输入玩家名!")
                                    return false
                                }
                                if (target == sender) {
                                    sender.sendMessage("§c不能向申请自己!")
                                    return false
                                }
                                sender.applyTrade(target)
                                return false
                            }

                            "cancelapply" -> {
                                target?.run {
                                    sender.takeUnless { it.cancelApply(this, true) }?.let {
                                        sender.sendMessage("§c没有申请记录!")
                                    }
                                    return false
                                }
                                sender.getLastApplyCache()?.let {
                                    sender.cancelApply(it.first, true)
                                    return false
                                }
                                sender.sendMessage("§c没有申请记录!")
                                return false
                            }

                            "refuse" -> {
                                target?.run {
                                    sender.takeUnless { it.refuseTrade(this) }?.let {
                                        sender.sendMessage("§c没有申请记录!")
                                    }
                                    return false
                                }
                                sender.refuseAllTrade()
                                sender.sendMessage("§a已拒尝试所有申请!")
                                return false
                            }
                        }
                        return false
                    }
                }
            }
        }
        sender.sendMessage(HELPMSG)
        return false
    }

    override fun onTabComplete(
        sender: CommandSender, cmd: Command, label: String, args: Array<out String>
    ): List<String>? {
        if (args.isEmpty()) return subList
        if (args.size == 1) return subList.filter { it.startsWith(args[0]) }
        if (args.size < 3) if (args[0].lowercase() != "reload") return Bukkit.getOnlinePlayers().map { it.name }
        return null
    }
}