package italo.vaffapp.app.entity;

import italo.vaffapp.app.common.CommonMethods;

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

    public void setId(int i) {
        _id = i;
    }
    public void setInsult(String i) { _insult = i; }
    public void setDesc(String d) { _desc = d; }
    public void setEnglish(String e) { _english = e; }
    public void setRegionId(int ri){ _region_id = ri; }
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

    // this method is used by the Master/Detail flow
    public String toString() {
        String region_name;

        if (_region_id == 17)
            region_name = "Trentino";
        else if ( _region_id == 7)
            region_name = "Friuli";
        else
            region_name = CommonMethods.getRegionFromId(_region_id);

        return region_name.toUpperCase() + " -\n" + _insult;
    }
}
