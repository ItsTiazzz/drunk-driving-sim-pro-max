package me.tiazzz.ddspmkt.player

import godot.annotation.Register
import godot.annotation.Script
import godot.annotation.Visible
import godot.api.Camera3D
import godot.api.CharacterBody3D
import godot.api.ColorRect
import godot.api.DirectionalLight3D
import godot.api.Input
import godot.api.InputEvent
import godot.api.InputEventMouseMotion
import godot.api.Label
import godot.api.Node3D
import godot.api.Object
import godot.api.RayCast3D
import godot.api.ShaderMaterial
import godot.core.Vector3
import godot.core.asStringName
import godot.global.GD
import me.tiazzz.ddspmkt.api.Interactable
import me.tiazzz.ddspmkt.api.Promptable
import me.tiazzz.ddspmkt.getNodeAs

@Script
class Player : CharacterBody3D() {
	companion object {
		const val WALK_SPEED = 5.3
		const val SPRINT_SPEED = 8.5
		const val JUMP_VELOCITY = 4.5
		const val SENSITIVITY = 0.01

		const val BOB_FREQ = 2.0
		const val BOB_AMP = 0.08

		const val BASE_FOV = 75.0
		const val FOV_CHANGE = 1.5
	}

	var speed = WALK_SPEED

	var tBob = 0.0
	@Visible
	var shouldBob = true
	@Visible
	var shouldMove = true

	lateinit var head: Node3D
	lateinit var camera: Camera3D
	lateinit var interactionRay: RayCast3D

	var currentInteractable: Object? = null
	var currentPrompt: String? = null
	lateinit var prompt: Label

	@Visible
	lateinit var bacLabel: Label
	@Visible
	var bac = 0.0
	lateinit var vignette: ColorRect
	lateinit var instructionsLabel: Label

	lateinit var directionalLight: DirectionalLight3D
	var night = false

	@Visible
	var randomizeMovement = false
	var randomizationTime = 0.0
	var ran1 = "left"
	var ran2 = "right"
	var ran3 = "forward"
	var ran4 = "backward"

	override fun _ready() {
		head = getNodeAs("Head")
		camera = getNodeAs("Head/Camera")
		interactionRay = getNodeAs("Head/Camera/InteractionRay")
		prompt = getNodeAs("Label")
		bacLabel = getNodeAs("BACLabel")
		vignette = getNodeAs("Vignette")
		instructionsLabel = getNodeAs("InstructionsLabel")
		directionalLight = getNodeAs("../Environment/DirectionalLight3D")

		visible = false
		Input.setMouseMode(Input.MouseMode.CAPTURED)
		Input.setDefaultCursorShape()
		(vignette.material as ShaderMaterial).setShaderParameter("intensity", 0.0)
	}

	override fun _unhandledInput(event: InputEvent) {
		if (event is InputEventMouseMotion) {
			head.rotateY((-event.relative.x * SENSITIVITY).toFloat())
			camera.rotateX((-event.relative.y * SENSITIVITY).toFloat())
			camera.rotationMutate {
				x = GD.clamp(camera.rotation.x, GD.degToRad(-70.0), GD.degToRad(60.0))
			}
		}
	}

	override fun _physicsProcess(delta: Double) {
		if (!isOnFloor()) {
			velocity += getGravity() * delta
		}

		if (shouldMove) {
			move(delta)
		} else {
			velocity = Vector3.ZERO
		}

		tBob += delta * velocity.length() * if (isOnFloor()) 1.0 else 0.0
		camera.transformMutate {
			origin = headBob(tBob)
		}

		val velocityClamped = GD.clamp(velocity.length(), 0.5, SPRINT_SPEED * 2)
		val targetFov = BASE_FOV + FOV_CHANGE * velocityClamped
		camera.fov = GD.lerp(camera.fov, targetFov.toFloat(), (delta * 8.0).toFloat())

		updateHover()
		if (Input.isActionJustReleased("interact")) {
			interact()
		}
		if (Input.isActionJustReleased("nightmode")) {
			directionalLight.visible = night
			night = !night
		}

		moveAndSlide()
	}

	override fun _process(delta: Double) {
		if (currentPrompt != null && currentInteractable != null) {
			prompt.text = currentPrompt ?: ""
		} else {
			prompt.text = ""
		}
	}

	@Register
	fun move(delta: Double) {
		if (Input.isActionJustPressed("jump") && isOnFloor()) {
			velocityMutate { y = JUMP_VELOCITY }
		}

		speed = if (Input.isActionPressed("sprint")) {
			SPRINT_SPEED - bac
		} else {
			WALK_SPEED - bac
		}

		if (randomizeMovement) {
			randomizationTime += delta
		} else {
			randomizationTime = 0.0
		}

		if (randomizationTime >= 5) {
			randomizationTime = 0.0
			val rand = GD.randiRange(0, 1)
			val old1 = ran1
			val old2 = ran2
			val old3 = ran3
			val old4 = ran4
			if (rand == 0) {
				ran1 = old2
				ran2 = old3
				ran3 = old4
				ran4 = old1
			} else {
				ran1 = old4
				ran2 = old1
				ran3 = old2
				ran4 = old3
			}
		}

		val inputDir = Input.getVector(ran1, ran2, ran3, ran4)
		val direction = (head.transform.basis * Vector3(inputDir.x, 0, inputDir.y)).normalized()

		if (isOnFloor()) {
			if (direction != null) {
				velocityMutate {
					x = direction.x * speed
					z = direction.z * speed
				}
			} else {
				velocityMutate {
					x = GD.lerp(velocity.x, direction.x * speed, delta * 7.0)
					z = GD.lerp(velocity.z, direction.z * speed, delta * 7.0)
				}
			}
		}
	}

	@Register
	fun headBob(time: Double): Vector3 {
		var time = time
		val pos = Vector3.ZERO
		if (!shouldBob) {
			time = 0.0
		}
		pos.y = GD.sin(time * BOB_FREQ) * (BOB_AMP * (bac + 1))
		pos.x = GD.cos(time * BOB_FREQ / 2) * (BOB_AMP * (bac + 1))
		return pos
	}

	@Register
	fun updateHover() {
		if (!interactionRay.isColliding()) {
			resetHover()
			return
		}
		val hit = interactionRay.getCollider()
		if (hit == null) {
			resetHover()
			return
		}

		if (hit is Interactable) {
			if (hit.shouldInteract()) {
				currentInteractable = hit
			} else {
				currentInteractable = null
				currentPrompt = null
			}
		}

		if (hit is Promptable) {
			currentPrompt = hit.getPrompt()
		} else {
			resetHover()
		}
	}

	@Register
	fun resetHover() {
		currentPrompt = null
		currentInteractable = null
	}

	@Register
	fun interact() {
		if (!interactionRay.isColliding()) {
			return
		}
		val hit = interactionRay.getCollider() as? Interactable ?: return
		if (hit.shouldInteract()) {
			hit.interact()
		}
	}
}
