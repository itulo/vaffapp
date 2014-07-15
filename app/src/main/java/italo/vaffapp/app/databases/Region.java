package italo.vaffapp.app.databases;

/**
 * Created by iarmenti on 6/16/14.
 */
public class Region {

    int row_id;
    String _name;

    // constructors
    public Region(int id, String name) {
        this.row_id = id;
        this._name = name;
    }
    public Region(String name) {
        this._name = name;
    }
    public Region() {}

    void setName(String n) { _name = n; }
    public String getName(){
        return _name;
    }

}
