<?php
// openminds_server/getFormationsProches.php
require_once 'config.php';

$token = isset($_POST['token']) ? $_POST['token'] : '';
if ($token === '') { echo json_encode(["status"=>"missing_token"]); exit(); }

$u = $db_con->prepare("SELECT id, city FROM user WHERE token = ?");
$u->bind_param("s", $token); $u->execute();
$user = $u->get_result()->fetch_assoc();
if (!$user) { echo json_encode(["status"=>"invalid_token"]); exit(); }

$city = $user['city'] ?? '';

if (empty($city)) {
    // Pas de ville renseignée : retourner toutes les formations en présentiel
    $result = $db_con->query(
        "SELECT f.id, f.title, f.description, f.theme, f.type, f.location,
                CONCAT(u.firstname,' ',u.lastname) AS author
         FROM formation f LEFT JOIN user u ON u.id = f.created_by
         WHERE f.type = 'presentiel' ORDER BY f.id DESC"
    );
    echo json_encode([
        "status"     => "success",
        "user_city"  => null,
        "formations" => $result->fetch_all(MYSQLI_ASSOC)
    ], JSON_UNESCAPED_UNICODE);
} else {
    // Filtrer par ville correspondante (insensible à la casse)
    $stmt = $db_con->prepare(
        "SELECT f.id, f.title, f.description, f.theme, f.type, f.location,
                CONCAT(u.firstname,' ',u.lastname) AS author
         FROM formation f LEFT JOIN user u ON u.id = f.created_by
         WHERE f.type = 'presentiel'
           AND LOWER(f.location) LIKE LOWER(CONCAT('%',?,'%'))
         ORDER BY f.id DESC"
    );
    $stmt->bind_param("s", $city); $stmt->execute();
    $nearby = $stmt->get_result()->fetch_all(MYSQLI_ASSOC);

    // Si aucun résultat dans la ville, retourner toutes les présentiel
    if (empty($nearby)) {
        $all = $db_con->query(
            "SELECT f.id, f.title, f.description, f.theme, f.type, f.location,
                    CONCAT(u.firstname,' ',u.lastname) AS author
             FROM formation f LEFT JOIN user u ON u.id = f.created_by
             WHERE f.type = 'presentiel' ORDER BY f.id DESC"
        )->fetch_all(MYSQLI_ASSOC);
        echo json_encode([
            "status"     => "success",
            "user_city"  => $city,
            "no_nearby"  => true,
            "formations" => $all
        ], JSON_UNESCAPED_UNICODE);
    } else {
        echo json_encode([
            "status"     => "success",
            "user_city"  => $city,
            "formations" => $nearby
        ], JSON_UNESCAPED_UNICODE);
    }
}
