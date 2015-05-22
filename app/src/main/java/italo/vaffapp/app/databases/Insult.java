package italo.vaffapp.app.databases;

import italo.vaffapp.app.util.SharedMethods;

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

    void setId(int i) {
        _id = i;
    }
    void setInsult(String i) { _insult = i; }
    void setDesc(String d) { _desc = d; }
    void setEnglish(String e) { _english = e; }
    void setRegionId(int ri){ _region_id = ri; }
    public int getId() { return _id; }
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

    /* this method is required in the Master/Detail flow
       to show a list of Insult only by _insult
     */
    public String toString() {
        String region_name;

        if (_region_id == 17)
            region_name = "Trentino";
        else if ( _region_id == 7)
            region_name = "Friuli";
        else
            region_name = SharedMethods.getRegionFromId(_region_id);

        return region_name.toUpperCase() + " -\n" + _insult;
    }
}
