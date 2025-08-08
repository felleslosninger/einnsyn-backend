package no.einnsyn.backend.configuration.typeadapters;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import no.einnsyn.backend.EinnsynControllerTestBase;
import no.einnsyn.backend.common.expandablefield.ExpandableField;
import no.einnsyn.backend.entities.bruker.models.BrukerDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class ExpandableFieldDeserializerTest extends EinnsynControllerTestBase {

  @Autowired
  @Qualifier("gsonPrettyAllowUnknown")
  private Gson gson;

  @Test
  void testExpandableFieldDeserializationWithEmail() throws Exception {
    // Create a user with an email
    var brukerJSON = getBrukerJSON();
    var brukerEmail = brukerJSON.getString("email");
    var response = post("/bruker", brukerJSON);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var brukerDTO = gson.fromJson(response.getBody(), BrukerDTO.class);
    var brukerId = brukerDTO.getId();

    try {
      // Test that we can use email in ExpandableField and it gets resolved to eInnsynId
      var type = new TypeToken<ExpandableField<BrukerDTO>>() {}.getType();
      ExpandableField<BrukerDTO> testDTO = gson.fromJson(brukerEmail, type);
      assertEquals(brukerId, testDTO.getId());
    } finally {
      deleteAdmin("/bruker/" + brukerDTO.getId());
    }
  }
}
