<?php
// openminds_server/enrollFormation.php
require_once 'config.php';

$token        = isset($_POST['token'])        ? $_POST['token']           : '';
$formation_id = isset($_POST['formation_id']) ? (int)$_POST['formation_id'] : 0;
if ($token === '' || $formation_id === 0) { echo json_encode(["status"=>"missing_fields"]); exit(); }

$u = $db_con->prepare("SELECT id FROM user WHERE token = ?");
$u->bind_param("s", $token); $u->execute();
$row = $u->get_result()->fetch_assoc();
if (!$row) { echo json_encode(["status"=>"invalid_token"]); exit(); }
$user_id = $row['id'];

// Empêcher de s'inscrire à sa propre formation
$own = $db_con->prepare("SELECT id FROM formation WHERE id = ? AND created_by = ?");
$own->bind_param("ii", $formation_id, $user_id); $own->execute();
if ($own->get_result()->num_rows > 0) {
    echo json_encode(["status"=>"own_formation"]); exit();
}

// Empêcher la ré-inscription
$chk = $db_con->prepare("SELECT id FROM enrollment WHERE user_id=? AND formation_id=?");
$chk->bind_param("ii", $user_id, $formation_id); $chk->execute();
if ($chk->get_result()->num_rows > 0) { echo json_encode(["status"=>"already_enrolled"]); exit(); }

$ins = $db_con->prepare("INSERT INTO enrollment (user_id, formation_id) VALUES (?,?)");
$ins->bind_param("ii", $user_id, $formation_id);
echo json_encode(["status" => $ins->execute() ? "success" : "error_insert"]);
