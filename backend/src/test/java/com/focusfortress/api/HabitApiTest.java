package com.focusfortress.api;

import com.focusfortress.api.support.ApiTestBase;
import com.focusfortress.api.support.TestUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.focusfortress.api.support.Payloads.habit;
import static com.focusfortress.api.support.Payloads.unique;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

@DisplayName("Habits API  /api/habits")
class HabitApiTest extends ApiTestBase {

    private TestUser alice;

    @BeforeEach
    void setUp() {
        alice = auth.registerVerifiedUser();
    }

    @Nested
    @DisplayName("POST /habits")
    class Create {

        @Test
        @DisplayName("creates a habit and returns 201 with Location header")
        void createsHabit() {
            long categoryId = createCategory(alice);
            long subcategoryId = createSubcategory(alice, categoryId);
            String name = unique("Read");

            int id = as(alice).body(habit(name, categoryId, subcategoryId))
                    .when().post("/habits")
                    .then()
                    .statusCode(201)
                    .header("Location", notNullValue())
                    .body("id", notNullValue())
                    .body("name", equalTo(name))
                    .body("description", equalTo("Read 10 pages every day"))
                    .body("icon", equalTo("📖"))
                    .body("durationDays", equalTo(21))
                    .body("categoryId", equalTo((int) categoryId))
                    .body("subcategoryId", equalTo((int) subcategoryId))
                    .body("done", equalTo(false))
                    .extract().path("id");

            as(alice).get("/habits").then().body("id", hasItem(id));
        }

        @Test
        @DisplayName("Location header points at the created habit")
        void locationHeaderPointsAtHabit() {
            var response = as(alice).body(habit(unique("Walk"))).post("/habits");

            response.then().statusCode(201)
                    .header("Location", endsWith("/api/habits/" + response.path("id")));
        }

        @Test
        @DisplayName("category and subcategory are optional")
        void categoryIsOptional() {
            as(alice).body(habit(unique("Stretch")))
                    .when().post("/habits")
                    .then()
                    .statusCode(201)
                    .body("categoryId", equalTo(null))
                    .body("subcategoryId", equalTo(null));
        }

        @Test
        @DisplayName("rejects blank name, non-positive duration and oversized icon with 400")
        void rejectsInvalidPayload() {
            Map<String, Object> body = habit("  ");
            body.put("durationDays", 0);
            body.put("icon", "TOO-LONG-ICON");

            as(alice).body(body)
                    .when().post("/habits")
                    .then()
                    .statusCode(400)
                    .body("errors", hasKey("name"))
                    .body("errors", hasKey("durationDays"))
                    .body("errors", hasKey("icon"));
        }

        @Test
        @DisplayName("rejects malformed JSON with 400")
        void rejectsMalformedJson() {
            as(alice).body("{\"name\": ").post("/habits").then().statusCode(400);
        }

        @Test
        @DisplayName("rejects duplicate name for the same user with 409")
        void rejectsDuplicateName() {
            String name = unique("Meditate");
            createHabit(alice, habit(name));

            as(alice).body(habit(name))
                    .when().post("/habits")
                    .then()
                    .statusCode(409)
                    .body("message", equalTo("Habit with this name already exists for the user"));
        }

        @Test
        @DisplayName("the same name is allowed for a different user")
        void sameNameAllowedForOtherUser() {
            String name = unique("Meditate");
            createHabit(alice, habit(name));

            as(auth.registerVerifiedUser()).body(habit(name)).post("/habits").then().statusCode(201);
        }

        @Test
        @DisplayName("rejects unknown category with 404")
        void rejectsUnknownCategory() {
            Map<String, Object> body = habit(unique("Run"));
            body.put("categoryId", Long.MAX_VALUE);

            as(alice).body(body)
                    .when().post("/habits")
                    .then()
                    .statusCode(404)
                    .body("message", equalTo("Category not found"));
        }
    }

    @Nested
    @DisplayName("GET /habits")
    class Read {

