package FunctionalTesting;

import org.junit.BeforeClass;
import static io.restassured.RestAssured.*;

public class BaseClass {

    @BeforeClass
    public static void setUp(){
        baseURI = "https://reqres.in";
        basePath = "/api";
    }
}
