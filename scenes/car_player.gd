extends VehicleBody3D

const MAX_STEER = 0.4
const ENGINE_POWER = 100

@onready var camera_pivot: Node3D = $CameraPivot
@onready var camera_3d: Camera3D = $CameraPivot/Camera3D
@onready var reverse_camera_3d: Camera3D = $CameraPivot/ReverseCamera3D

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
	driving = true

func leave() -> void:
	driving = false

func get_prompt() -> String:
	return "Druk E om in te stappen" if driving else "Druk E om uit te stappen"

func _physics_process(delta: float) -> void:
	if not driving:
		return
	steering = move_toward(steering, Input.get_axis("ui_right", "ui_left") * MAX_STEER, delta * 2.5)
	engine_force = Input.get_axis("ui_down", "ui_up") * ENGINE_POWER
	camera_pivot.global_position = camera_pivot.global_position.lerp(global_position, delta * 20.0)
	camera_pivot.transform = camera_pivot.transform.interpolate_with(transform, delta * 5.0)
	look_direction = look_direction.lerp(global_position + linear_velocity, delta * 5.0)
	_check_camera_switch()
	camera_3d.look_at(look_direction)
	reverse_camera_3d.look_at(look_direction)
	
func _check_camera_switch():
	if linear_velocity.dot(transform.basis.z) > -0.05:
		camera_3d.current = true
	else:
		reverse_camera_3d.current = true
