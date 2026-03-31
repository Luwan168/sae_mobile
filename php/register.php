<?php
// openminds_server/register.php
require_once 'config.php';

$firstname = isset($_POST['firstname']) ? trim($_POST['firstname']) : '';
$lastname  = isset($_POST['lastname'])  ? trim($_POST['lastname'])  : '';
$email     = isset($_POST['email'])     ? trim($_POST['email'])     : '';
$password  = isset($_POST['password'])  ? $_POST['password']        : '';
$phone     = isset($_POST['phone'])     ? trim($_POST['phone'])     : '';

if ($firstname === '' || $lastname === '' || $email === '' || $password === '') {
    echo json_encode(["status" => "missing_fields"]);
    exit();
}

$chk = $db_con->prepare("SELECT id FROM user WHERE email = ?");
$chk->bind_param("s", $email);
$chk->execute();
if ($chk->get_result()->num_rows > 0) {
    echo json_encode(["status" => "email_exists"]);
    exit();
}

$hashed = password_hash($password, PASSWORD_BCRYPT);
$stmt = $db_con->prepare(
    "INSERT INTO user (firstname, lastname, email, password, phone, role) VALUES (?, ?, ?, ?, ?, 'benevole')"
);
$stmt->bind_param("sssss", $firstname, $lastname, $email, $hashed, $phone);

if ($stmt->execute()) {
    echo json_encode(["status" => "success"]);
} else {
    echo json_encode(["status" => "error_insert"]);
}
