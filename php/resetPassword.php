<?php
// openminds_server/resetPassword.php
require_once 'config.php';

$email = isset($_POST['email']) ? trim($_POST['email']) : '';
if ($email === '') { echo json_encode(["status" => "missing_email"]); exit(); }

$chk = $db_con->prepare("SELECT id FROM user WHERE email = ?");
$chk->bind_param("s", $email);
$chk->execute();
if ($chk->get_result()->num_rows === 0) {
    echo json_encode(["status" => "email_not_found"]);
    exit();
}

$newPass = substr(str_shuffle('abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789'), 0, 10);
$hashed  = password_hash($newPass, PASSWORD_BCRYPT);

$upd = $db_con->prepare("UPDATE user SET password = ? WHERE email = ?");
$upd->bind_param("ss", $hashed, $email);
$upd->execute();

echo json_encode(["status" => "success", "new_password" => $newPass]);
