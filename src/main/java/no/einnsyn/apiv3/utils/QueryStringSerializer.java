package no.einnsyn.apiv3.utils;

import com.google.gson.Gson;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

@Service
public class QueryStringSerializer {

  private final Gson gson;

  public QueryStringSerializer(Gson gson) {
    this.gson = gson;
  }

  public String convert(Object obj) {
    var jsonString = gson.toJson(obj);
    var jsonObject = new JSONObject(jsonString);
    var queryString = new StringBuilder();
    flatten(jsonObject, "", queryString);
    return "?" + queryString;
  }

  private void flatten(JSONObject jsonObject, String prefix, StringBuilder queryString) {
    for (var key : jsonObject.keySet()) {
      var value = jsonObject.get(key);
      switch (value) {
        case JSONObject objectValue -> {
          flatten(objectValue, prefix + key + ".", queryString);
        }
        case JSONArray arrayValue -> {
          for (var arrayObject : arrayValue) {
            if (arrayObject instanceof JSONObject) {
              flatten((JSONObject) arrayObject, prefix + key + ".", queryString);
            } else {
              append(queryString, prefix + key, arrayObject.toString());
            }
          }
        }
        default -> {
          append(queryString, prefix + key, value.toString());
        }
      }
    }
  }

  private void append(StringBuilder queryString, String key, String value) {
    if (queryString.isEmpty()) {
      queryString.append("&");
    }
    queryString
        .append(URLEncoder.encode(key, StandardCharsets.UTF_8))
        .append("=")
        .append(URLEncoder.encode(value, StandardCharsets.UTF_8));
  }
}
