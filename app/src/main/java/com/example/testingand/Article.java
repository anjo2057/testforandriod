package com.example.testingand;

import java.io.Serializable;
import java.text.ParseException;
import java.util.Hashtable;
import org.json.simple.JSONObject;
import com.example.testingand.exceptions.ServerCommunicationError;

public class Article extends ModelEntity implements Serializable {

    private String titleText;
    private String category;
    private String abstractText;
    private String bodyText;
    private String footerText;
    private int idUser;
    private Image mainImage;
    private String imageDescription;
    private String thumbnail;
    private String updatedAtIso;
    private String createdAtIso;


    private String parseStringFromJson(JSONObject jsonArticle, String key, String def){
        Object in = jsonArticle.getOrDefault(key,def);
        return (in==null?def:in).toString();
    }

    // Normalisera inkommande datum till "yyyy-MM-dd HH:mm"
    private String normalizeDate(Object raw) {
        if (raw == null) return "";
        try {
            String s = raw.toString().trim();
            if (s.isEmpty() || "null".equalsIgnoreCase(s)) return "";
            String cleaned = s.replace('T', ' ').replace('Z', ' ').trim();
            if (cleaned.length() >= 16) cleaned = cleaned.substring(0, 16);
            return cleaned;
        } catch (Exception e) {
            return raw.toString(); // sista fallback
        }
    }

    // plockar första icke-tomma datumet från en lista av nycklar
    private String firstNonEmptyDate(JSONObject json, String... keys) {
        for (String k : keys) {
            String normalized = normalizeDate(json.get(k));
            if (normalized != null && !normalized.isEmpty())
                return normalized;
        }
        return "";
    }

    @SuppressWarnings("unchecked")
    protected Article(ModelManager mm, JSONObject jsonArticle){
        super(mm);
        try{
            id = Integer.parseInt(jsonArticle.get("id").toString());
            idUser = Integer.parseInt(parseStringFromJson(jsonArticle,"id_user","0"));
            titleText = parseStringFromJson(jsonArticle,"title","");
            category = parseStringFromJson(jsonArticle,"category","");
            abstractText = parseStringFromJson(jsonArticle,"abstract","");
            bodyText = parseStringFromJson(jsonArticle,"body","");
            footerText = parseStringFromJson(
                    jsonArticle,
                    "subtitle",
                    parseStringFromJson(jsonArticle,"footer","")
            );

            imageDescription = parseStringFromJson(jsonArticle,"image_description","");
            thumbnail = parseStringFromJson(jsonArticle,"thumbnail_image","");

            String imageData = parseStringFromJson(jsonArticle,"image_data","");

            if (imageData!=null && !imageData.isEmpty())
                mainImage = new Image(mm, 1, imageDescription, id, imageData);

            String updateDateFromJson = normalizeDate(jsonArticle.get("update_date"));

            updatedAtIso = !updateDateFromJson.isEmpty()
                    ? updateDateFromJson
                    : firstNonEmptyDate(jsonArticle,
                    "updatedAt");

            createdAtIso = firstNonEmptyDate(jsonArticle,
                    "createdAt");

            if (createdAtIso.isEmpty()) createdAtIso = updatedAtIso;

        }catch(Exception e){
            Logger.log(Logger.ERROR, "ERROR: Error parsing Article: from json"+jsonArticle+"\n"+e.getMessage());
            throw new IllegalArgumentException("ERROR: Error parsing Article: from json"+jsonArticle);
        }
    }

    public Article(ModelManager mm, String category, String titleText, String abstractText, String body, String footer){
        super(mm);
        id = -1;
        this.category = category;
        idUser = Integer.parseInt(mm.getIdUser());
        this.abstractText = abstractText;
        this.titleText = titleText;
        bodyText = body;
        footerText = footer;
        // createdAtIso = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date());
    }

    public void setId(int id){
        if (id <1){
            throw new IllegalArgumentException("ERROR: Error setting a wrong id to an article:"+id);
        }
        if (this.id>0 ){
            throw new IllegalArgumentException("ERROR: Error setting an id to an article with an already valid id:"+this.id);
        }
        this.id = id;
    }

    public String getTitleText() {
        return titleText;
    }

    public String getCategory() {
        return category;
    }
    public void setCategory(String category) {
        this.category= category;
    }
    public void setTitleText(String titleText) {
        this.titleText = titleText;
    }
    public String getAbstractText() {
        return abstractText;
    }
    public void setAbstractText(String abstractText) {
        this.abstractText = abstractText;
    }
    public String getBodyText() {
        return bodyText;
    }
    public void setBodyText(String bodyText) {
        this.bodyText = bodyText;
    }
    public String getFooterText() {
        return footerText;
    }
    public void setFooterText(String footerText) {
        this.footerText = footerText;
    }

    public int getIdUser(){
        return idUser;
    }

    public Image getImage() throws ServerCommunicationError {
        Image image = mainImage;
        if (mainImage==null && thumbnail!=null && !thumbnail.isEmpty()){
            image = new Image(mm,1,"",getId(),thumbnail);
        }
        return image;
    }
    public void setImage(Image image) {
        this.mainImage = image;
    }

    public Image addImage(String b64Image, String description) throws ServerCommunicationError{
        int order = 1;
        Image img =new Image(mm, order, description, getId(), b64Image);
        mainImage= img;
        return img;
    }

    public String getUpdatedAtIso() { return updatedAtIso; }
    public String getCreatedAtIso() { return createdAtIso; }

    public String getUpdatedAtDisplay() {
        String v = (updatedAtIso != null && !updatedAtIso.isEmpty()) ? updatedAtIso : createdAtIso;
        return (v == null || v.isEmpty()) ? "–" : v;
    }

    public long getUpdatedAtSortKey() {
        String iso = (updatedAtIso != null && !updatedAtIso.isEmpty()) ? updatedAtIso : createdAtIso;
        if (iso == null || iso.isEmpty()) return 0L;
        try {
            String key = iso.substring(0, Math.min(16, iso.length())).replaceAll("[^0-9]", "");
            if (key.length() == 0) return 0L;
            return Long.parseLong(key);
        } catch (Exception e) {
            return 0L;
        }
    }

    @Override
    public String toString() {
        return "Article [id=" + getId()
                //+ "isPublic=" + isPublic + ", isDeleted=" + isDeleted
                +", titleText=" + titleText
                +", abstractText=" + abstractText
                +  ", bodyText="	+ bodyText + ", footerText=" + footerText
                //+ ", publicationDate=" + Utils.dateToString(publicationDate)
                +", image_description=" + imageDescription
                +", image_data=" + mainImage
                +", thumbnail=" + thumbnail
                + "]";
    }

    public Hashtable<String,String> getAttributes(){
        Hashtable<String,String> res = new Hashtable<String,String>();
        //res.put("is_public", ""+(isPublic?1:0));
        //res.put("id_user", ""+idUser);
        res.put("category", category);
        res.put("abstract", abstractText);
        res.put("title", titleText);
        //res.put("is_deleted", ""+(isDeleted?1:0));
        res.put("body", bodyText);
        res.put("subtitle", footerText);
        if (mainImage!=null){
            res.put("image_data", mainImage.getImage());
            res.put("image_media_type", "image/png");
        }

        if (mainImage!=null && mainImage.getDescription()!=null && !mainImage.getDescription().isEmpty())
            res.put("image_description", mainImage.getDescription());
        else if (imageDescription!=null && !imageDescription.isEmpty())
            res.put("image_description", imageDescription);

        //res.put("publication_date", publicationDate==null?null:Utils.dateToString(publicationDate));
        return res;
    }
}