        @Test
        @DisplayName("a new user has no habits")
        void emptyForNewUser() {
            as(alice).get("/habits").then().statusCode(200).body("$", hasSize(0));
        }

        @Test
        @DisplayName("lists own habits with current streak")
        void listsHabitsWithStreak() {
            long id = createHabit(alice, habit(unique("Journal")));
            as(alice).post("/habits/{id}/done", id).then().statusCode(200);
            createHabit(alice, habit(unique("Floss")));

            as(alice).get("/habits")
                    .then()
                    .statusCode(200)
                    .body("$", hasSize(2))
                    .body("find { it.id == %s }.currentStreak".formatted(id), equalTo(1));
        }

        @Test
        @DisplayName("filters by category and by subcategory")
        void filtersByCategoryAndSubcategory() {
            long categoryId = createCategory(alice);
            long subA = createSubcategory(alice, categoryId);
            long subB = createSubcategory(alice, categoryId);
            long habitA = createHabit(alice, habit(unique("A"), categoryId, subA));
            long habitB = createHabit(alice, habit(unique("B"), categoryId, subB));
            createHabit(alice, habit(unique("Uncategorised")));

            as(alice).get("/habits/by-category/{c}", categoryId)
                    .then().statusCode(200).body("id", containsInAnyOrder((int) habitA, (int) habitB));
            as(alice).get("/habits/by-subcategory/{s}", subB)
                    .then().statusCode(200).body("id", contains((int) habitB));
            as(alice).get("/habits/by-category/{c}/by-subcategory/{s}", categoryId, subA)
                    .then().statusCode(200).body("id", contains((int) habitA));
        }

        @Test
        @DisplayName("tree groups habits under their category and subcategory")
        void treeGroupsHabits() {
            long categoryId = createCategory(alice);
            long subcategoryId = createSubcategory(alice, categoryId);
            String name = unique("Tree");
            createHabit(alice, habit(name, categoryId, subcategoryId));

            as(alice).get("/habits/tree")
                    .then()
                    .statusCode(200)
                    .body("find { it.id == %s }.subcategories[0].id".formatted(categoryId), equalTo((int) subcategoryId))
                    .body("find { it.id == %s }.subcategories[0].habits.name".formatted(categoryId), contains(name));
        }
    }

    @Nested
    @DisplayName("PUT /habits/{id}")
    class Update {

        @Test
        @DisplayName("updates all editable fields")
        void updatesHabit() {
            long categoryId = createCategory(alice);
            long subcategoryId = createSubcategory(alice, categoryId);
            long id = createHabit(alice, habit(unique("Old"), categoryId, subcategoryId));
            String newName = unique("New");
            Map<String, Object> body = habit(newName, categoryId, subcategoryId);
            body.put("description", "Updated");
            body.put("durationDays", 30);

            as(alice).body(body)
                    .when().put("/habits/{id}", id)
                    .then()
                    .statusCode(200)
                    .body("id", equalTo((int) id))
                    .body("name", equalTo(newName))
                    .body("description", equalTo("Updated"))
                    .body("durationDays", equalTo(30));
        }

        @Test
        @DisplayName("rejects subcategory from another category with 409")
        void rejectsMismatchedSubcategory() {
            long categoryA = createCategory(alice);
            long categoryB = createCategory(alice);
            long subOfB = createSubcategory(alice, categoryB);
            long id = createHabit(alice, habit(unique("Mismatch"), categoryA, createSubcategory(alice, categoryA)));

            as(alice).body(habit(unique("Mismatch"), categoryA, subOfB))
                    .when().put("/habits/{id}", id)
                    .then()
                    .statusCode(409)
                    .body("message", equalTo("Subcategory does not belong to the selected category"));
        }

