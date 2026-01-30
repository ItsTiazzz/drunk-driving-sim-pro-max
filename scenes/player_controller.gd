extends CharacterBody3D

var speed
const WALK_SPEED = 5.0
const SPRINT_SPEED = 8.5
const JUMP_VELOCITY = 4.5
const SENSITIVITY = 0.01

const BOB_FREQ = 2.0
const BOB_AMP = 0.08
var t_bob = 0.0

const BASE_FOV = 75.0
const FOV_CHANGE = 1.5

@onready var head: Node3D = $Head
@onready var camera: Camera3D = $Head/Camera
@onready var interaction_ray: RayCast3D = $Head/Camera/InteractionRay

var current_interactable = null
var current_prompt = null
@onready var prompt: Label = $Label

func _ready() -> void:
	Input.set_mouse_mode(Input.MOUSE_MODE_CAPTURED)
	Input.set_default_cursor_shape()

func _unhandled_input(event: InputEvent) -> void:
	if event is InputEventMouseMotion:
		head.rotate_y(-event.relative.x * SENSITIVITY)
		camera.rotate_x(-event.relative.y * SENSITIVITY)
		camera.rotation.x = clamp(camera.rotation.x, deg_to_rad(-70), deg_to_rad(60))

func _physics_process(delta: float) -> void:
	# Add the gravity.
	if not is_on_floor():
		velocity += get_gravity() * delta

	# Handle jump.
	if Input.is_action_just_pressed("jump") and is_on_floor():
		velocity.y = JUMP_VELOCITY
		
	# Handle speed.
	if Input.is_action_pressed("sprint"):
		speed = SPRINT_SPEED
	else:
		speed = WALK_SPEED

	# Get the input direction and handle the movement/deceleration.
	# As good practice, you should replace UI actions with custom gameplay actions.
	var input_dir := Input.get_vector("left", "right", "forward", "backward")
	var direction := (head.transform.basis * Vector3(input_dir.x, 0, input_dir.y)).normalized()
	if is_on_floor():
		if direction:
			velocity.x = direction.x * speed
			velocity.z = direction.z * speed
		else:
			velocity.x = lerp(velocity.x, direction.x * speed, delta * 7.0)
			velocity.z = lerp(velocity.z, direction.z * speed, delta * 7.0)
	else:
		velocity.x = lerp(velocity.x, direction.x * speed, delta * 3.0)
		velocity.z = lerp(velocity.z, direction.z * speed, delta * 3.0)
		
	t_bob += delta * velocity.length() * float(is_on_floor())
	camera.transform.origin = _headbob(t_bob)
	
	var velocity_clamped = clamp(velocity.length(), 0.5, SPRINT_SPEED * 2)
	var target_fov = BASE_FOV + FOV_CHANGE * velocity_clamped
	camera.fov = lerp(camera.fov, target_fov, delta * 8.0)
	
	_update_hover()
	if Input.is_action_just_released("interact"):
		_interact()
	
	move_and_slide()
	
func _process(_delta: float) -> void:
	if current_prompt != null and current_interactable != null:
		prompt.text = current_prompt
	else:
		prompt.text = ""

func _headbob(time) -> Vector3:
	var pos = Vector3.ZERO
	pos.y = sin(time * BOB_FREQ) * BOB_AMP
	pos.x = cos(time * BOB_FREQ / 2) * BOB_AMP
	return pos

func _update_hover() -> void:
	print(interaction_ray.is_colliding())	
	print(interaction_ray.get_collider())
	if !interaction_ray.is_colliding():
		_reset_hover()
		return
	var hit = interaction_ray.get_collider()
	if !hit:
		_reset_hover()
		return
	if hit.has_method("interact"):
		if hit.has_method("should_interact"):
			if hit.should_interact():
				current_interactable = hit
			else:
				current_interactable = null
				current_prompt = null
		else:
			current_interactable = hit
		if hit.has_method("get_prompt"):
			current_prompt = hit.get_prompt()
		else:
			_reset_hover()

func _reset_hover() -> void:
	current_prompt = null
	current_interactable = null

func _interact() -> void:
	var hit = interaction_ray.get_collider()
	if interaction_ray.is_colliding():
		if !hit: return
		if hit.has_method("should_interact"):
			if !hit.should_interact(): return
		if hit.has_method("interact"): hit.interact()
