<?php
// openminds_server/getAllBonnesPratiques.php — Admin uniquement
require_once 'config.php';

$token = isset($_POST['token']) ? $_POST['token'] : '';
if ($token === '') { echo json_encode(["status"=>"missing_token"]); exit(); }

$u = $db_con->prepare("SELECT id, role FROM user WHERE token = ?");
$u->bind_param("s", $token); $u->execute();
$user = $u->get_result()->fetch_assoc();
if (!$user || $user['role'] !== 'admin') {
    echo json_encode(["status"=>"forbidden"]); exit();
}

$result = $db_con->query("SELECT id, content, theme, active FROM bonne_pratique ORDER BY id");
echo json_encode(["status"=>"success","pratiques"=>$result->fetch_all(MYSQLI_ASSOC)]);
