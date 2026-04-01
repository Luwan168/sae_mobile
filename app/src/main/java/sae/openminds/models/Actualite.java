package sae.openminds.models;

// ============================================================
//  app/src/main/java/sae/openminds/models/Actualite.java
// ============================================================
public class Actualite {
    public int    id;
    public String title;
    public String content;
    public String image_url;      // null si pas d'image
    public String published_at;
    public String author;
}