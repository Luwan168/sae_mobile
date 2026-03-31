<?php
// openminds_server/updateProfile.php
require_once 'config.php';

$token   = isset($_POST['token'])   ? $_POST['token']         : '';
$address = isset($_POST['address']) ? trim($_POST['address'])  : '';
$city    = isset($_POST['city'])    ? trim($_POST['city'])     : '';

if ($token === '') { echo json_encode(["status"=>"missing_token"]); exit(); }

$u = $db_con->prepare("SELECT id FROM user WHERE token = ?");
$u->bind_param("s", $token); $u->execute();
$row = $u->get_result()->fetch_assoc();
if (!$row) { echo json_encode(["status"=>"invalid_token"]); exit(); }

$stmt = $db_con->prepare("UPDATE user SET address=?, city=? WHERE id=?");
$stmt->bind_param("ssi", $address, $city, $row['id']);
echo json_encode(["status" => $stmt->execute() ? "success" : "error_update"]);
