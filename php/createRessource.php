<?php
// openminds_server/createRessource.php — Formateur + Admin
require_once 'config.php';

$token   = isset($_POST['token'])   ? $_POST['token']          : '';
$title   = isset($_POST['title'])   ? trim($_POST['title'])    : '';
$content = isset($_POST['content']) ? trim($_POST['content'])  : '';
$theme   = isset($_POST['theme'])   ? trim($_POST['theme'])    : '';
$type    = isset($_POST['type'])    ? $_POST['type']           : 'article';

if ($token === '' || $title === '' || $content === '') {
    echo json_encode(["status"=>"missing_fields"]); exit();
}
if (!in_array($type, ['article','guide'])) $type = 'article';

$u = $db_con->prepare("SELECT id, role FROM user WHERE token = ?");
$u->bind_param("s", $token); $u->execute();
$user = $u->get_result()->fetch_assoc();
if (!$user || !in_array($user['role'], ['formateur','admin'])) {
    echo json_encode(["status"=>"forbidden"]); exit();
}

$stmt = $db_con->prepare(
    "INSERT INTO ressource (title, content, theme, type, created_by) VALUES (?,?,?,?,?)"
);
$stmt->bind_param("ssssi", $title, $content, $theme, $type, $user['id']);
echo json_encode(["status" => $stmt->execute() ? "success" : "error_insert"]);
