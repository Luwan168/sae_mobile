<?php
// openminds_server/getFormationStudents.php
require_once 'config.php';

$token        = isset($_POST['token'])        ? $_POST['token']           : '';
$formation_id = isset($_POST['formation_id']) ? (int)$_POST['formation_id'] : 0;
if ($token === '' || $formation_id === 0) { echo json_encode(["status"=>"missing_fields"]); exit(); }

$u = $db_con->prepare("SELECT id, role FROM user WHERE token = ?");
$u->bind_param("s", $token); $u->execute();
$user = $u->get_result()->fetch_assoc();
if (!$user || !in_array($user['role'], ['formateur','admin'])) {
    echo json_encode(["status"=>"forbidden"]); exit();
}

$stmt = $db_con->prepare(
    "SELECT u.id, u.firstname, u.lastname, u.email,
            e.status, e.score, e.enrolled_at
     FROM enrollment e
     JOIN user u ON u.id = e.user_id
     WHERE e.formation_id = ?
     ORDER BY e.enrolled_at DESC"
);
$stmt->bind_param("i", $formation_id); $stmt->execute();
$students = $stmt->get_result()->fetch_all(MYSQLI_ASSOC);

$f = $db_con->prepare("SELECT title FROM formation WHERE id = ?");
$f->bind_param("i", $formation_id); $f->execute();
$formation = $f->get_result()->fetch_assoc();

echo json_encode([
    "status"          => "success",
    "formation_title" => $formation ? $formation['title'] : '',
    "students"        => $students
], JSON_UNESCAPED_UNICODE);
