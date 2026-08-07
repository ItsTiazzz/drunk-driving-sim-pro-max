package me.tiazzz.ddspmkt.item

import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.api.AudioStreamPlayer
import godot.api.ColorRect
import godot.api.MeshInstance3D
import godot.api.Node3D
import godot.api.ShaderMaterial
import godot.core.Vector4
import godot.coroutines.await
import godot.coroutines.awaitMainThread
import godot.coroutines.godotCoroutine
import godot.global.GD
import me.tiazzz.ddspmkt.api.Interactable
import me.tiazzz.ddspmkt.api.Promptable
import me.tiazzz.ddspmkt.getNodeAs
import me.tiazzz.ddspmkt.player.Player

@RegisterClass
class Beer : Node3D(), Interactable, Promptable {
    var empty = false

    val format: (value: Double) -> String = { value -> "BAC: $value promille" }

    val vignetteMap = mapOf(
        0.5 to 0.3,
        1.0 to 0.4,
        1.5 to 0.5,
        2.0 to 0.6,
        2.5 to 0.7,
        3.0 to 0.8,
        3.5 to 0.9,
        4.0 to 1.0,
        4.5 to 1.3,
        5.0 to 2.0,
    )
    
    lateinit var player: Player
    lateinit var vignette: ColorRect
    lateinit var glokPlayer: AudioStreamPlayer
    lateinit var heartBeatPlayer: AudioStreamPlayer
    lateinit var breathingPlayer: AudioStreamPlayer
    lateinit var diePlayer: AudioStreamPlayer
    lateinit var head: Node3D
    lateinit var blackout: ColorRect
    lateinit var mesh: MeshInstance3D

    @RegisterFunction
    override fun _ready() {
        player = getNodeAs("../../../Player")
        vignette = getNodeAs("../../../Player/Vignette")
        glokPlayer = getNodeAs("../../../Player/GlokPlayer")
        heartBeatPlayer = getNodeAs("../../../Player/HeartBeatPlayer")
        breathingPlayer = getNodeAs("../../../Player/BreathingPlayer")
        diePlayer = getNodeAs("../../../Player/DiePlayer")
        head = getNodeAs("../../../Player/Head")
        blackout = getNodeAs("../../../Player/Blackout")
        mesh = getNodeAs("..")
    }

    @RegisterFunction
    override fun getPrompt(): String {
        return "Druk E om te nuttigen"
    }

    @RegisterFunction
    override fun shouldInteract(): Boolean {
        return !empty
    }

    @RegisterFunction
    override fun interact() {
        empty = true
        mesh.visible = false
        player.addCollisionExceptionWith(this)

        player.bac += 0.5
        val bac = player.bac
        player.bacLabel.text = format(bac)
        GD.print(vignetteMap[bac])
        (vignette.material as ShaderMaterial).setShaderParameter("intensity", vignetteMap[bac])
        glokPlayer.play()

        when (bac) {
            1.0 -> heartBeatPlayer.play()
            2.0 -> breathingPlayer.play()
            3.5 -> player.randomizeMovement = true
            4.0 -> heartBeatPlayer.volumeDb = 24f
            5.0 -> godotCoroutine {
                getTree()!!.createTimer(6.0).timeout.await()
                awaitMainThread {
                    breathingPlayer.stop()
                    diePlayer.play()
                    (vignette.material as ShaderMaterial).setShaderParameter("intensity", 0.8)
                    (vignette.material as ShaderMaterial).setShaderParameter(
                        "vignette_color",
                        Vector4(190f, 0f, 0f, 1f)
                    )
                    player.shouldMove = false
                    player.shouldBob = false
                }
                getTree()!!.createTimer(3.30).timeout.await()
                awaitMainThread {
                    heartBeatPlayer.stop()
                    head.positionMutate {
                        y -= 1.2
                    }
                }
                getTree()!!.createTimer(0.07).timeout.await()
                awaitMainThread {
                    blackout.visible = true
                }
            }
        }
    }
}