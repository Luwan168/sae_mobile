<?php
// openminds_server/submitQuiz.php
require_once 'config.php';

$token        = isset($_POST['token'])        ? $_POST['token']        : '';
$formation_id = isset($_POST['formation_id']) ? (int)$_POST['formation_id'] : 0;
$answers_raw  = isset($_POST['answers'])      ? $_POST['answers']      : '';

if ($token==='' || $formation_id===0 || $answers_raw==='') {
    echo json_encode(["status"=>"missing_fields"]); exit();
}

$u = $db_con->prepare("SELECT id FROM user WHERE token = ?");
$u->bind_param("s",$token); $u->execute();
$row = $u->get_result()->fetch_assoc();
if (!$row) { echo json_encode(["status"=>"invalid_token"]); exit(); }
$user_id = $row['id'];

$answers = json_decode($answers_raw, true);

$stmt = $db_con->prepare("SELECT id, correct_answer FROM quiz WHERE formation_id = ?");
$stmt->bind_param("i",$formation_id); $stmt->execute();
$questions = $stmt->get_result()->fetch_all(MYSQLI_ASSOC);

$total   = count($questions);
$correct = 0;
foreach ($questions as $q) {
    if (isset($answers[$q['id']]) && $answers[$q['id']] === $q['correct_answer']) {
        $correct++;
    }
}
$score = $total > 0 ? (int)round(($correct / $total) * 100) : 0;

$upd = $db_con->prepare(
    "UPDATE enrollment SET score=?, status='termine' WHERE user_id=? AND formation_id=?"
);
$upd->bind_param("iii", $score, $user_id, $formation_id);
$upd->execute();

// Attribuer badge si score >= 70
$badge_awarded = false;
if ($score >= 70) {
    $b = $db_con->prepare("SELECT id FROM badge WHERE formation_id = ? LIMIT 1");
    $b->bind_param("i",$formation_id); $b->execute();
    $badge = $b->get_result()->fetch_assoc();
    if ($badge) {
        $ins = $db_con->prepare(
            "INSERT IGNORE INTO user_badge (user_id, badge_id) VALUES (?,?)"
        );
        $ins->bind_param("ii",$user_id,$badge['id']);
        $badge_awarded = $ins->execute();
    }
}

echo json_encode([
    "status"        => "success",
    "score"         => $score,
    "badge_awarded" => $badge_awarded
]);
