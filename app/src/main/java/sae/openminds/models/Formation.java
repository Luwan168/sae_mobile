package sae.openminds.models;

// ============================================================
//  app/src/main/java/sae/openminds/models/Formation.java
//  Le type (en_ligne/presentiel) a été supprimé :
//  toutes les formations sont désormais en présentiel
// ============================================================
public class Formation {
    public int     id;
    public String  title;
    public String  description;
    public String  theme;
    public String  location;
    public String  author;
    public int     max_places;     // 0 = illimité
    public int     enrolled_count;
    public int     places_left;    // places restantes
    public boolean is_full;
}
