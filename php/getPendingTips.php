<?php
// openminds_server/getPendingTips.php
// Admin uniquement : récupère les tips en attente de validation
require_once 'config.php';

$token = isset($_POST['token']) ? $_POST['token'] : '';
if ($token === '') { echo json_encode(["status" => "missing_token"]); exit(); }

$u = $db_con->prepare("SELECT id, role FROM user WHERE token = ?");
$u->bind_param("s", $token); $u->execute();
$user = $u->get_result()->fetch_assoc();
if (!$user || $user['role'] !== 'admin') {
    echo json_encode(["status" => "forbidden"]); exit();
}

$result = $db_con->query(
    "SELECT bp.id, bp.content, bp.theme, bp.status,
            CONCAT(u.firstname,' ',u.lastname) AS submitted_by_name
     FROM bonne_pratique bp
     LEFT JOIN user u ON u.id = bp.submitted_by
     WHERE bp.status = 'pending'
     ORDER BY bp.id ASC"
);
echo json_encode([
    "status"   => "success",
    "pratiques" => $result->fetch_all(MYSQLI_ASSOC)
], JSON_UNESCAPED_UNICODE);
