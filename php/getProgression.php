<?php
// openminds_server/getProgression.php
require_once 'config.php';
$token = isset($_POST['token']) ? $_POST['token'] : '';
if ($token==='') { echo json_encode(["status"=>"missing_token"]); exit(); }

$u = $db_con->prepare("SELECT id, firstname, lastname, role FROM user WHERE token = ?");
$u->bind_param("s",$token); $u->execute();
$user = $u->get_result()->fetch_assoc();
if (!$user) { echo json_encode(["status"=>"invalid_token"]); exit(); }

$stmt = $db_con->prepare(
    "SELECT e.id, e.status, e.score, e.enrolled_at, f.title, f.theme
     FROM enrollment e JOIN formation f ON f.id = e.formation_id
     WHERE e.user_id = ? ORDER BY e.enrolled_at DESC"
);
$stmt->bind_param("i",$user['id']); $stmt->execute();
$enrollments = $stmt->get_result()->fetch_all(MYSQLI_ASSOC);

$total    = count($enrollments);
$done     = count(array_filter($enrollments, fn($e) => $e['status']==='termine'));
$avgScore = $done > 0
    ? round(array_sum(array_column(array_filter($enrollments,fn($e)=>$e['status']==='termine'),'score'))/$done)
    : 0;

echo json_encode([
    "status"      => "success",
    "user"        => $user,
    "enrollments" => $enrollments,
    "stats"       => ["total"=>$total,"done"=>$done,"avg_score"=>$avgScore]
]);
