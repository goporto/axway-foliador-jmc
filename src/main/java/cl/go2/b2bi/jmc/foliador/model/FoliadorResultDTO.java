package cl.go2.b2bi.jmc.foliador.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class FoliadorResultDTO {

    private String correlativo;
    private String tDoc;
    private String entidadOrigen;
    private String filial;
    private String usuarioCasilla;
    private String archivoEntrada;
    private int status; // 0 = OK, >0 = Error
    private String errorMessage;

    public FoliadorResultDTO() {}

    public FoliadorResultDTO(String correlativo, String tDoc, String entidadOrigen, String filial) {
        this.correlativo = correlativo;
        this.tDoc = tDoc;
        this.entidadOrigen = entidadOrigen;
        this.filial = filial;
        this.status = 0;
        this.errorMessage = "SUCCESS";
    }

    // Getters y Setters
    public String getCorrelativo() { return correlativo; }
    public void setCorrelativo(String correlativo) { this.correlativo = correlativo; }

    public String gettDoc() { return tDoc; }
    public void settDoc(String tDoc) { this.tDoc = tDoc; }

    public String getEntidadOrigen() { return entidadOrigen; }
    public void setEntidadOrigen(String entidadOrigen) { this.entidadOrigen = entidadOrigen; }

    public String getFilial() { return filial; }
    public void setFilial(String filial) { this.filial = filial; }

    public String getUsuarioCasilla() { return usuarioCasilla; }
    public void setUsuarioCasilla(String usuarioCasilla) { this.usuarioCasilla = usuarioCasilla; }

    public String getArchivoEntrada() { return archivoEntrada; }
    public void setArchivoEntrada(String archivoEntrada) { this.archivoEntrada = archivoEntrada; }

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public String toJson() {
        try {
            return new ObjectMapper().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            return "{\"status\":-1, \"errorMessage\":\"Error serializando a JSON\"}";
        }
    }
}