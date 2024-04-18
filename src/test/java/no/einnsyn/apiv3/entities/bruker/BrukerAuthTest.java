package no.einnsyn.apiv3.entities.bruker;

import static org.junit.jupiter.api.Assertions.assertEquals;

import no.einnsyn.apiv3.EinnsynControllerTestBase;
import no.einnsyn.apiv3.authentication.bruker.models.TokenResponse;
import no.einnsyn.apiv3.entities.bruker.models.BrukerDTO;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class BrukerAuthTest extends EinnsynControllerTestBase {

  @Test
  void testAdminPermissions() throws Exception {
    var response = post("/bruker", getBrukerJSON());
    var responseDTO = gson.fromJson(response.getBody(), BrukerDTO.class);

    // Check that a normal user cannot update Bruker
    var updateJSON = getBrukerJSON();
    updateJSON.remove("password");
    updateJSON.put("email", "updated@example.com");
    response = put("/bruker/" + responseDTO.getId(), updateJSON);
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // Check that admin can update Bruker
    response = put("/bruker/" + responseDTO.getId(), updateJSON, adminKey);
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Check that a normal user cannot delete Bruker
    response = delete("/bruker/" + responseDTO.getId());
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // Check that admin can delete Bruker
    response = delete("/bruker/" + responseDTO.getId(), adminKey);
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  @Test
  void testBrukerPermissions() throws Exception {
    var brukerJSON = getBrukerJSON();
    brukerJSON.put("email", "bruker1@example.com");
    brukerJSON.put("password", "Password1");
    var response = post("/bruker", brukerJSON);
    var bruker1DTO = gson.fromJson(response.getBody(), BrukerDTO.class);
    var bruker1 = brukerService.findById(bruker1DTO.getId());

    // Activate bruker1
    response = putAnon("/bruker/" + bruker1DTO.getId() + "/activate/" + bruker1.getSecret(), null);
    assertEquals(HttpStatus.OK, response.getStatusCode());

    brukerJSON = getBrukerJSON();
    brukerJSON.put("email", "bruker2@example.com");
    brukerJSON.put("password", "Password2");
    response = post("/bruker", brukerJSON);
    var bruker2DTO = gson.fromJson(response.getBody(), BrukerDTO.class);
    var bruker2 = brukerService.findById(bruker2DTO.getId());

    // Activate bruker2
    response = putAnon("/bruker/" + bruker2DTO.getId() + "/activate/" + bruker2.getSecret(), null);
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Get token for bruker1
    var loginRequest = new JSONObject();
    loginRequest.put("username", "bruker1@example.com");
    loginRequest.put("password", "Password1");
    response = post("/auth/token", loginRequest);
    var tokenResponse = gson.fromJson(response.getBody(), TokenResponse.class);
    var accessToken1 = tokenResponse.getToken();

    // Get token for bruker2
    loginRequest.put("username", "bruker2@example.com");
    loginRequest.put("password", "Password2");
    response = post("/auth/token", loginRequest);
    tokenResponse = gson.fromJson(response.getBody(), TokenResponse.class);
    var accessToken2 = tokenResponse.getToken();

    // Check that anonymous cannot update Bruker
    var updateJSON = getBrukerJSON();
    updateJSON.remove("password");
    updateJSON.put("email", "updated@example.com");
    response = putAnon("/bruker/" + bruker1DTO.getId(), updateJSON);
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // Check that bruker2 cannot update bruker1
    response = put("/bruker/" + bruker1DTO.getId(), updateJSON, accessToken2);
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // Check that bruker1 can update bruker1
    response = put("/bruker/" + bruker1DTO.getId(), updateJSON, accessToken1);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    bruker1DTO = gson.fromJson(response.getBody(), BrukerDTO.class);

    // Check that anonymous cannot delete bruker1
    response = deleteAnon("/bruker/" + bruker1DTO.getId());
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // Check that bruker2 cannot delete bruker1
    response = delete("/bruker/" + bruker1DTO.getId(), accessToken2);
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

    // Check that bruker1 can delete bruker1
    response = delete("/bruker/" + bruker1DTO.getId(), accessToken1);
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Check that bruker2 can delete bruker2, but with email as username
    response = delete("/bruker/" + bruker2DTO.getEmail(), accessToken2);
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }
}
