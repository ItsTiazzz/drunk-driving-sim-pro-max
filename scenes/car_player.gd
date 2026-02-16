extends VehicleBody3D

const MAX_STEER = 0.8
const ENGINE_POWER = 100

@onready var camera_pivot: Node3D = $CameraPivot
@onready var camera_3d: Camera3D = $CameraPivot/Camera3D
@onready var reverse_camera_3d: Camera3D = $CameraPivot/ReverseCamera3D
@onready var player: CharacterBody3D = $"../Player"
@onready var player_camera: Camera3D = $"../Player/Head/Camera"
@onready var body: MeshInstance3D = $Body
@onready var this: VehicleBody3D = $"."

var look_direction
var driving = false

func _ready() -> void:
	look_direction = global_position
	
func should_interact() -> bool:
	return !driving

func interact() -> void:
	if !driving: enter()

func enter() -> void:
	#Input.set_mouse_mode(Input.MOUSE_MODE_HIDDEN)
	player.visible = false
	player.add_collision_exception_with(this)
	driving = true

func leave() -> void:
	driving = false
	player.global_position = global_position
	player.global_position.x = player.global_position.x + 5
	player.visible = true
	player.remove_collision_exception_with(this)
	player_camera.current = true

func get_prompt() -> String:
	return "Druk E om uit te stappen" if driving else "Druk E om in te stappen"

func _physics_process(delta: float) -> void:
	if not driving:
		return
	steering = move_toward(steering, Input.get_axis("right", "left") * MAX_STEER, delta * 2.5)
	engine_force = Input.get_axis("backward", "forward") * ENGINE_POWER
	camera_pivot.global_position = camera_pivot.global_position.lerp(global_position, delta * 20.0)
	camera_pivot.transform = camera_pivot.transform.interpolate_with(transform, delta * 5.0)
	look_direction = look_direction.lerp(global_position + linear_velocity, delta * 5.0)
	_check_camera_switch()
	camera_3d.look_at(look_direction)
	reverse_camera_3d.look_at(look_direction)
	
	if Input.is_action_just_released("exit"):
		leave()
	
func _check_camera_switch():
	if linear_velocity.dot(transform.basis.z) > -0.05:
		camera_3d.current = true
	else:
		reverse_camera_3d.current = true
