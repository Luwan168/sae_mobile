<?php
// openminds_server/login.php
require_once 'config.php';

$email    = isset($_POST['email'])    ? trim($_POST['email'])    : '';
$password = isset($_POST['password']) ? $_POST['password']       : '';

if ($email === '' || $password === '') {
    echo json_encode(["status" => "missing_fields"]);
    exit();
}

$stmt = $db_con->prepare("SELECT id, password, role, token FROM user WHERE email = ?");
$stmt->bind_param("s", $email);
$stmt->execute();
$result = $stmt->get_result();

if ($row = $result->fetch_assoc()) {
    if (password_verify($password, $row['password'])) {
        $token = $row['token'];
        if (empty($token)) {
            $token = bin2hex(random_bytes(16));
            $upd = $db_con->prepare("UPDATE user SET token = ? WHERE id = ?");
            $upd->bind_param("si", $token, $row['id']);
            $upd->execute();
        }
        echo json_encode([
            "status" => "success",
            "token"  => $token,
            "role"   => $row['role']
        ]);
    } else {
        echo json_encode(["status" => "wrong_credentials"]);
    }
} else {
    echo json_encode(["status" => "wrong_credentials"]);
}
