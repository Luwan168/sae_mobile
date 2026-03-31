<?php
// openminds_server/addQuestion.php
// Formateur + Admin : ajouter une question à une formation
require_once 'config.php';

$token          = isset($_POST['token'])          ? $_POST['token']                  : '';
$formation_id   = isset($_POST['formation_id'])   ? (int)$_POST['formation_id']      : 0;
$question       = isset($_POST['question'])       ? trim($_POST['question'])          : '';
$choices_raw    = isset($_POST['choices'])        ? $_POST['choices']                : '';
$correct_answer = isset($_POST['correct_answer']) ? trim($_POST['correct_answer'])   : '';

if ($token === '' || $formation_id === 0 || $question === '' || $choices_raw === '' || $correct_answer === '') {
    echo json_encode(["status" => "missing_fields"]); exit();
}

$u = $db_con->prepare("SELECT id, role FROM user WHERE token = ?");
$u->bind_param("s", $token); $u->execute();
$user = $u->get_result()->fetch_assoc();
if (!$user || !in_array($user['role'], ['formateur', 'admin'])) {
    echo json_encode(["status" => "forbidden"]); exit();
}

// Vérifier que la formation appartient au formateur (sauf admin)
if ($user['role'] === 'formateur') {
    $chk = $db_con->prepare("SELECT id FROM formation WHERE id = ? AND created_by = ?");
    $chk->bind_param("ii", $formation_id, $user['id']); $chk->execute();
    if ($chk->get_result()->num_rows === 0) {
        echo json_encode(["status" => "forbidden"]); exit();
    }
}

// choices doit être un JSON valide ex: ["Choix A","Choix B","Choix C"]
$choices = json_decode($choices_raw);
if (!is_array($choices) || count($choices) < 2) {
    echo json_encode(["status" => "invalid_choices"]); exit();
}
$choices_json = json_encode($choices, JSON_UNESCAPED_UNICODE);

$stmt = $db_con->prepare(
    "INSERT INTO quiz (formation_id, question, choices, correct_answer) VALUES (?, ?, ?, ?)"
);
$stmt->bind_param("isss", $formation_id, $question, $choices_json, $correct_answer);
echo json_encode(["status" => $stmt->execute() ? "success" : "error_insert"]);
