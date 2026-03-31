<?php
// openminds_server/getBadges.php
require_once 'config.php';
$token = isset($_POST['token']) ? $_POST['token'] : '';
if ($token==='') { echo json_encode(["status"=>"missing_token"]); exit(); }

$u = $db_con->prepare("SELECT id FROM user WHERE token = ?");
$u->bind_param("s",$token); $u->execute();
$row = $u->get_result()->fetch_assoc();
if (!$row) { echo json_encode(["status"=>"invalid_token"]); exit(); }

$stmt = $db_con->prepare(
    "SELECT b.id, b.name, b.image_url, f.title AS formation_title, ub.obtained_at
     FROM user_badge ub
     JOIN badge b    ON b.id = ub.badge_id
     LEFT JOIN formation f ON f.id = b.formation_id
     WHERE ub.user_id = ? ORDER BY ub.obtained_at DESC"
);
$stmt->bind_param("i",$row['id']); $stmt->execute();
$badges = $stmt->get_result()->fetch_all(MYSQLI_ASSOC);

echo json_encode(["status"=>"success","badges"=>$badges]);
