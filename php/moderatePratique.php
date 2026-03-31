<?php
// openminds_server/moderatePratique.php — Admin uniquement
require_once 'config.php';

$token  = isset($_POST['token'])  ? $_POST['token']     : '';
$id     = isset($_POST['id'])     ? (int)$_POST['id']   : 0;
$active = isset($_POST['active']) ? (int)$_POST['active'] : 0;
$action = isset($_POST['action']) ? $_POST['action']      : ''; // 'moderate' ou 'delete'

if ($token === '' || $id === 0) { echo json_encode(["status"=>"missing_fields"]); exit(); }

$u = $db_con->prepare("SELECT id, role FROM user WHERE token = ?");
$u->bind_param("s", $token); $u->execute();
$user = $u->get_result()->fetch_assoc();
if (!$user || $user['role'] !== 'admin') {
    echo json_encode(["status"=>"forbidden"]); exit();
}

if ($action === 'delete') {
    $stmt = $db_con->prepare("DELETE FROM bonne_pratique WHERE id = ?");
    $stmt->bind_param("i", $id);
} else {
    $stmt = $db_con->prepare("UPDATE bonne_pratique SET active = ? WHERE id = ?");
    $stmt->bind_param("ii", $active, $id);
}

echo json_encode(["status" => $stmt->execute() ? "success" : "error_update"]);
