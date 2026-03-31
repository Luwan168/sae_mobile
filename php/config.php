<?php
// ============================================================
//  openminds_server/config.php
//  Inclure ce fichier en haut de chaque script PHP
// ============================================================
$db_host = "mysql-openminds.alwaysdata.net";
$db_uid  = "openminds";          // votre identifiant AlwaysData
$db_pass = "VOTRE_MOT_DE_PASSE"; // à remplacer
$db_name = "openminds_bd";

$db_con = new mysqli($db_host, $db_uid, $db_pass, $db_name);
$db_con->set_charset("utf8mb4");

if ($db_con->connect_error) {
    http_response_code(500);
    die(json_encode(["status" => "error", "message" => "DB connection failed"]));
}

header("Content-Type: application/json; charset=utf-8");
