package com.focusfortress.api;

import com.focusfortress.api.support.ApiTestBase;
import com.focusfortress.api.support.RegistrationPayload;
import com.focusfortress.api.support.TestUser;
import com.focusfortress.model.InterestCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.focusfortress.api.support.Payloads.unique;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

@DisplayName("Interests API  /api/interests, /api/user/interests")
class InterestApiTest extends ApiTestBase {

    @Nested
    @DisplayName("public catalog  GET /api/interests")
    class Catalog {

        @Test
        @DisplayName("lists every interest group with its category structure")
        void listsInterestGroups() {
            anonymous().get("/interests")
                    .then()
                    .statusCode(200)
                    .body("$", hasSize(InterestCategory.values().length))
                    .body("key", hasItem("PHILOSOPHY_MINDFULNESS"))
                    .body("find { it.key == 'PHILOSOPHY_MINDFULNESS' }.displayName", equalTo("Philosophy & Mindfulness"))
                    .body("find { it.key == 'PHILOSOPHY_MINDFULNESS' }.structure.name",
                            containsInAnyOrder("Mind & Body", "Personal Growth"));
        }

        @Test
        @DisplayName("flattened subcategory list matches the catalog and carries parent info")
        void listsSubcategories() {
            int expected = Arrays.stream(InterestCategory.values())
                    .flatMap(group -> group.getCategories().stream())
                    .mapToInt(category -> category.getSubcategories().size())
                    .sum();

            anonymous().get("/interests/subcategories")
                    .then()
                    .statusCode(200)
                    .body("$", hasSize(expected))
                    .body("find { it.name == 'Stoicism' }.categoryName", equalTo("Mind & Body"))
                    .body("icon", everyItem(notNullValue()))
                    .body("categoryName", everyItem(notNullValue()));
        }
    }

    @Nested
    @DisplayName("my interests  /api/user/interests")
    class MyInterests {

        private TestUser user;

        @BeforeEach
        void setUp() {
            user = auth.registerVerifiedUser(RegistrationPayload.aValidRegistration()
                    .with("selectedInterests", List.of("Meditation", "Physics", "Reading")));
        }

        @Test
        @DisplayName("GET returns the interests chosen at registration")
        void returnsRegistrationInterests() {
            as(user).get("/user/interests")
                    .then()
                    .statusCode(200)
                    .body("subcategoryName", containsInAnyOrder("Meditation", "Physics", "Reading"))
                    .body("find { it.subcategoryName == 'Physics' }.categoryName", equalTo("Natural Sciences"))
                    .body("isCustom", everyItem(equalTo(false)));
        }

        @Test
        @DisplayName("PUT adds and removes interests in one call")
        void managesInterests() {
            as(user).body(Map.of("add", List.of("Stoicism", "Hiking"), "remove", List.of("Physics")))
                    .when().put("/user/interests")
                    .then()
                    .statusCode(200)
                    .body("subcategoryName", containsInAnyOrder("Meditation", "Reading", "Stoicism", "Hiking"));
        }

        @Test
        @DisplayName("PUT rejects adding an interest that is already selected (409) and changes nothing")
        void rejectsDuplicateAddition() {
            as(user).body(Map.of("add", List.of("Meditation", "Hiking"), "remove", List.of()))
                    .when().put("/user/interests")
                    .then()
                    .statusCode(409)
                    .body("message", containsString("already selected: Meditation"));

            as(user).get("/user/interests").then().body("subcategoryName", not(hasItem("Hiking")));
        }

        @Test
        @DisplayName("PUT rejects missing add/remove lists with 400")
        void rejectsMissingLists() {
            as(user).body(Map.of("add", List.of("Hiking")))
                    .when().put("/user/interests")
                    .then()
                    .statusCode(400);
        }

        @Test
        @DisplayName("POST /custom adds a custom interest")
        void addsCustomInterest() {
            String name = unique("Chess");

            as(user).body(Map.of("name", name, "emoji", "♟️"))
                    .when().post("/user/interests/custom")
                    .then()
                    .statusCode(200)
                    .body("id", notNullValue())
                    .body("subcategoryName", equalTo(name))
                    .body("categoryName", equalTo("Custom"))
                    .body("isCustom", equalTo(true))
                    .body("emoji", equalTo("♟️"));
        }

        @Test
        @DisplayName("POST /custom rejects blank name and emoji with 400")
        void rejectsInvalidCustomInterest() {
            as(user).body(Map.of("name", " ", "emoji", ""))
                    .when().post("/user/interests/custom")
                    .then()
                    .statusCode(400)
                    .body("errors.keySet()", containsInAnyOrder("name", "emoji"));
        }

        @Test
        @DisplayName("DELETE /{id} removes one of my interests")
        void deletesInterest() {
            int id = as(user).get("/user/interests").path("find { it.subcategoryName == 'Physics' }.id");

            as(user).delete("/user/interests/{id}", id).then().statusCode(204);

            as(user).get("/user/interests")
                    .then()
                    .body("subcategoryName", containsInAnyOrder("Meditation", "Reading"));
        }

        @Disabled("BUG: DELETE /api/user/interests/{id} of another user -> observed 409 Conflict, "
                + "expected 403/404 (ownership violation is thrown as IllegalArgumentException)")
        @Test
        @DisplayName("DELETE of another user's interest is refused with 403/404")
        void cannotDeleteOtherUsersInterest() {
            int aliceInterestId = as(user).get("/user/interests").path("[0].id");
            TestUser bob = auth.registerVerifiedUser();

            as(bob).delete("/user/interests/{id}", aliceInterestId)
                    .then()
                    .statusCode(anyOf(equalTo(403), equalTo(404)));
        }

        @Test
        @DisplayName("DELETE of another user's interest leaves it in place")
        void otherUsersInterestSurvivesDeleteAttempt() {
            int aliceInterestId = as(user).get("/user/interests").path("[0].id");

            as(auth.registerVerifiedUser()).delete("/user/interests/{id}", aliceInterestId)
                    .then()
                    .statusCode(not(equalTo(204)));

            as(user).get("/user/interests").then().body("id", hasItem(aliceInterestId));
        }

        @Disabled("BUG: DELETE /api/user/interests/{unknownId} -> observed 409 Conflict, expected 404")
        @Test
        @DisplayName("DELETE of an unknown interest returns 404")
        void unknownInterest() {
            as(user).delete("/user/interests/{id}", Long.MAX_VALUE).then().statusCode(404);
        }

        @Disabled("BUG: POST /api/user/interests/custom with a name another user already used -> observed 409 "
                + "leaking raw SQL constraint error, expected 200 (custom interests share one global 'Custom' "
                + "category with a unique (name, category_id) constraint)")
        @Test
        @DisplayName("two users can each add a custom interest with the same name")
        void customInterestNamesArePerUser() {
            String name = unique("Origami");
            as(user).body(Map.of("name", name, "emoji", "🦢")).post("/user/interests/custom").then().statusCode(200);

            as(auth.registerVerifiedUser()).body(Map.of("name", name, "emoji", "🦢"))
                    .when().post("/user/interests/custom")
                    .then()
                    .statusCode(200);
        }
    }
}
