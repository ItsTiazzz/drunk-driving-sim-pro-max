package me.tiazzz.ddspmkt

import godot.api.Node

inline fun <reified T : Node> Node.getNodeAs(path: String): T {
    return getNode(path) as T
}