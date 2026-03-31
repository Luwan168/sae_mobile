<?php
// openminds_server/getQuiz.php
require_once 'config.php';

$token        = isset($_POST['token'])        ? $_POST['token']        : '';
$formation_id = isset($_POST['formation_id']) ? (int)$_POST['formation_id'] : 0;
if ($token==='' || $formation_id===0) { echo json_encode(["status"=>"missing_fields"]); exit(); }

$u = $db_con->prepare("SELECT id FROM user WHERE token = ?");
$u->bind_param("s",$token); $u->execute();
if ($u->get_result()->num_rows===0) { echo json_encode(["status"=>"invalid_token"]); exit(); }

$stmt = $db_con->prepare("SELECT id, question, choices FROM quiz WHERE formation_id = ?");
$stmt->bind_param("i", $formation_id); $stmt->execute();
$result = $stmt->get_result();

$questions = [];
while ($row = $result->fetch_assoc()) {
    $row['choices'] = json_decode($row['choices'], true);
    $questions[] = $row;
}
echo json_encode(["status"=>"success","questions"=>$questions]);
