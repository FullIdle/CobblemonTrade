package org.figsq.cobblemontrade.cobblemontrade

import com.cobblemon.mod.common.api.reactive.ObservableSubscription

class EventHandler {
    companion object {
        private val list = ArrayList<ObservableSubscription<*>>()
        fun register(){
        }

        fun unregister(){
            list.forEach { it.unsubscribe() }
        }
    }
}
