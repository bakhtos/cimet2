package edu.university.ecs.lab.common.writers;

import javax.json.*;
import javax.json.stream.JsonGenerator;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/** Utility class for writing JSON to a file. */
public class MsJsonWriter {
  /** Private constructor to prevent instantiation. */
  private MsJsonWriter() {}

  /**
   * Write the given JSON object to the given file path.
   *
   * @param jout the JSON object to write
   * @param fileName the file to write the JSON object to
   * @throws IOException if an I/O error occurs
   */
  public static void writeJsonToFile(JsonObject jout, String fileName) throws IOException {
    try (FileWriter writer = new FileWriter(fileName)) {
      Map<String, Object> properties = new HashMap<>();
      properties.put(JsonGenerator.PRETTY_PRINTING, true);
      JsonWriterFactory writerFactory = Json.createWriterFactory(properties);

      JsonWriter jsonWriter = writerFactory.createWriter(writer);
      jsonWriter.write(jout);
      jsonWriter.close();
    }
  }

  public static void writeJsonToFile(JsonArray jout, String fileName) throws IOException {
    try (FileWriter writer = new FileWriter(fileName)) {
      Map<String, Object> properties = new HashMap<>();
      properties.put(JsonGenerator.PRETTY_PRINTING, true);
      JsonWriterFactory writerFactory = Json.createWriterFactory(properties);

      JsonWriter jsonWriter = writerFactory.createWriter(writer);
      jsonWriter.write(jout);
      jsonWriter.close();
    }
  }
}
