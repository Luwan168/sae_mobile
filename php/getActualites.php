<?php
// openminds_server/getActualites.php
require_once 'config.php';
$token = isset($_POST['token']) ? $_POST['token'] : '';
if ($token==='') { echo json_encode(["status"=>"missing_token"]); exit(); }
$u = $db_con->prepare("SELECT id FROM user WHERE token=?"); $u->bind_param("s",$token); $u->execute();
if ($u->get_result()->num_rows===0) { echo json_encode(["status"=>"invalid_token"]); exit(); }

$result = $db_con->query(
    "SELECT a.id, a.title, a.content, a.published_at, CONCAT(u.firstname,' ',u.lastname) AS author
     FROM actualite a LEFT JOIN user u ON u.id=a.created_by ORDER BY a.published_at DESC"
);
echo json_encode(["status"=>"success","actualites"=>$result->fetch_all(MYSQLI_ASSOC)]);
