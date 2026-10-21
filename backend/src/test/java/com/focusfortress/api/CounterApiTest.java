package com.focusfortress.api;

import com.focusfortress.api.support.ApiTestBase;
import com.focusfortress.api.support.TestUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;

import static com.focusfortress.api.support.Payloads.counter;
import static com.focusfortress.api.support.Payloads.unique;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;

@DisplayName("Counters API  /api/counters")
class CounterApiTest extends ApiTestBase {

    private TestUser alice;

    @BeforeEach
    void setUp() {
        alice = auth.registerVerifiedUser();
    }

    @Nested
    @DisplayName("create and read")
    class CreateAndRead {

        @Test
        @DisplayName("POST creates a counter that starts now when no start date is given")
        void createsCounterStartingNow() {
            String name = unique("NoSugar");
            LocalDateTime before = LocalDateTime.now().minusSeconds(1);

            String start = as(alice).body(counter(name))
                    .when().post("/counters")
                    .then()
                    .statusCode(201)
                    .header("Location", notNullValue())
                    .body("id", notNullValue())
                    .body("name", equalTo(name))
                    .body("description", equalTo("Days without sugar"))
                    .body("icon", equalTo("🍬"))
                    .extract().path("startDateTime");

            assertThat(LocalDateTime.parse(start)).isAfter(before);
        }

        @Test
        @DisplayName("POST keeps an explicit start date")
        void keepsExplicitStartDate() {
            Map<String, Object> body = counter(unique("Smoke-free"));
            body.put("startDateTime", "2024-01-01T08:30:00");

            as(alice).body(body)
                    .when().post("/counters")
                    .then()
                    .statusCode(201)
                    .body("startDateTime", startsWith("2024-01-01T08:30"));
        }

        @Test
        @DisplayName("POST rejects blank name with 400")
        void rejectsBlankName() {
            as(alice).body(counter(""))
                    .when().post("/counters")
                    .then()
                    .statusCode(400)
                    .body("errors", hasKey("name"));
        }

        @Test
        @DisplayName("POST rejects duplicate name with 409")
        void rejectsDuplicateName() {
            String name = unique("Coffee");
            createId(alice, "/counters", counter(name));

            as(alice).body(counter(name))
                    .when().post("/counters")
                    .then()
                    .statusCode(409)
                    .body("message", equalTo("Counter with this name already exists for the user"));
        }

        @Test
        @DisplayName("GET lists only the caller's counters")
        void listsOwnCounters() {
            createId(alice, "/counters", counter(unique("One")));
            createId(alice, "/counters", counter(unique("Two")));

            as(alice).get("/counters").then().statusCode(200).body("$", hasSize(2));
            as(auth.registerVerifiedUser()).get("/counters").then().statusCode(200).body("$", hasSize(0));
        }

        @Test
        @DisplayName("GET /{id} returns the counter")
        void getsById() {
            String name = unique("ById");
            long id = createId(alice, "/counters", counter(name));

            as(alice).get("/counters/{id}", id)
                    .then()
                    .statusCode(200)
                    .body("id", equalTo((int) id))
                    .body("name", equalTo(name));
        }

        @Test
        @DisplayName("GET /{id} returns 404 for an unknown counter")
        void unknownCounter() {
            as(alice).get("/counters/{id}", Long.MAX_VALUE)
                    .then()
                    .statusCode(404)
                    .body("message", equalTo("Counter not found"));
        }
    }

    @Nested
    @DisplayName("update, reset and delete")
    class Modify {

        @Test
        @DisplayName("PUT updates name, description and icon")
        void updatesCounter() {
            long id = createId(alice, "/counters", counter(unique("Before")));
            String newName = unique("After");

            as(alice).body(Map.of("name", newName, "description", "changed", "icon", "✅"))
                    .when().put("/counters/{id}", id)
                    .then()
                    .statusCode(200)
                    .body("name", equalTo(newName))
                    .body("description", equalTo("changed"))
                    .body("icon", equalTo("✅"));
        }

        @Test
        @DisplayName("PUT rejects renaming to another existing counter name with 409")
        void rejectsRenameToExistingName() {
            String taken = unique("Taken");
            createId(alice, "/counters", counter(taken));
            long id = createId(alice, "/counters", counter(unique("Other")));

            as(alice).body(counter(taken)).put("/counters/{id}", id).then().statusCode(409);
        }

        @Test
        @DisplayName("PATCH /reset moves the start date to now")
        void resetsCounter() {
            Map<String, Object> body = counter(unique("Reset"));
            body.put("startDateTime", "2020-01-01T00:00:00");
            long id = createId(alice, "/counters", body);
            LocalDateTime before = LocalDateTime.now().minusSeconds(1);

            String start = as(alice).patch("/counters/{id}/reset", id)
                    .then()
                    .statusCode(200)
                    .extract().path("startDateTime");

            assertThat(LocalDateTime.parse(start)).isAfter(before);
        }

        @Test
        @DisplayName("DELETE removes the counter")
        void deletesCounter() {
            long id = createId(alice, "/counters", counter(unique("Gone")));

            as(alice).delete("/counters/{id}", id).then().statusCode(204);

            as(alice).get("/counters/{id}", id).then().statusCode(404);
            as(alice).get("/counters").then().body("id", not(hasItem((int) id)));
        }
    }

    @Nested
    @DisplayName("user isolation  - every operation on another user's counter is 403")
    class Isolation {

        private TestUser bob;
        private long aliceCounterId;
        private String aliceCounterName;

        @BeforeEach
        void aliceOwnsACounter() {
            bob = auth.registerVerifiedUser();
            aliceCounterName = unique("AlicePrivate");
            aliceCounterId = createId(alice, "/counters", counter(aliceCounterName));
        }

        @Test
        @DisplayName("GET /{id}")
        void cannotRead() {
            as(bob).get("/counters/{id}", aliceCounterId).then().statusCode(403);
        }

        @Test
        @DisplayName("PUT /{id}")
        void cannotUpdate() {
            as(bob).body(counter("hijacked")).put("/counters/{id}", aliceCounterId).then().statusCode(403);
            assertAliceCounterUntouched();
        }

        @Test
        @DisplayName("PATCH /{id}/reset")
        void cannotReset() {
            as(bob).patch("/counters/{id}/reset", aliceCounterId).then().statusCode(403);
        }

        @Test
        @DisplayName("DELETE /{id}")
        void cannotDelete() {
            as(bob).delete("/counters/{id}", aliceCounterId).then().statusCode(403);
            assertAliceCounterUntouched();
        }

        private void assertAliceCounterUntouched() {
            as(alice).get("/counters/{id}", aliceCounterId)
                    .then()
                    .statusCode(200)
                    .body("name", equalTo(aliceCounterName));
        }
    }
}
