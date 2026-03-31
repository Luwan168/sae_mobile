<?php
// openminds_server/getFormations.php
require_once 'config.php';

$token = isset($_POST['token']) ? $_POST['token'] : '';
if ($token === '') { echo json_encode(["status" => "missing_token"]); exit(); }

$chk = $db_con->prepare("SELECT id FROM user WHERE token = ?");
$chk->bind_param("s", $token);
$chk->execute();
if ($chk->get_result()->num_rows === 0) {
    echo json_encode(["status" => "invalid_token"]); exit();
}

$result = $db_con->query(
    "SELECT f.id, f.title, f.description, f.theme, f.type, f.location,
            CONCAT(u.firstname,' ',u.lastname) AS author
     FROM formation f
     LEFT JOIN user u ON u.id = f.created_by
     ORDER BY f.id DESC"
);

$formations = [];
while ($row = $result->fetch_assoc()) {
    $formations[] = $row;
}
echo json_encode(["status" => "success", "formations" => $formations]);
