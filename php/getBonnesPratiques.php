<?php
// openminds_server/getBonnesPratiques.php
require_once 'config.php';
$result = $db_con->query("SELECT id, content, theme FROM bonne_pratique WHERE active=1 AND status='approved' ORDER BY RAND() LIMIT 1");
$row = $result->fetch_assoc();
echo json_encode(["status"=>"success","pratique"=>$row]);
