<?php
// FICHIER TEMPORAIRE - À SUPPRIMER APRÈS USAGE
// Appelez cette URL dans le navigateur pour obtenir le hash de Admin1234
// puis utilisez-le dans phpMyAdmin pour mettre à jour les mots de passe

$password = 'Admin1234';
$hash = password_hash($password, PASSWORD_BCRYPT);

// Met à jour directement les 3 comptes de test
require_once 'config.php';
$stmt = $db_con->prepare("UPDATE user SET password = ? WHERE email IN ('admin@openminds.fr','formateur@openminds.fr','benevole@openminds.fr')");
$stmt->bind_param("s", $hash);
$stmt->execute();

echo json_encode([
    "status"   => "success",
    "message"  => "Mots de passe mis à jour pour les 3 comptes de test",
    "hash"     => $hash,
    "updated"  => $stmt->affected_rows
]);
?>
