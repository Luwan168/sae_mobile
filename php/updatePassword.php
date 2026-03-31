<?php
// openminds_server/updatePassword.php
require_once 'config.php';

$token    = isset($_POST['token'])        ? $_POST['token']        : '';
$oldPass  = isset($_POST['old_password']) ? $_POST['old_password'] : '';
$newPass  = isset($_POST['new_password']) ? $_POST['new_password'] : '';

if ($token === '' || $oldPass === '' || $newPass === '') {
    echo json_encode(["status" => "missing_fields"]); exit();
}

$stmt = $db_con->prepare("SELECT id, password FROM user WHERE token = ?");
$stmt->bind_param("s", $token);
$stmt->execute();
$row = $stmt->get_result()->fetch_assoc();

if (!$row) { echo json_encode(["status" => "invalid_token"]); exit(); }

if (!password_verify($oldPass, $row['password'])) {
    echo json_encode(["status" => "wrong_password"]); exit();
}

$hashed = password_hash($newPass, PASSWORD_BCRYPT);
$upd = $db_con->prepare("UPDATE user SET password = ? WHERE id = ?");
$upd->bind_param("si", $hashed, $row['id']);
$upd->execute();
echo json_encode(["status" => "success"]);
