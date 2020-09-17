/*
Michael Koval
*/
package FunctionalTesting;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;

import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.junit.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class Task0 extends BaseClass {

    @Test
    public void task00_01() throws URISyntaxException {
        URI uri = new URI("/users?page=1");

        given()
                .accept(ContentType.JSON)
                .when()
                .get(uri)
                .then()
                .body("data.email", hasItems("george.bluth@reqres.in", "emma.wong@reqres.in"), "data.id", hasSize(6));
    }

    @Test
    public void task00_02() throws URISyntaxException {
        URI uri = new URI("/users");
        String json = "{\n" + "    \"name\": \"Michael\",\n" + "    \"job\": \"Automation tester\"\n" + "}";
        Response resp = given()
                .body(json)
                .post(uri)
                .thenReturn();
        JsonPath jsonPath = new JsonPath(resp.asString());
        String date = jsonPath.getString("createdAt");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        DateTimeFormatter formatter2 = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        System.out.println(LocalDate.parse(date, formatter).format(formatter2));
    }
}
