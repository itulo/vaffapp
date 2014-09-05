package italo.vaffapp.app.databases;

/**
 * Created by iarmenti on 4/26/14.
 */
public class Insult {

    int _id;
    String _insult;
    String _desc;
    String _english;
    int _region_id;

    // constructors
    public Insult(int id, String insult, String desc) {
        this._id = id;
        this._insult = insult;
        this._desc = desc;
    }
    public Insult(String insult, String desc) {
        this._insult = insult;
        this._desc = desc;
    }
    public Insult() {}

    void setInsult(String i) { _insult = i; }
    void setDesc(String d) { _desc = d; }
    void setEnglish(String e) { _english = e; }
    void setRegionId(int ri){ _region_id = ri; }
    public String getInsult(){
        return _insult;
    }
    public String getDesc(){
        return _desc;
    }
    public String getEnglish(){
        return _english;
    }
    public int getRegionId(){
        return _region_id;
    }
}
