<?php
// openminds_server/getStats.php  — Admin uniquement
require_once 'config.php';

$token = isset($_POST['token']) ? $_POST['token'] : '';
if ($token === '') { echo json_encode(["status"=>"missing_token"]); exit(); }

$u = $db_con->prepare("SELECT id, role FROM user WHERE token = ?");
$u->bind_param("s", $token); $u->execute();
$user = $u->get_result()->fetch_assoc();
if (!$user || $user['role'] !== 'admin') {
    echo json_encode(["status"=>"forbidden"]); exit();
}

$total_users      = $db_con->query("SELECT COUNT(*) AS n FROM user")->fetch_assoc()['n'];
$total_formations = $db_con->query("SELECT COUNT(*) AS n FROM formation")->fetch_assoc()['n'];
$total_enroll     = $db_con->query("SELECT COUNT(*) AS n FROM enrollment")->fetch_assoc()['n'];
$avg_row          = $db_con->query("SELECT IFNULL(AVG(score),0) AS avg FROM enrollment WHERE status='termine'")->fetch_assoc();
$avg_score        = (int) round($avg_row['avg']);

echo json_encode([
    "status" => "success",
    "stats"  => [
        "total_users"       => (int)$total_users,
        "total_formations"  => (int)$total_formations,
        "total_enrollments" => (int)$total_enroll,
        "avg_score"         => $avg_score
    ]
]);
