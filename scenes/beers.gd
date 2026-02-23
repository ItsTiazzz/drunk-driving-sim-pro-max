extends Node3D

var empty = false
const format = "BAC: %s promille"

var vignettemap = {
	0.5: 0.3,
	1.0: 0.4,
	1.5: 0.5,
	2.0: 0.6,
	2.5: 0.7,
	3.0: 0.8,
	3.5: 0.9,
	4.0: 1.0,
	4.5: 1.3,
	5.0: 2
}

@onready var player: CharacterBody3D = $"../../../Player"
@onready var vignette: ColorRect = $"../../../Player/Vignette"
@onready var glok_player: AudioStreamPlayer = $"../../../Player/GlokPlayer"
@onready var heart_beat_player: AudioStreamPlayer = $"../../../Player/HeartBeatPlayer"
@onready var breathing_player: AudioStreamPlayer = $"../../../Player/BreathingPlayer"
@onready var die_player: AudioStreamPlayer = $"../../../Player/DiePlayer"
@onready var head: Node3D = $"../../../Player/Head"
@onready var blackout: ColorRect = $"../../../Player/Blackout"
@onready var mesh: MeshInstance3D = $".."
@onready var this: StaticBody3D = $"."

func get_prompt() -> String:
	return "Druk E om te nuttigen"

func should_interact() -> bool:
	return !empty

func interact() -> void:
	empty = true
	mesh.visible = false
	player.add_collision_exception_with(this)
	player.bac = player.bac + 0.5
	player.bac_label.text = format % player.bac
	print(vignettemap[player.bac])
	vignette.material.set_shader_parameter("intensity", vignettemap[player.bac])
	glok_player.play()
	
	if (player.bac == 1):
		heart_beat_player.play()
	elif (player.bac == 2):
		breathing_player.play()
	elif (player.bac == 3.5):
		player.randomize_movement = true
	elif (player.bac == 4):
		heart_beat_player.volume_db = 24
	elif (player.bac == 5):
		await get_tree().create_timer(6).timeout
		breathing_player.stop()
		die_player.play()
		vignette.material.set_shader_parameter("intensity", 0.8)
		vignette.material.set_shader_parameter("vignette_color", Vector4(190, 0, 0, 1))
		player.should_move = false
		player.should_bob = false
		await get_tree().create_timer(3.30).timeout
		heart_beat_player.stop()
		head.position.y = head.position.y - 1.2
		await get_tree().create_timer(0.07).timeout
		blackout.visible = true
