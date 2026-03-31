<?php
// openminds_server/createFormation.php
require_once 'config.php';

$token       = isset($_POST['token'])       ? $_POST['token']            : '';
$title       = isset($_POST['title'])       ? trim($_POST['title'])       : '';
$description = isset($_POST['description']) ? trim($_POST['description']) : '';
$theme       = isset($_POST['theme'])       ? trim($_POST['theme'])       : '';
$type        = isset($_POST['type'])        ? $_POST['type']              : 'en_ligne';
$location    = isset($_POST['location'])    ? trim($_POST['location'])    : null;

if ($token === '' || $title === '' || $theme === '') {
    echo json_encode(["status" => "missing_fields"]); exit();
}
if (!in_array($type, ['presentiel', 'en_ligne'])) {
    echo json_encode(["status" => "invalid_type"]); exit();
}

$u = $db_con->prepare("SELECT id, role, firstname, lastname FROM user WHERE token = ?");
$u->bind_param("s", $token); $u->execute();
$user = $u->get_result()->fetch_assoc();
if (!$user || !in_array($user['role'], ['formateur', 'admin'])) {
    echo json_encode(["status" => "forbidden"]); exit();
}

// Créer la formation
$stmt = $db_con->prepare(
    "INSERT INTO formation (title, description, theme, type, location, created_by)
     VALUES (?, ?, ?, ?, ?, ?)"
);
$stmt->bind_param("sssssi", $title, $description, $theme, $type, $location, $user['id']);

if (!$stmt->execute()) {
    echo json_encode(["status" => "error_insert"]); exit();
}
$formation_id = $db_con->insert_id;

// Créer une actualité automatique
$typeLabel   = ($type === 'presentiel') ? "en présentiel à $location" : "en ligne";
$newsTitle   = "Nouvelle formation disponible : $title";
$newsContent = "Une nouvelle formation \"$title\" ($theme) est maintenant disponible $typeLabel. "
             . ($description ? $description : "Inscrivez-vous dès maintenant !");
$newsContent .= " — Proposée par " . $user['firstname'] . " " . $user['lastname'] . ".";

$news = $db_con->prepare(
    "INSERT INTO actualite (title, content, created_by) VALUES (?, ?, ?)"
);
$news->bind_param("ssi", $newsTitle, $newsContent, $user['id']);
$news->execute();

echo json_encode([
    "status"       => "success",
    "formation_id" => $formation_id,
    "news_created" => true
], JSON_UNESCAPED_UNICODE);
