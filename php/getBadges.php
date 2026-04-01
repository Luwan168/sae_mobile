<?php
// openminds_server/getBadges.php
require_once 'config.php';
$token = isset($_POST['token']) ? $_POST['token'] : '';
if ($token==='') { echo json_encode(["status"=>"missing_token"]); exit(); }

$u = $db_con->prepare("SELECT id FROM user WHERE token = ?");
$u->bind_param("s",$token); $u->execute();
$row = $u->get_result()->fetch_assoc();
if (!$row) { echo json_encode(["status"=>"invalid_token"]); exit(); }


$sql = "SELECT 
            b.id, 
            b.title, 
            b.image, 
            f.title as formation_name,
            e.score
        FROM badge b
        INNER JOIN enrollment e ON b.formation_id = e.formation_id
        INNER JOIN formation f ON f.id = b.formation_id
        WHERE e.user_id = ? 
          AND e.status = 'termine' 
          AND e.score >= 75";

$stmt = $db_con->prepare($sql);
$stmt->bind_param("i", $user_id);
$stmt->execute();
$badges = $stmt->get_result()->fetch_all(MYSQLI_ASSOC);




echo json_encode(["status"=>"success","badges"=>$badges]);