        @Test
        @DisplayName("rejects changing duration once the habit has a streak")
        void rejectsDurationChangeAfterStart() {
            long categoryId = createCategory(alice);
            long subcategoryId = createSubcategory(alice, categoryId);
            String name = unique("Started");
            long id = createHabit(alice, habit(name, categoryId, subcategoryId));
            as(alice).post("/habits/{id}/done", id).then().statusCode(200);

            Map<String, Object> body = habit(name, categoryId, subcategoryId);
            body.put("durationDays", 60);

            as(alice).body(body)
                    .when().put("/habits/{id}", id)
                    .then()
                    .statusCode(400)
                    .body("message", equalTo(
                            "Cannot change duration after starting the habit. Delete and recreate to restart."));
        }

        @Test
        @DisplayName("rejects editing a completed habit with 400")
        void rejectsEditingCompletedHabit() {
            long categoryId = createCategory(alice);
            long subcategoryId = createSubcategory(alice, categoryId);
            Map<String, Object> oneDay = habit(unique("OneDay"), categoryId, subcategoryId);
            oneDay.put("durationDays", 1);
            long id = createHabit(alice, oneDay);
            as(alice).post("/habits/{id}/done", id).then().statusCode(200);

            as(alice).body(oneDay)
                    .when().put("/habits/{id}", id)
                    .then()
                    .statusCode(400)
                    .body("message", equalTo("Cannot edit a completed habit"));
        }

        @Test
        @DisplayName("returns 404 for a habit that does not exist")
        void unknownHabit() {
            long categoryId = createCategory(alice);
            as(alice).body(habit(unique("Ghost"), categoryId, createSubcategory(alice, categoryId)))
                    .when().put("/habits/{id}", Long.MAX_VALUE)
                    .then()
                    .statusCode(404);
        }

        @Disabled("BUG: PUT /api/habits/{id} without categoryId -> observed 409 'The given id must not be null', "
                + "expected 200 like on create, which accepts a habit without category (update calls findById(null))")
        @Test
        @DisplayName("habit without category can be updated")
        void updatesHabitWithoutCategory() {
            long id = createHabit(alice, habit(unique("NoCategory")));

            as(alice).body(habit(unique("StillNoCategory")))
                    .when().put("/habits/{id}", id)
                    .then()
                    .statusCode(200);
        }
    }

    @Nested
    @DisplayName("DELETE /habits/{id}")
    class Delete {

        @Test
        @DisplayName("deletes the habit together with its progress")
        void deletesHabit() {
            long id = createHabit(alice, habit(unique("Temporary")));
            as(alice).post("/habits/{id}/done", id).then().statusCode(200);

            as(alice).delete("/habits/{id}", id).then().statusCode(204);

            as(alice).get("/habits").then().body("id", not(hasItem((int) id)));
            as(alice).get("/habits/{id}/streak", id).then().statusCode(404);
        }

        @Test
        @DisplayName("returns 404 for a habit that does not exist")
        void unknownHabit() {
            as(alice).delete("/habits/{id}", Long.MAX_VALUE)
                    .then()
                    .statusCode(404)
                    .body("message", equalTo("Habit not found"));
        }
    }

    @Nested
    @DisplayName("progress  POST /habits/{id}/done, GET /habits/{id}/streak")
    class Progress {

        @Test
        @DisplayName("a fresh habit has streak 0")
        void freshHabitHasNoStreak() {
            long id = createHabit(alice, habit(unique("Fresh")));

            as(alice).get("/habits/{id}/streak", id).then().statusCode(200).body(equalTo("0"));
        }

        @Test
        @DisplayName("marking done returns the new streak")
        void markDoneReturnsStreak() {
            long id = createHabit(alice, habit(unique("Daily")));

            as(alice).post("/habits/{id}/done", id).then().statusCode(200).body(equalTo("1"));
            as(alice).get("/habits/{id}/streak", id).then().statusCode(200).body(equalTo("1"));
        }

