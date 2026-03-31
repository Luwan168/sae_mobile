<?php
// openminds_server/enrollFormation.php
require_once 'config.php';

// Nombre max de formations actives simultanées pour un bénévole
define('MAX_ACTIVE_ENROLLMENTS', 3);

$token        = isset($_POST['token'])        ? $_POST['token']             : '';
$formation_id = isset($_POST['formation_id']) ? (int)$_POST['formation_id'] : 0;
if ($token === '' || $formation_id === 0) { echo json_encode(["status"=>"missing_fields"]); exit(); }

$u = $db_con->prepare("SELECT id, role FROM user WHERE token = ?");
$u->bind_param("s", $token); $u->execute();
$row = $u->get_result()->fetch_assoc();
if (!$row) { echo json_encode(["status"=>"invalid_token"]); exit(); }
$user_id = $row['id'];
$role    = $row['role'];

// 1. Vérifier que la formation existe
$fStmt = $db_con->prepare(
    "SELECT created_by, max_places,
            (SELECT COUNT(*) FROM enrollment e WHERE e.formation_id = f.id) AS enrolled_count
     FROM formation f WHERE f.id = ?"
);
$fStmt->bind_param("i", $formation_id); $fStmt->execute();
$formation = $fStmt->get_result()->fetch_assoc();
if (!$formation) { echo json_encode(["status"=>"formation_not_found"]); exit(); }

// 2. Empêcher de s'inscrire à sa propre formation
if ((int)$formation['created_by'] === $user_id) {
    echo json_encode(["status"=>"own_formation"]); exit();
}

// 3. Empêcher la ré-inscription
$chk = $db_con->prepare("SELECT id FROM enrollment WHERE user_id=? AND formation_id=?");
$chk->bind_param("ii", $user_id, $formation_id); $chk->execute();
if ($chk->get_result()->num_rows > 0) { echo json_encode(["status"=>"already_enrolled"]); exit(); }

// 4. Vérifier les places disponibles
if ($formation['max_places'] !== null) {
    $placesLeft = (int)$formation['max_places'] - (int)$formation['enrolled_count'];
    if ($placesLeft <= 0) {
        echo json_encode(["status"=>"formation_full"]); exit();
    }
}

// 5. Limiter le nombre de formations actives (bénévoles uniquement)
if ($role === 'benevole') {
    $activeStmt = $db_con->prepare(
        "SELECT COUNT(*) AS cnt FROM enrollment WHERE user_id=? AND status='en_cours'"
    );
    $activeStmt->bind_param("i", $user_id); $activeStmt->execute();
    $cnt = (int)$activeStmt->get_result()->fetch_assoc()['cnt'];
    if ($cnt >= MAX_ACTIVE_ENROLLMENTS) {
        echo json_encode(["status"=>"too_many_active", "max"=>MAX_ACTIVE_ENROLLMENTS]); exit();
    }
}

// 6. Inscrire
$ins = $db_con->prepare("INSERT INTO enrollment (user_id, formation_id) VALUES (?,?)");
$ins->bind_param("ii", $user_id, $formation_id);
echo json_encode(["status" => $ins->execute() ? "success" : "error_insert"]);
