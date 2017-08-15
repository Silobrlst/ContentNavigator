import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

public class JsonLoader {
    private static String loadDataFromFile(File fileIn) {
        try {
            fileIn.createNewFile();

            byte[] encoded = Files.readAllBytes(Paths.get(fileIn.getAbsolutePath()));
            return new String(encoded, Charset.defaultCharset());
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    private static void saveDataToFile(File fileIn, String dataIn) {
        BufferedWriter bw = null;
        FileWriter fw = null;

        try {
            fw = new FileWriter(fileIn);
            bw = new BufferedWriter(fw);
            bw.write(dataIn);

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (bw != null)
                    bw.close();

                if (fw != null)
                    fw.close();

            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public static JSONObject loadJSON(File fileIn) {
        String data = loadDataFromFile(fileIn);

        if (data.isEmpty()) {
            data = "{}";
        }

        return new JSONObject(data);
    }

    public static void saveJSON(File fileIn, JSONObject jsonObjectIn) {
        saveDataToFile(fileIn, jsonObjectIn.toString(5));
    }
}
