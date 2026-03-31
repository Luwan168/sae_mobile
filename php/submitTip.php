<?php
// openminds_server/submitTip.php
// Tout utilisateur connecté peut soumettre un tip
// Le tip est en attente (pending) jusqu'à validation admin
require_once 'config.php';

$token   = isset($_POST['token'])   ? $_POST['token']        : '';
$content = isset($_POST['content']) ? trim($_POST['content']) : '';
$theme   = isset($_POST['theme'])   ? trim($_POST['theme'])   : 'general';

if ($token === '' || $content === '') {
    echo json_encode(["status" => "missing_fields"]); exit();
}

$u = $db_con->prepare("SELECT id FROM user WHERE token = ?");
$u->bind_param("s", $token); $u->execute();
$user = $u->get_result()->fetch_assoc();
if (!$user) { echo json_encode(["status" => "invalid_token"]); exit(); }

$stmt = $db_con->prepare(
    "INSERT INTO bonne_pratique (content, theme, active, status, submitted_by)
     VALUES (?, ?, 0, 'pending', ?)"
);
$stmt->bind_param("ssi", $content, $theme, $user['id']);
echo json_encode(["status" => $stmt->execute() ? "success" : "error_insert"]);
