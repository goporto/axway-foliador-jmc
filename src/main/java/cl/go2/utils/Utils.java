package cl.go2.utils;

import com.axway.xib.Context;
import com.axway.xib.Data;
import com.axway.xib.ProcessingMessage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Utils {

    public static String[] tokenize_filename(String filename){
        //String[] parts = filename.split("_", 2);
        //String usuarioCasilla = parts[0];
        //String cleanedFilename = parts.length > 1 ? parts[1] : "";

        return filename.split("_", 2);
    }


    public static long getFileSize(Object fileSizeObj){
        long fileSize = 0;

        if (fileSizeObj != null) {
            if (fileSizeObj instanceof Number) {
                fileSize = ((Number) fileSizeObj).longValue();
            } else if (fileSizeObj instanceof String) {
                fileSize = Long.parseLong((String) fileSizeObj);
            }

        }else
            fileSize = -1;

        return fileSize;
    }
    /*
        Copia un mensaje de un buffer a otro.
     */
    public static void copyMessage(Context context, ProcessingMessage input_msg, ProcessingMessage output_msg) throws IOException {

        Data outputData = context.getContainer().createProcessingData();
        Data inputData = input_msg.getData();
        InputStream input = inputData.getInput(); //
        OutputStream output = outputData.getOutput(); //

        try {
            // Transferencia progresiva por bloques (64 KB) segura para archivos de varios gigas
            byte[] buffer = new byte[1024 * 64];
            int bytesRead;
            while ((bytesRead = input.read(buffer)) > 0) { //
                output.write(buffer, 0, bytesRead); //
            }
        } finally {
            // Asegurar siempre el cierre de flujos físicos para evitar bloqueos en disco
            if (output != null) output.close(); //
            if (input != null) input.close(); //
            if (inputData != null) inputData.close(); //
        }

        // Asignar los datos procesados al mensaje de salida
        output_msg.setData(outputData); //
        outputData.close(); //

    }
}
