package me.tiazzz.ddspmkt.player

import godot.annotation.Register
import godot.annotation.Script
import godot.api.AudioStreamPlayer
import godot.api.Camera3D
import godot.api.ColorRect
import godot.api.Input
import godot.api.Label
import godot.api.Node3D
import godot.api.VehicleBody3D
import godot.common.util.snapped
import godot.core.Vector3
import godot.global.GD
import me.tiazzz.ddspmkt.api.Interactable
import me.tiazzz.ddspmkt.api.Promptable
import me.tiazzz.ddspmkt.getNodeAs

@Script
class Car : VehicleBody3D(), Interactable, Promptable {
	companion object {
		const val MAX_STEER = 0.5
		const val ENGINE_POWER = 80
	}

	lateinit var cameraPivot: Node3D
	lateinit var camera3D: Camera3D
	lateinit var reverseCamera3D: Camera3D

	lateinit var player: Player
	lateinit var playerCamera: Camera3D
	lateinit var audio1: AudioStreamPlayer
	lateinit var audio2: AudioStreamPlayer
	lateinit var audio3: AudioStreamPlayer
	lateinit var heartBeatPlayer: AudioStreamPlayer
	lateinit var breathingPlayer: AudioStreamPlayer
	lateinit var label: Label
	lateinit var blackout: ColorRect
	lateinit var speedLabel: Label
	val formatSpeed: (speed: Double) -> String = { speed -> "Snelheid: $speed km/h" }

	var lookDirection: Vector3 = Vector3.ZERO
	var driving = false
	var last = 0

	override fun _ready() {
		cameraPivot = getNodeAs("CameraPivot")
		camera3D = getNodeAs("CameraPivot/Camera3D")
		reverseCamera3D = getNodeAs("CameraPivot/ReverseCamera3D")
		player = getNodeAs("../Player")
		playerCamera = getNodeAs("../Player/Head/Camera")
		audio1 = getNodeAs("AudioStreamPlayer")
		audio2 = getNodeAs("AudioStreamPlayer2")
		audio3 = getNodeAs("AudioStreamPlayer3")
		heartBeatPlayer = getNodeAs("../Player/HeartBeatPlayer")
		breathingPlayer = getNodeAs("../Player/BreathingPlayer")
		label = getNodeAs("../Player/InstructionsLabel")
		blackout = getNodeAs("../Player/Blackout")
		speedLabel = getNodeAs("Label")

		lookDirection = globalPosition
	}

	@Register
	override fun shouldInteract() = !driving

	@Register
	override fun interact() {
		if (player.bac == 5.0) {
			return
		}
		if (!driving) {
			enter()
		}
	}

	@Register
	fun enter() {
		player.addCollisionExceptionWith(this)
		driving = true
		label.text = """W,A,S,D: beweging
            F: uitstappen
			B: radio""".trimIndent()
		audio2.play()
		speedLabel.visible = true
	}

	@Register
	fun leave() {
		driving = false
		player.globalPosition = globalPosition
		player.globalPosition.x += 5
		player.removeCollisionExceptionWith(this)
		playerCamera.current = true
		label.text = """W,A,S,D: lopen
            Spatie: springen
            Shift: rennen
			N: nachtmodus""".trimIndent()
		audio1.stop()
		speedLabel.visible = false
	}

	@Register
	override fun getPrompt(): String {
		if (player.bac == 5.0) {
			return "War isd e dkeur kolink?//!"
		}
		return "Druk E om in te stappen"
	}

	override fun _physicsProcess(delta: Double) {
		if (!driving) {
			return
		}
		steering = GD.moveToward(steering, (Input.getAxis("right", "left")) * (MAX_STEER - (player.bac / 10)).toFloat(), (delta * 2.5).toFloat())
		engineForce = Input.getAxis("backward", "forward") * ENGINE_POWER
		cameraPivot.globalPosition = cameraPivot.globalPosition.lerp(globalPosition, delta * 20.0)
		cameraPivot.transform = cameraPivot.transform.interpolateWith(transform, delta * 5.0)
		lookDirection = lookDirection.lerp(globalPosition + linearVelocity, delta * 5.0)
		checkCameraSwitch()
		camera3D.lookAt(lookDirection)
		reverseCamera3D.lookAt(lookDirection)

		if (Input.isActionJustReleased("exit")) {
			leave()
		}
		if (Input.isActionJustReleased("asgore")) {
			if (audio1.playing) {
				audio1.stop()
			} else {
				audio1.play()
			}
		}

		if (last - linearVelocity.length() > 8.5) {
			blackout.visible = true
			audio3.play()
			breathingPlayer.stop()
			heartBeatPlayer.stop()
			audio1.stop()
		}

		last = linearVelocity.length().toInt()

		speedLabel.text = formatSpeed(snapped(linearVelocity.length() * 3.6, 1.0))
	}

	@Register
	fun checkCameraSwitch() {
		if (linearVelocity.dot(transform.basis.z) > -0.05) {
			camera3D.current = true
		} else {
			reverseCamera3D.current = true
		}
	}
}
