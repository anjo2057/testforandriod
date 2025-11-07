package com.example.testingand;

import java.io.Serializable;
import java.util.Enumeration;
import java.util.Hashtable;

import org.json.simple.JSONObject;

import com.example.testingand.exceptions.ServerCommunicationError;

public abstract class ModelEntity implements Serializable {
    protected int id;
    protected transient ModelManager mm;

    public ModelEntity(ModelManager mm){
        this.mm=mm;
    }

    /**
     * Sets a ModelManager for the entity.
     * This is needed after deserialization since the ModelManager is transient.
     * Aux for editArticle in CreateArticleActivity
     * @param mm ModelManager instance.
     */
    public void setModelManager(ModelManager mm) {
        this.mm = mm;
    }

    public int getId() {
        return id;
    }

    protected abstract Hashtable<String,String> getAttributes();

    public void save() throws ServerCommunicationError{
        int id = mm.save(this);
        this.id = id;
    }

    public void delete() throws ServerCommunicationError{
        mm.delete(this);
    }

    @SuppressWarnings("unchecked")
    public JSONObject toJSON(){
        JSONObject jsonArticle = new JSONObject();
        if(getId()>0)
            jsonArticle.put("id", getId());

        Hashtable<String,String> res = getAttributes();
        Enumeration<String> keys = res.keys();
        while (keys.hasMoreElements()) {
            String key = keys.nextElement();
            jsonArticle.put(key, JSONObject.escape(res.get((key))));
        }
        return jsonArticle;
    }
}
