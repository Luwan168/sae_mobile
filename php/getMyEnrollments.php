<?php
// openminds_server/getMyEnrollments.php
// Utilisé par le FormateurFragment : retourne les inscriptions
// aux formations créées par le formateur connecté
require_once 'config.php';

$token = isset($_POST['token']) ? $_POST['token'] : '';
if ($token === '') { echo json_encode(["status"=>"missing_token"]); exit(); }

$u = $db_con->prepare("SELECT id, role FROM user WHERE token = ?");
$u->bind_param("s", $token); $u->execute();
$user = $u->get_result()->fetch_assoc();
if (!$user) { echo json_encode(["status"=>"invalid_token"]); exit(); }

// Formateur voit les inscriptions à SES formations
// Admin voit toutes les inscriptions
$sql = ($user['role'] === 'admin')
    ? "SELECT e.id, e.status, e.score, e.enrolled_at,
              f.title, f.theme,
              CONCAT(u.firstname,' ',u.lastname) AS student_name
       FROM enrollment e
       JOIN formation f ON f.id = e.formation_id
       JOIN user u ON u.id = e.user_id
       ORDER BY e.enrolled_at DESC"
    : "SELECT e.id, e.status, e.score, e.enrolled_at,
              f.title, f.theme,
              CONCAT(u.firstname,' ',u.lastname) AS student_name
       FROM enrollment e
       JOIN formation f ON f.id = e.formation_id
       JOIN user u ON u.id = e.user_id
       WHERE f.created_by = {$user['id']}
       ORDER BY e.enrolled_at DESC";

$result = $db_con->query($sql);
echo json_encode(["status"=>"success","enrollments"=>$result->fetch_all(MYSQLI_ASSOC)]);
