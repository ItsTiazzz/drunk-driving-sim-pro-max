extends VehicleBody3D

const MAX_STEER = 0.6
const ENGINE_POWER = 60

@onready var camera_pivot: Node3D = $CameraPivot
@onready var camera_3d: Camera3D = $CameraPivot/Camera3D
@onready var reverse_camera_3d: Camera3D = $CameraPivot/ReverseCamera3D
@onready var player: CharacterBody3D = $"../Player"
@onready var player_camera: Camera3D = $"../Player/Head/Camera"
@onready var body: MeshInstance3D = $Body
@onready var this: VehicleBody3D = $"."
@onready var audio: AudioStreamPlayer = $AudioStreamPlayer
@onready var audio2: AudioStreamPlayer = $AudioStreamPlayer2
@onready var audio3: AudioStreamPlayer = $AudioStreamPlayer3
@onready var heart_beat_player: AudioStreamPlayer = $"../Player/HeartBeatPlayer"
@onready var breathing_player: AudioStreamPlayer = $"../Player/BreathingPlayer"
@onready var label: Label = $"../Player/InstructionsLabel"
@onready var blackout: ColorRect = $"../Player/Blackout"

var look_direction
var driving = false
var last = 0

func _ready() -> void:
	look_direction = global_position
	
func should_interact() -> bool:
	return !driving

func interact() -> void:
	if player.bac == 5:
		return
	if !driving: enter()

func enter() -> void:
	#Input.set_mouse_mode(Input.MOUSE_MODE_HIDDEN)
	player.add_collision_exception_with(this)
	driving = true
	label.text = """W,S: motor
A,D: sturen
F: uitstappen
B: radio"""
	audio2.play()

func leave() -> void:
	driving = false
	player.global_position = global_position
	player.global_position.x = player.global_position.x + 5
	player.remove_collision_exception_with(this)
	player_camera.current = true
	label.text = """W,A,S,D: lopen
Spatie: springen
Shift: rennen"""
	audio.stop()

func get_prompt() -> String:
	if player.bac == 5:
		return "War isd e dkeur kolink?//!"
	return "Druk E om in te stappen"

func _physics_process(delta: float) -> void:
	if not driving:
		return
	steering = move_toward(steering, (Input.get_axis("right", "left")) * (MAX_STEER - (player.bac/10)), delta * 2.5)
	engine_force = Input.get_axis("backward", "forward") * ENGINE_POWER
	camera_pivot.global_position = camera_pivot.global_position.lerp(global_position, delta * 20.0)
	camera_pivot.transform = camera_pivot.transform.interpolate_with(transform, delta * 5.0)
	look_direction = look_direction.lerp(global_position + linear_velocity, delta * 5.0)
	_check_camera_switch()
	camera_3d.look_at(look_direction)
	reverse_camera_3d.look_at(look_direction)
	
	if Input.is_action_just_released("exit"):
		leave()
	if Input.is_action_just_released("asgore"):
		if audio.playing:
			audio.stop()
		else:
			audio.play()

	if last - linear_velocity.length() > 8.5:
		blackout.visible = true
		audio3.play()
		breathing_player.stop()
		heart_beat_player.stop()
		audio.stop()
	last = linear_velocity.length()
	
func _check_camera_switch():
	if linear_velocity.dot(transform.basis.z) > -0.05:
		camera_3d.current = true
	else:
		reverse_camera_3d.current = true