        @Test
        @DisplayName("can be marked done only once per day")
        void onlyOncePerDay() {
            long id = createHabit(alice, habit(unique("Once")));
            as(alice).post("/habits/{id}/done", id).then().statusCode(200);

            as(alice).post("/habits/{id}/done", id)
                    .then()
                    .statusCode(400)
                    .body("message", equalTo("Already marked as done today"));
        }

        @Test
        @DisplayName("reaching durationDays completes the habit")
        void completesHabit() {
            Map<String, Object> body = habit(unique("Sprint"));
            body.put("durationDays", 1);
            long id = createHabit(alice, body);

            as(alice).post("/habits/{id}/done", id).then().statusCode(200).body(equalTo("1"));

            as(alice).get("/habits")
                    .then()
                    .body("find { it.id == %s }.done".formatted(id), equalTo(true));
        }

        @Test
        @DisplayName("returns 404 for a habit that does not exist")
        void unknownHabit() {
            as(alice).post("/habits/{id}/done", Long.MAX_VALUE).then().statusCode(404);
        }
    }

    @Nested
    @DisplayName("user isolation")
    class Isolation {

        private TestUser bob;
        private long aliceHabitId;
        private String aliceHabitName;

        @BeforeEach
        void aliceOwnsAHabit() {
            bob = auth.registerVerifiedUser();
            aliceHabitName = unique("AlicePrivate");
            long categoryId = createCategory(alice);
            aliceHabitId = createHabit(alice,
                    habit(aliceHabitName, categoryId, createSubcategory(alice, categoryId)));
        }

        @Test
        @DisplayName("other user's habits are not listed, neither flat nor in the tree")
        void notListed() {
            String habitIdsInTree = "subcategories.flatten().habits.flatten().id";
            as(alice).get("/habits/tree").then().body(habitIdsInTree, hasItem((int) aliceHabitId));

            as(bob).get("/habits").then().statusCode(200).body("id", not(hasItem((int) aliceHabitId)));
            as(bob).get("/habits/tree").then().statusCode(200)
                    .body(habitIdsInTree, not(hasItem((int) aliceHabitId)));
        }

        @Test
        @DisplayName("cannot update another user's habit (404)")
        void cannotUpdate() {
            long categoryId = createCategory(bob);
            as(bob).body(habit("hijacked", categoryId, createSubcategory(bob, categoryId)))
                    .when().put("/habits/{id}", aliceHabitId)
                    .then()
                    .statusCode(404);

            assertAliceHabitUntouched();
        }

        @Test
        @DisplayName("cannot delete another user's habit (403)")
        void cannotDelete() {
            as(bob).delete("/habits/{id}", aliceHabitId)
                    .then()
                    .statusCode(403)
                    .body("message", equalTo("Access denied"));

            assertAliceHabitUntouched();
        }

        @Test
        @DisplayName("cannot mark another user's habit as done (403)")
        void cannotMarkDone() {
            as(bob).post("/habits/{id}/done", aliceHabitId).then().statusCode(403);

            as(alice).get("/habits/{id}/streak", aliceHabitId).then().body(equalTo("0"));
        }

        @Test
        @DisplayName("cannot read another user's streak (403)")
        void cannotReadStreak() {
            as(bob).get("/habits/{id}/streak", aliceHabitId).then().statusCode(403);
        }

        @Test
        @DisplayName("category filters only return the caller's own habits")
        void filtersAreScopedToCaller() {
            long categoryId = createCategory(alice);
            long subcategoryId = createSubcategory(alice, categoryId);
            createHabit(alice, habit(unique("Scoped"), categoryId, subcategoryId));

            as(bob).get("/habits/by-category/{c}", categoryId).then().statusCode(200).body("$", hasSize(0));
            as(bob).get("/habits/by-subcategory/{s}", subcategoryId).then().statusCode(200).body("$", hasSize(0));
        }

        private void assertAliceHabitUntouched() {
            as(alice).get("/habits")
                    .then()
                    .body("find { it.id == %s }.name".formatted(aliceHabitId), equalTo(aliceHabitName));
        }
    }
}
