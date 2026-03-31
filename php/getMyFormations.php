<?php
// openminds_server/getMyFormations.php
require_once 'config.php';

$token = isset($_POST['token']) ? $_POST['token'] : '';
if ($token === '') { echo json_encode(["status"=>"missing_token"]); exit(); }

$u = $db_con->prepare("SELECT id, role FROM user WHERE token = ?");
$u->bind_param("s", $token); $u->execute();
$user = $u->get_result()->fetch_assoc();
if (!$user || !in_array($user['role'], ['formateur','admin'])) {
    echo json_encode(["status"=>"forbidden"]); exit();
}

// Admin voit toutes les formations, formateur seulement les siennes
$sql = ($user['role'] === 'admin')
    ? "SELECT f.id, f.title, f.theme, f.type, f.location,
              (SELECT COUNT(*) FROM enrollment e WHERE e.formation_id = f.id) AS student_count
       FROM formation f ORDER BY f.id DESC"
    : "SELECT f.id, f.title, f.theme, f.type, f.location,
              (SELECT COUNT(*) FROM enrollment e WHERE e.formation_id = f.id) AS student_count
       FROM formation f WHERE f.created_by = {$user['id']} ORDER BY f.id DESC";

$result = $db_con->query($sql);
echo json_encode([
    "status"     => "success",
    "formations" => $result->fetch_all(MYSQLI_ASSOC)
], JSON_UNESCAPED_UNICODE);
