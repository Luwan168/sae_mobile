<?php
// openminds_server/createFormation.php
// Toutes les formations sont en présentiel — colonne type supprimée
require_once 'config.php';

$token       = isset($_POST['token'])       ? $_POST['token']             : '';
$title       = isset($_POST['title'])       ? trim($_POST['title'])        : '';
$description = isset($_POST['description']) ? trim($_POST['description'])  : '';
$theme       = isset($_POST['theme'])       ? trim($_POST['theme'])        : '';
$location    = isset($_POST['location'])    ? trim($_POST['location'])     : '';
$max_places  = isset($_POST['max_places']) && $_POST['max_places'] !== ''
               ? (int)$_POST['max_places'] : null;

if ($token === '' || $title === '' || $theme === '' || $location === '') {
    echo json_encode(["status" => "missing_fields"]); exit();
}

$u = $db_con->prepare("SELECT id, role, firstname, lastname FROM user WHERE token = ?");
$u->bind_param("s", $token); $u->execute();
$user = $u->get_result()->fetch_assoc();
if (!$user || !in_array($user['role'], ['formateur', 'admin'])) {
    echo json_encode(["status" => "forbidden"]); exit();
}

$stmt = $db_con->prepare(
    "INSERT INTO formation (title, description, theme, location, max_places, created_by)
     VALUES (?, ?, ?, ?, ?, ?)"
);
$stmt->bind_param("ssssii", $title, $description, $theme, $location, $max_places, $user['id']);

if (!$stmt->execute()) {
    echo json_encode(["status" => "error_insert"]); exit();
}
$formation_id = $db_con->insert_id;

// Actualité automatique à la création
$newsTitle   = "Nouvelle formation disponible : $title";
$newsContent = "Une nouvelle formation \"$title\" ($theme) est disponible en présentiel à $location. "
             . ($description ?: "Inscrivez-vous dès maintenant !")
             . " — Proposée par " . $user['firstname'] . " " . $user['lastname'] . ".";

$news = $db_con->prepare("INSERT INTO actualite (title, content, created_by) VALUES (?, ?, ?)");
$news->bind_param("ssi", $newsTitle, $newsContent, $user['id']);
$news->execute();

echo json_encode([
    "status"       => "success",
    "formation_id" => $formation_id,
    "news_created" => true
], JSON_UNESCAPED_UNICODE);
