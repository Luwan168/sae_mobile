<?php
// openminds_server/deleteAccount.php
require_once 'config.php';

$token = isset($_POST['token']) ? $_POST['token'] : '';
if ($token === '') { echo json_encode(["status" => "missing_token"]); exit(); }

$stmt = $db_con->prepare("SELECT id, role FROM user WHERE token = ?");
$stmt->bind_param("s", $token);
$stmt->execute();
$row = $stmt->get_result()->fetch_assoc();

if (!$row) { echo json_encode(["status" => "invalid_token"]); exit(); }

// Un admin ne peut pas supprimer son propre compte
if ($row['role'] === 'admin') {
    echo json_encode(["status" => "admin_cannot_delete"]); exit();
}

$id = $row['id'];
$s1 = $db_con->prepare("DELETE FROM user_badge  WHERE user_id = ?"); $s1->bind_param("i",$id); $s1->execute();
$s2 = $db_con->prepare("DELETE FROM enrollment  WHERE user_id = ?"); $s2->bind_param("i",$id); $s2->execute();
$s3 = $db_con->prepare("DELETE FROM user        WHERE id      = ?"); $s3->bind_param("i",$id);
if ($s3->execute()) {
    echo json_encode(["status" => "success"]);
} else {
    echo json_encode(["status" => "error_delete"]);
}
