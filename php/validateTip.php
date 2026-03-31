<?php
// openminds_server/validateTip.php
// Admin uniquement : approuve ou rejette un tip
require_once 'config.php';

$token  = isset($_POST['token'])  ? $_POST['token']       : '';
$id     = isset($_POST['id'])     ? (int)$_POST['id']     : 0;
$action = isset($_POST['action']) ? $_POST['action']      : ''; // 'approve' ou 'reject'

if ($token === '' || $id === 0 || !in_array($action, ['approve', 'reject'])) {
    echo json_encode(["status" => "missing_fields"]); exit();
}

$u = $db_con->prepare("SELECT id, role FROM user WHERE token = ?");
$u->bind_param("s", $token); $u->execute();
$user = $u->get_result()->fetch_assoc();
if (!$user || $user['role'] !== 'admin') {
    echo json_encode(["status" => "forbidden"]); exit();
}

$status = ($action === 'approve') ? 'approved' : 'rejected';
$active = ($action === 'approve') ? 1 : 0;

$stmt = $db_con->prepare(
    "UPDATE bonne_pratique SET status = ?, active = ? WHERE id = ?"
);
$stmt->bind_param("sii", $status, $active, $id);
echo json_encode(["status" => $stmt->execute() ? "success" : "error_update"]);
