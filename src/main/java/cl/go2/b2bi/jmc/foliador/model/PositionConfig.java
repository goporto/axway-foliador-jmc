package cl.go2.b2bi.jmc.foliador.model;

public class PositionConfig {
    public int id;
    public String opcion;
    public int startPos;
    public int endPos;
    public int startPosInstitucion;
    public int endPosInstitucion;
    public int startPosDate;
    public int endPosDate;

    public PositionConfig(int id, String opcion, int startPos, int endPos,
                          int startPosInstitucion, int endPosInstitucion,
                          int startPosDate, int endPosDate) {
        this.id = id;
        this.opcion = opcion;
        this.startPos = startPos;
        this.endPos = endPos;
        this.startPosInstitucion = startPosInstitucion;
        this.endPosInstitucion = endPosInstitucion;
        this.startPosDate = startPosDate;
        this.endPosDate = endPosDate;
    }
}