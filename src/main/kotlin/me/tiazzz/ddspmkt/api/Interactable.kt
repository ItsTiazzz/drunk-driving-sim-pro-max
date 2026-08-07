package me.tiazzz.ddspmkt.api

interface Interactable {
    fun shouldInteract(): Boolean {
        return true
    }
    fun interact()
}