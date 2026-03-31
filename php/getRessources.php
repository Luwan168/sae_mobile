<?php
// openminds_server/getRessources.php
require_once 'config.php';
$token = isset($_POST['token']) ? $_POST['token'] : '';
if ($token==='') { echo json_encode(["status"=>"missing_token"]); exit(); }
$u = $db_con->prepare("SELECT id FROM user WHERE token=?"); $u->bind_param("s",$token); $u->execute();
if ($u->get_result()->num_rows===0) { echo json_encode(["status"=>"invalid_token"]); exit(); }

$result = $db_con->query(
    "SELECT r.id, r.title, r.content, r.theme, r.type, CONCAT(u.firstname,' ',u.lastname) AS author
     FROM ressource r LEFT JOIN user u ON u.id=r.created_by ORDER BY r.id DESC"
);
echo json_encode(["status"=>"success","ressources"=>$result->fetch_all(MYSQLI_ASSOC)]);
