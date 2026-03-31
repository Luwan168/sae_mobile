package sae.openminds.models;

// ============================================================
//  app/src/main/java/sae/openminds/models/Enrollment.java
// ============================================================
public class Enrollment {
    public int    id;
    public String status;       // "en_cours", "termine", "abandonne"
    public int    score;
    public String enrolled_at;
    public String title;        // formation title
    public String theme;
}
